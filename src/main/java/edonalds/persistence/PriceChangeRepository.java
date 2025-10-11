package edonalds.persistence;

import java.time.LocalDate;
import java.time.Period;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import edonalds.model.PriceChange;

public interface PriceChangeRepository extends CrudRepository<PriceChange, Long> {
    List<PriceChange> findAll();

    @Query("SELECT DATE(MIN(effectiveFrom)) as from, DATE(MAX(effectiveFrom)) as to FROM PriceChange")
    StatsPeriod getPeriod();

    //@Query("SELECT MIN(effectiveFrom) FROM PriceChange")
    //OffsetDateTime getMinDate();
}
