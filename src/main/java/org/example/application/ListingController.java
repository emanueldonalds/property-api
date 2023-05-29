package org.example.application;

import org.example.model.Listing;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final JpaListingRepository repository;

    public ListingController(JpaListingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Listing> getListings() {
        return repository.findAll();
    }

    @PutMapping
    public ResponseEntity<Void> updateListings(@RequestBody List<Listing> listingsParam) {
        var currentListings = repository.findAll();
        var updatedListings = new ArrayList<Listing>();

        for (var listing : listingsParam){
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