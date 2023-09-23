package edonalds.application;

import edonalds.model.Listing;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@Primary
public interface JpaListingRepository extends CrudRepository<Listing, Long> {
    List<Listing> findAll();
}
