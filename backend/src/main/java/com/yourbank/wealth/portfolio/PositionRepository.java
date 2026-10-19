package com.yourbank.wealth.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("select p from Position p join fetch p.instrument where p.portfolio.id = :portfolioId")
    List<Position> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    @Query("select p from Position p join fetch p.instrument "
            + "where p.portfolio.id = :portfolioId and p.instrument.id = :instrumentId")
    Optional<Position> findOne(@Param("portfolioId") Long portfolioId, @Param("instrumentId") Long instrumentId);
}
