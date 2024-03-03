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
import edonalds.persistence.ListingsRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final String apiKey;
    private final ListingsRepository repository;

    public ListingController(ListingsRepository repository, @Value("${security.apiKey}") String apiKey) {
        this.repository = repository;
        this.apiKey = apiKey;
    }

    @GetMapping
    public List<Listing> getListings(@Valid ListingsQuery filter) {
        var pageable = PageRequest.of(0, filter.limit());

        var listings = repository.findAll((root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            predicates.add(builder.equal(root.get("deleted"), filter.deleted()));

            if (filter.id() != null) {
                predicates.add(builder.equal(root.get("id"), filter.id()));
            }

            if (filter.agency() != null) {
                Expression<String> agencyExpr = root.get("agency");
                predicates.add(agencyExpr.in(filter.agency()));
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

        var currentListings = repository.findByDeletedOrUrlInOrderByFirstSeenDesc(false,
                listingsParam.stream().map(l -> l.getUrl()).toList());

        // Delete
        currentListings.stream()
                .filter(cl -> !listingsParam.contains(cl))
                .forEach(cl -> {
                    System.out.println("Deleting");
                    cl.setDeleted(true);
                });

        // Add
        var listingsToAdd = new ArrayList<>(listingsParam);
        listingsToAdd.removeAll(currentListings);
        currentListings.addAll(listingsToAdd);

        // Update
        for (Listing currentListing : currentListings) {
            listingsParam.stream()
                    .filter(listingParam -> listingParam.equals(currentListing))
                    .findFirst()
                    .ifPresent(listingParam -> {
                        currentListing.updatePrice(listingParam.getPrice());
                        currentListing.setDeleted(false);
                    });
        }

        currentListings.forEach(l -> l.setLastSeen(OffsetDateTime.now()));

        repository.saveAll(currentListings);
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
