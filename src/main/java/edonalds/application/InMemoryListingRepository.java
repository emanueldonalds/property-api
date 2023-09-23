package edonalds.application;

import edonalds.model.Listing;
import edonalds.model.Size;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class InMemoryListingRepository implements ListingRepository {
    private List<Listing> listings = new ArrayList<>();

    public InMemoryListingRepository() {
        listings.add(new Listing(
                1L,
                OffsetDateTime.now(),
                "Agency 1",
                "Apartment 1",
                "Street 1",
                "url1",
                115000,
                new Size(39.5f, "m2"),
                1986,
                188.50f,
                2));
        listings.add(new Listing(
                2L,
                OffsetDateTime.now(),
                "Agency 1",
                "Apartment 2",
                "Street 2",
                "url2",
                85000,
                new Size(30f, "m2"),
                1970,
                110f,
                1));
        listings.add(new Listing(
                3L,
                OffsetDateTime.now(),
                "Agency 2",
                "Apartment 3",
                "Street 3",
                "url3",
                205000,
                new Size(120f, "m2"),
                1998,
                810f,
                4));
    }

    public List<Listing> findAll() {
        return listings;
    }

    @Override
    public List<Listing> saveAll(List<Listing> listings) {
        this.listings = listings;
        return this.listings;
    }
}
