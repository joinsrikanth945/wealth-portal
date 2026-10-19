package com.yourbank.wealth.instrument;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    List<Instrument> findAllByOrderByNameAsc();
}
