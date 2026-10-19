package com.yourbank.wealth.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("select p from Portfolio p join fetch p.client where p.client.id = :clientId order by p.id")
    List<Portfolio> findByClientId(@Param("clientId") Long clientId);

    @Query("select p from Portfolio p join fetch p.client where p.client.id in :clientIds order by p.id")
    List<Portfolio> findByClientIds(@Param("clientIds") List<Long> clientIds);
}
