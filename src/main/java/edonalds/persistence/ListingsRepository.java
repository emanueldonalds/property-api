package edonalds.persistence;

import edonalds.model.Listing;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

@Primary
public interface ListingsRepository extends CrudRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByDeletedOrderByFirstSeenDesc(boolean deleted);
    List<Listing> findByDeletedOrUrlInOrderByFirstSeenDesc(boolean deleted, Collection<String> urls);
    List<Listing> findAll(Specification<Listing> spec);
}