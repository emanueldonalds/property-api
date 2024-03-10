package edonalds.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edonalds.model.Listing;
import edonalds.model.ListingsQuery;
import edonalds.model.ScrapeEvent;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.ScrapeHistoryRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final String apiKey;
    private final ScrapeHistoryRepository scrapeHistoryRepo;
    private final ListingsRepository listingsRepo;

    public ListingController(ScrapeHistoryRepository scrapeHistoryRepo, ListingsRepository listingsRepo, @Value("${security.apiKey}") String apiKey) {
        this.scrapeHistoryRepo = scrapeHistoryRepo;
        this.listingsRepo = listingsRepo;
        this.apiKey = apiKey;
    }

    @GetMapping
    public List<Listing> getListings(@Valid ListingsQuery query) {
        var pageable = PageRequest.of(0, query.limit());

        var listings = listingsRepo.findAll((root, criteriaQuery, builder) -> {
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

            return builder.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return listings.getContent();
    }

    @PutMapping
    public ResponseEntity<Void> updateListings(
            @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
            @RequestBody List<Listing> listingsParam) {

        if (apiKeyHeader == null || apiKeyHeader.isBlank() || !apiKeyHeader.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        var currentListings = listingsRepo.findByDeletedOrUrlIn(
                false,
                listingsParam.stream().map(l -> l.getUrl()).toList());

        // Delete
        var toDelete = currentListings.stream()
                .filter(cl -> !listingsParam.contains(cl))
                .collect(Collectors.toList());

        toDelete.forEach(cl -> {
            cl.setDeleted(true);
        });

        // Add
        var listingsToAdd = new ArrayList<>(listingsParam);
        listingsToAdd.removeAll(currentListings);
        currentListings.addAll(listingsToAdd);

        // Update
        var listingsToUpdate = currentListings.stream()
                .filter(cl -> listingsParam.stream()
                        .anyMatch(lp -> lp.equals(cl)))
                .collect(Collectors.toList());

        for (Listing listingToUpdate : listingsToUpdate) {
            listingsParam.stream()
                    .filter(listingParam -> listingParam.equals(listingToUpdate))
                    .findFirst()
                    .ifPresent(listingParam -> {
                        listingToUpdate.updatePrice(listingParam.getPrice());
                    });
        }

        currentListings.forEach(l -> l.setLastSeen(OffsetDateTime.now()));

        long nAdded = listingsToAdd.size();
        long nUpdated = listingsToUpdate.size();
        long nDeleted = toDelete.size();
        var scrapeEvent = new ScrapeEvent(nAdded, nUpdated, nDeleted);

        listingsRepo.saveAll(currentListings);
        scrapeHistoryRepo.save(scrapeEvent);

        return ResponseEntity.ok().build();
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
