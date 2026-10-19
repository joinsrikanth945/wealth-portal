package com.yourbank.wealth.messaging;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.messaging.MessageService.MessageDto;
import com.yourbank.wealth.messaging.MessageService.Thread;
import com.yourbank.wealth.messaging.MessageService.ThreadSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Messages", description = "Secure messages between clients and their relationship manager")
public class MessageController {

    public record SendRequest(@NotBlank @Size(max = 4000) String body) {
    }

    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    @GetMapping("/api/messages/threads")
    @Operation(summary = "Conversations: one per client (an RM sees their book, a client sees their own)")
    public List<ThreadSummary> threads(@AuthenticationPrincipal Jwt jwt) {
        return messages.threads(CurrentUser.from(jwt));
    }

    @GetMapping("/api/messages/threads/{clientId}")
    @Operation(summary = "Open a conversation (marks the other side's messages as read)")
    public Thread open(@PathVariable Long clientId, @AuthenticationPrincipal Jwt jwt) {
        return messages.open(clientId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/messages/threads/{clientId}")
    @Operation(summary = "Send a message in a conversation")
    public MessageDto send(@PathVariable Long clientId, @Valid @RequestBody SendRequest body,
                           @AuthenticationPrincipal Jwt jwt) {
        return messages.send(clientId, body.body(), CurrentUser.from(jwt));
    }

    @GetMapping("/api/messages/unread-count")
    @Operation(summary = "Number of unread messages, for the badge in the menu")
    public Map<String, Integer> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unread", messages.unreadCount(CurrentUser.from(jwt)));
    }
}
