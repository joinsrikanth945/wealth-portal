package com.yourbank.wealth.order;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.order.OrderDtos.DecisionRequest;
import com.yourbank.wealth.order.OrderDtos.OrderDto;
import com.yourbank.wealth.order.OrderDtos.OrderPreview;
import com.yourbank.wealth.order.OrderEnums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Orders", description = "Security and derivative orders, pre-trade checks and compliance approval")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/api/orders/preview")
    @Operation(summary = "Costs and pre-trade checks for an order, without placing it")
    public OrderPreview preview(@Valid @RequestBody OrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        return orders.preview(request, CurrentUser.from(jwt));
    }

    @PostMapping("/api/orders")
    @Operation(summary = "Place an order. Blocked if a check FAILs; waits for approval if one WARNs")
    public OrderDto place(@Valid @RequestBody OrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        return orders.place(request, CurrentUser.from(jwt));
    }

    @GetMapping("/api/orders")
    @Operation(summary = "Order blotter: orders the user may see, optionally for one portfolio or status")
    public List<OrderDto> list(
            @RequestParam(required = false) Long portfolioId,
            @RequestParam(required = false) Status status,
            @AuthenticationPrincipal Jwt jwt) {
        return orders.list(CurrentUser.from(jwt), portfolioId, status);
    }

    @PostMapping("/api/orders/{id}/cancel")
    @Operation(summary = "Cancel an order that is waiting for approval or not yet filled")
    public OrderDto cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return orders.cancel(id, CurrentUser.from(jwt));
    }

    @PostMapping("/api/admin/orders/{id}/approve")
    @Operation(summary = "Compliance approves an order (not your own)")
    public OrderDto approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest body,
                            @AuthenticationPrincipal Jwt jwt) {
        return orders.approve(id, body == null ? null : body.note(), CurrentUser.from(jwt));
    }

    @PostMapping("/api/admin/orders/{id}/reject")
    @Operation(summary = "Compliance rejects an order")
    public OrderDto reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest body,
                           @AuthenticationPrincipal Jwt jwt) {
        return orders.reject(id, body == null ? null : body.note(), CurrentUser.from(jwt));
    }
}
