package com.yourbank.wealth.book;

import com.yourbank.wealth.book.BookDtos.ClientDetail;
import com.yourbank.wealth.book.BookDtos.ClientListItem;
import com.yourbank.wealth.book.BookDtos.RmHome;
import com.yourbank.wealth.identity.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Clients", description = "The RM's book of clients, client pages and the RM homepage")
public class BookController {

    private final BookService book;

    public BookController(BookService book) {
        this.book = book;
    }

    @GetMapping("/api/rm/home")
    @Operation(summary = "RM homepage counters: AUM, MiFID problems, high-cash portfolios")
    public RmHome home(@AuthenticationPrincipal Jwt jwt) {
        return book.home(CurrentUser.from(jwt));
    }

    @GetMapping("/api/rm/clients")
    @Operation(summary = "The RM's clients (all clients for an admin), optionally filtered by name or number")
    public List<ClientListItem> clients(@RequestParam(required = false) String q, @AuthenticationPrincipal Jwt jwt) {
        return book.clients(CurrentUser.from(jwt), q);
    }

    @GetMapping("/api/clients/{clientId}")
    @Operation(summary = "One client: details, portfolios and MiFID overview")
    public ClientDetail client(@PathVariable Long clientId, @AuthenticationPrincipal Jwt jwt) {
        return book.client(clientId, CurrentUser.from(jwt));
    }

    @GetMapping("/api/client/me")
    @Operation(summary = "For a signed-in client: their own details and portfolios")
    public ClientDetail me(@AuthenticationPrincipal Jwt jwt) {
        return book.self(CurrentUser.from(jwt));
    }
}
