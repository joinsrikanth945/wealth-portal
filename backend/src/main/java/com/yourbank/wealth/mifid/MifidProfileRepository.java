package com.yourbank.wealth.mifid;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MifidProfileRepository extends JpaRepository<MifidProfile, Long> {

    /** Newest first. */
    @Query("select m from MifidProfile m where m.client.id = :clientId order by m.createdAt desc, m.id desc")
    List<MifidProfile> findByClientId(@Param("clientId") Long clientId);
}
