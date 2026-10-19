package com.yourbank.wealth.messaging;

import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Secure messaging: one conversation per client, between the client and the
 * bank (their RM). "Unread" means sent by the other side and not opened yet.
 */
@Service
public class MessageService {

    public record MessageDto(Long id, boolean fromClient, String senderName, String body, Instant createdAt,
                             Instant readAt, boolean mine) {
    }

    public record ThreadSummary(Long clientId, String clientName, String clientNumber, String lastMessage,
                                Instant lastAt, int unread) {
    }

    public record Thread(Long clientId, String clientName, List<MessageDto> messages) {
    }

    private final MessageRepository messages;
    private final ClientAccess access;
    private final Clock clock;

    public MessageService(MessageRepository messages, ClientAccess access, Clock clock) {
        this.messages = messages;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ThreadSummary> threads(CurrentUser user) {
        List<Client> clients = isClientUser(user) ? List.of(access.self(user)) : access.book(user);
        if (clients.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Message>> byClient = messages.findByClientIds(clients.stream().map(Client::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getClient().getId()));
        boolean clientUser = isClientUser(user);
        return clients.stream()
                .map(c -> {
                    List<Message> list = byClient.getOrDefault(c.getId(), List.of());
                    Message last = list.isEmpty() ? null : list.getLast();
                    int unread = (int) list.stream().filter(m -> isUnreadFor(m, clientUser)).count();
                    return new ThreadSummary(c.getId(), c.getFullName(), c.getClientNumber(),
                            last == null ? null : preview(last.getBody()), last == null ? null : last.getCreatedAt(), unread);
                })
                .sorted(Comparator.comparing(ThreadSummary::lastAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Opens a conversation and marks the other side's messages as read. */
    @Transactional
    public Thread open(Long clientId, CurrentUser user) {
        Client client = requireParticipant(clientId, user);
        boolean clientUser = isClientUser(user);
        Instant now = clock.instant();
        List<Message> list = messages.findByClientId(clientId);
        list.stream().filter(m -> isUnreadFor(m, clientUser)).forEach(m -> m.markRead(now));
        return new Thread(client.getId(), client.getFullName(), list.stream().map(m -> dto(m, clientUser)).toList());
    }

    @Transactional
    public MessageDto send(Long clientId, String body, CurrentUser user) {
        Client client = requireParticipant(clientId, user);
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The message is empty");
        }
        boolean clientUser = isClientUser(user);
        Message m = messages.save(new Message(client, clientUser, user.username(), user.displayName(),
                body.strip(), clock.instant()));
        return dto(m, clientUser);
    }

    @Transactional(readOnly = true)
    public int unreadCount(CurrentUser user) {
        return threads(user).stream().mapToInt(ThreadSummary::unread).sum();
    }

    private Client requireParticipant(Long clientId, CurrentUser user) {
        if (isClientUser(user)) {
            Client self = access.self(user);
            if (!self.getId().equals(clientId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
            }
            return self;
        }
        return access.requireManaged(clientId, user);
    }

    private static boolean isClientUser(CurrentUser user) {
        return user.isClient() && !user.isRm() && !user.isAdmin();
    }

    private static boolean isUnreadFor(Message m, boolean clientUser) {
        return m.getReadAt() == null && m.isFromClient() != clientUser;
    }

    private static MessageDto dto(Message m, boolean clientUser) {
        return new MessageDto(m.getId(), m.isFromClient(), m.getSenderName(), m.getBody(), m.getCreatedAt(),
                m.getReadAt(), m.isFromClient() == clientUser);
    }

    private static String preview(String body) {
        return body.length() <= 90 ? body : body.substring(0, 87) + "...";
    }
}
