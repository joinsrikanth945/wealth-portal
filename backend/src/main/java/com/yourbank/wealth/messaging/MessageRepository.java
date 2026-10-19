package com.yourbank.wealth.messaging;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where m.client.id = :clientId order by m.createdAt, m.id")
    List<Message> findByClientId(@Param("clientId") Long clientId);

    @Query("select m from Message m where m.client.id in :clientIds order by m.createdAt, m.id")
    List<Message> findByClientIds(@Param("clientIds") List<Long> clientIds);
}
