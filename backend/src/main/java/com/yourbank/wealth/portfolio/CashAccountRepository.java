package com.yourbank.wealth.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashAccountRepository extends JpaRepository<CashAccount, Long> {

    @Query("select c from CashAccount c where c.portfolio.id = :portfolioId order by c.currency")
    List<CashAccount> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    @Query("select c from CashAccount c where c.portfolio.id = :portfolioId and c.currency = :currency")
    Optional<CashAccount> findByPortfolioIdAndCurrency(
            @Param("portfolioId") Long portfolioId, @Param("currency") String currency);
}
