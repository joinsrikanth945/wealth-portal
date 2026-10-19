package com.yourbank.wealth.order;

import com.yourbank.wealth.order.OrderEnums.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    @Query("select o from TradeOrder o join fetch o.instrument join fetch o.portfolio p join fetch p.client "
            + "where p.id in :portfolioIds order by o.createdAt desc")
    List<TradeOrder> findByPortfolioIds(@Param("portfolioIds") List<Long> portfolioIds);

    @Query("select o from TradeOrder o join fetch o.instrument join fetch o.portfolio p join fetch p.client "
            + "where o.status = :status order by o.createdAt")
    List<TradeOrder> findByStatus(@Param("status") Status status);

    @Query("select o from TradeOrder o join fetch o.instrument join fetch o.portfolio p join fetch p.client "
            + "order by o.createdAt desc")
    List<TradeOrder> findAllWithDetails();
}
