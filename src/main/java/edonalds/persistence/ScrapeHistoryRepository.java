package edonalds.persistence;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import edonalds.model.Listing;
import edonalds.model.ScrapeEvent;

@Primary
public interface ScrapeHistoryRepository extends CrudRepository<ScrapeEvent, Long>, JpaSpecificationExecutor<Listing> {
    List<ScrapeEvent> findAll();
}
