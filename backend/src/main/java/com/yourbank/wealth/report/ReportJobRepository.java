package com.yourbank.wealth.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {

    @Query("select r from ReportJob r where r.portfolio.id = :portfolioId order by r.requestedAt desc")
    List<ReportJob> findByPortfolioId(@Param("portfolioId") Long portfolioId);
}
