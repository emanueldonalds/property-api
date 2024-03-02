package edonalds.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edonalds.model.Listing;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final String apiKey;

    private final JpaListingRepository repository;

    public ListingController(JpaListingRepository repository, @Value("${security.apiKey}") String apiKey) {
        this.repository = repository;
        this.apiKey = apiKey;
    }

    @GetMapping
    public List<Listing> getListings() {
        var r = repository.findByDeletedOrderByFirstSeenDesc(false);
        return r;
    }

    @PutMapping
    public ResponseEntity<Void> updateListings(
            @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
            @RequestBody List<Listing> listingsParam) {

        if (apiKeyHeader == null || apiKeyHeader.isBlank() || !apiKeyHeader.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        var currentListings = repository.findByDeletedOrUrlInOrderByFirstSeenDesc(false, listingsParam.stream().map(l -> l.getUrl()).toList());

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
}
