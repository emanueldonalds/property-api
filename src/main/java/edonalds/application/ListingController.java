package edonalds.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edonalds.model.GetListingsResponse;
import edonalds.model.Listing;
import edonalds.model.ListingsQuery;
import edonalds.model.ScrapeEvent;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.ScrapeHistoryRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final String apiKey;
    private final ScrapeHistoryRepository scrapeHistoryRepo;
    private final ListingsRepository listingsRepo;

    public ListingController(ScrapeHistoryRepository scrapeHistoryRepo, ListingsRepository listingsRepo,
            @Value("${security.apiKey}") String apiKey) {
        this.scrapeHistoryRepo = scrapeHistoryRepo;
        this.listingsRepo = listingsRepo;
        this.apiKey = apiKey;
    }

    @GetMapping
    @Transactional
    public GetListingsResponse getListings(@Valid ListingsQuery query) {
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

    @PutMapping
    public ResponseEntity<Void> updateListings(
            @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
            @RequestBody List<Listing> listingsRequest) {

        if (apiKeyHeader == null || apiKeyHeader.isBlank() || !apiKeyHeader.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        var currentListings = listingsRepo.findByDeleted(false);

        List<Listing> added = getAdded(listingsRequest, currentListings);
        List<Listing> updated = getUpdated(listingsRequest, currentListings);
        List<Listing> deleted = getDeleted(listingsRequest, currentListings);
        List<Listing> untouched = new ArrayList<Listing>(currentListings).stream()
                .filter(l -> !added.contains(l))
                .filter(l -> !updated.contains(l))
                .filter(l -> !deleted.contains(l))
                .collect(Collectors.toList());

        setLastSeen(added);
        setLastSeen(updated);
        setLastSeen(untouched);

        List<Listing> result = new ArrayList<Listing>();
        result.addAll(added);
        result.addAll(updated);
        result.addAll(deleted);
        result.addAll(untouched);

        System.out.println("About to update prices");

        // Update price history
        listingsRequest.forEach(listingRequest -> result.stream()
                .filter(x -> Objects.equals(x.getUrl(), listingRequest.getUrl()))
                .findFirst()
                .ifPresent(x -> x.updatePriceHistory(listingRequest.getPrice())));

        long nTotal = result.size() - deleted.size();

        var scrapeEvent = new ScrapeEvent(added.size(), updated.size(), deleted.size(), nTotal);

        listingsRepo.saveAll(result);
        scrapeHistoryRepo.save(scrapeEvent);

        return ResponseEntity.ok().build();
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());

        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", errors);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
