package edonalds.application;

import java.util.ArrayList;
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
        return repository.findAll();
    }

    @PutMapping
    public ResponseEntity<Void> updateListings(
            @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
            @RequestBody List<Listing> listingsParam) {

        if (apiKeyHeader == null || apiKeyHeader.isBlank() || !apiKeyHeader.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        var currentListings = repository.findAll();
        var updatedListings = new ArrayList<Listing>();

        for (var listing : listingsParam) {
            for (Listing currentListing : currentListings) {
                if (currentListing.equals(listing)) {
                    listing.setId(currentListing.getId());
                    break;
                }
            }
            updatedListings.add(listing);
        }
        var listingsToDelete = currentListings.stream()
                .filter(l -> listingsParam.stream().noneMatch(lp -> lp.equals(l)))
                .toList();

        repository.deleteAll(listingsToDelete);
        repository.saveAll(updatedListings);

        return ResponseEntity.ok().build();
    }
}
