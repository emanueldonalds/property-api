package edonalds.persistence;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import edonalds.model.Listing;

@Primary
public interface ListingsRepository extends CrudRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    List<Listing> findAll();
    List<Listing> findAll(Specification<Listing> spec);
    List<Listing> findByDeleted(boolean deleted);
    List<Listing> findByDeletedTrueAndUrlInAndLastSeenAfter(Collection<String> urls, OffsetDateTime lastSeen);

    @Transactional(readOnly = true)
    List<Listing> findByIdInAndGeoLocationIsNullAndDeletedIsFalse(List<Long> ids);
}
