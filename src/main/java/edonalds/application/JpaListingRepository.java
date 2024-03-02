package edonalds.application;

import edonalds.model.Listing;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

@Primary
public interface JpaListingRepository extends CrudRepository<Listing, Long> {
    List<Listing> findByDeletedOrderByFirstSeenDesc(boolean deleted);
    List<Listing> findByDeletedOrUrlInOrderByFirstSeenDesc(boolean deleted, Collection<String> urls);
}
