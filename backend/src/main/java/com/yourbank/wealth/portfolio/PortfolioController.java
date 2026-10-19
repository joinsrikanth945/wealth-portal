package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolios", description = "Holdings, valuation, allocation and performance")
public class PortfolioController {

    private final PortfolioService portfolios;

    public PortfolioController(PortfolioService portfolios) {
        this.portfolios = portfolios;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full portfolio view: valuation, positions, allocation and 12-month history",
            description = "RMs see portfolios of their own clients; clients see only their own.")
    public PortfolioDetail get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return portfolios.detail(id, CurrentUser.from(jwt));
    }
}
