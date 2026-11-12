package edonalds.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edonalds.model.GetListingsResponse;
import edonalds.model.Listing;
import edonalds.model.ListingsQuery;
import edonalds.model.ScrapeEvent;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.ScrapeHistoryRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListingService {

    private final ScrapeHistoryRepository scrapeHistoryRepo;
    private final ListingsRepository listingsRepo;

    @Transactional
    public GetListingsResponse getListings(ListingsQuery query) {
        var pageable = PageRequest.of(0, query.limit());

        var listingsPage = listingsRepo.findAll((root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();

            predicates.add(builder.equal(root.get("deleted"), query.deleted()));

            if (query.id() != null) {
                predicates.add(builder.equal(root.get("id"), query.id()));
            }

            if (query.agency() != null) {
                Expression<String> agencyExpr = root.get("agency");
                predicates.add(agencyExpr.in(query.agency()));
            }

            if (query.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), query.minPrice()));
            }

            if (query.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), query.maxPrice()));
            }

            if (query.minRooms() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("rooms"), query.minRooms()));
            }

            if (query.maxRooms() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("rooms"), query.maxRooms()));
            }

            if (query.orderByDesc() != null) {
                criteriaQuery.orderBy(builder.desc(root.get(query.orderByDesc())));
            }

            if (query.orderByAsc() != null) {
                criteriaQuery.orderBy(builder.asc(root.get(query.orderByAsc())));
            }

            if (query.visibility() != null && !query.visibility().isEmpty()) {
                Join<Listing, String> visibilityJoin = root.join("visibility");
                predicates.add(visibilityJoin.in(query.visibility()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        var listings = listingsPage.getContent();

        ScrapeEvent lastScrape = scrapeHistoryRepo.findAll().stream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .findFirst()
            .orElse(null);

        return new GetListingsResponse(listings, lastScrape);
    }

    @Transactional
    public List<Listing> updateListings(List<Listing> listingsRequest) {
        var currentListings = listingsRepo.findByDeleted(false);
        log.debug("Number of current listings: {}", currentListings.size());

        List<Listing> added = getAdded(listingsRequest, currentListings);
        List<Listing> updated = getUpdated(listingsRequest, currentListings);
        List<Listing> deleted = getDeleted(listingsRequest, currentListings);
        List<Listing> untouched = new ArrayList<Listing>(currentListings).stream()
            .filter(l -> !added.contains(l))
            .filter(l -> !updated.contains(l))
            .filter(l -> !deleted.contains(l))
            .collect(Collectors.toList());

        log.debug("Added {}", added.size());
        log.debug("Updated {}", updated.size());
        log.debug("Deleted {}", deleted.size());
        log.debug("Untouched {}", untouched.size());

        log.debug("Added list:");
        added.forEach(l -> log.debug(l.getUrl()));
        log.debug("Updated list:");
        updated.forEach(l -> log.debug(l.getUrl()));
        log.debug("Deleted list:");
        deleted.forEach(l -> log.debug(l.getUrl()));

        setLastSeen(added);
        setLastSeen(updated);
        setLastSeen(untouched);

        List<Listing> result = new ArrayList<Listing>();
        result.addAll(added);
        result.addAll(updated);
        result.addAll(deleted);
        result.addAll(untouched);

        // Update prices
        listingsRequest.forEach(listingRequest -> result.stream()
            .filter(x -> Objects.equals(x.getUrl(), listingRequest.getUrl()))
            .findFirst()
            .ifPresent(x -> x.updatePriceHistory(listingRequest.getPrice())));

        long nTotal = result.size() - deleted.size();
        var scrapeEvent = new ScrapeEvent(added.size(), updated.size(), deleted.size(), nTotal);

        log.debug("Scrape event: {}", scrapeEvent.toString());

        listingsRepo.saveAll(result);
        scrapeHistoryRepo.save(scrapeEvent);
        return result;
    }

    private void setLastSeen(List<Listing> listings) {
        listings.forEach(l -> l.setLastSeen(OffsetDateTime.now()));
    }

    private List<Listing> getUpdated(List<Listing> listingsRequest, List<Listing> currentListings) {
        var updated = new ArrayList<Listing>();

        for (Listing currentListing : currentListings) {
            for (Listing listingParam : listingsRequest) {
                if (Objects.equals(currentListing.getUrl(), listingParam.getUrl())) {
                    if (!currentListing.equalsByValue(listingParam)) {
                        updated.add(currentListing);
                        currentListing.setAgency(listingParam.getAgency());
                        currentListing.setName(listingParam.getName());
                        currentListing.setAddress(listingParam.getAddress());
                        currentListing.setUrl(listingParam.getUrl());
                        currentListing.setSize(listingParam.getSize());
                        currentListing.setBuildYear(listingParam.getBuildYear());
                        currentListing.setMonthlyCharge(listingParam.getMonthlyCharge());
                        currentListing.setRooms(listingParam.getRooms());
                    }
                }

            }
        }
        return updated;
    }

    private ArrayList<Listing> getAdded(List<Listing> listingsRequest, List<Listing> currentListings) {
        var added = new ArrayList<>(listingsRequest);
        added.removeAll(currentListings);
        return added;
    }

    private List<Listing> getDeleted(List<Listing> listingsRequest, List<Listing> currentListings) {
        var deleted = currentListings.stream()
            .filter(l -> !listingsRequest.contains(l))
            .collect(Collectors.toList());
        deleted.forEach(l -> l.setDeleted(true));
        return deleted;
    }
}
