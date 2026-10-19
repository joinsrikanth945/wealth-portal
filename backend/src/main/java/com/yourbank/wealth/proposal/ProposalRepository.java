package com.yourbank.wealth.proposal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    @Query("select pr from Proposal pr join fetch pr.portfolio p join fetch p.client "
            + "where p.id in :portfolioIds order by pr.createdAt desc")
    List<Proposal> findByPortfolioIds(@Param("portfolioIds") List<Long> portfolioIds);
}
