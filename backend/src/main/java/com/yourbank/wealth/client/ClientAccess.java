package com.yourbank.wealth.client;

import com.yourbank.wealth.identity.CurrentUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Who may see which client — the one place this rule lives.
 * Admins see everyone, an RM sees their own book, a client sees only themselves.
 */
@Component
public class ClientAccess {

    private final ClientRepository clients;

    public ClientAccess(ClientRepository clients) {
        this.clients = clients;
    }

    public boolean canSee(Client client, CurrentUser user) {
        if (user.isAdmin()) {
            return true;
        }
        if (user.isRm() && client.getRmUsername().equals(user.username())) {
            return true;
        }
        return user.isClient() && user.username().equals(client.getPortalUsername());
    }

    /** Staff only (RM or admin) and the client must be in their book. */
    public boolean canManage(Client client, CurrentUser user) {
        return user.isAdmin() || (user.isRm() && client.getRmUsername().equals(user.username()));
    }

    /** The client, if this user may see it. Unknown and forbidden both answer 404, so ids can't be probed. */
    public Client requireVisible(Long clientId, CurrentUser user) {
        Client client = clients.findById(clientId).orElseThrow(ClientAccess::notFound);
        if (!canSee(client, user)) {
            throw notFound();
        }
        return client;
    }

    public Client requireManaged(Long clientId, CurrentUser user) {
        Client client = requireVisible(clientId, user);
        if (!canManage(client, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client's relationship manager can do this");
        }
        return client;
    }

    /** The clients in this user's book (all clients for an admin). */
    public List<Client> book(CurrentUser user) {
        return user.isAdmin()
                ? clients.findAllByOrderByLastNameAsc()
                : clients.findByRmUsernameOrderByLastNameAsc(user.username());
    }

    /** The client record linked to a portal login. */
    public Client self(CurrentUser user) {
        return clients.findByPortalUsername(user.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No client record is linked to this login"));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
    }
}
