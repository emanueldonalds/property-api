package edonalds.application;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edonalds.client.NominatimClient;
import edonalds.client.NominatimRequest;
import edonalds.model.GeoLocation;
import edonalds.model.Listing;
import edonalds.persistence.ListingsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class GeoLocationService {

    private final ListingsRepository listingsRepository;
    private final NominatimClient nominatimClient;

    @Async("customExecutor")
    public CompletableFuture<List<Listing>> addGeoLocations(List<Long> listingIds) {
        log.info("Geolocation start...");

        var targetListings = listingsRepository.findByIdInAndGeoLocationIsNullAndDeletedIsFalse(listingIds);

        log.info("{} listings are missing geolocations. Querying from Nominatim.", targetListings.size());

        int succeeded = 0;
        int failed = 0;

        for (Listing listing : targetListings) {
            var geoLocation = findGeoLocation(listing);
            listing.setGeoLocation(geoLocation);

            if ("SUCCESS".equals(geoLocation.getQueryResult())) {
                succeeded++;
            } else {
                failed++;
            }
        }

        saveListings(targetListings);

        log.info("{} nomanitim requests succeeded, {} failed.", succeeded, failed);
        log.info("Fetching geolocations done.");

        return CompletableFuture.completedFuture(targetListings);
    }

    @Transactional
    private GeoLocation findGeoLocation(Listing listing) {
        var geoLocationBuilder = GeoLocation.builder();
        geoLocationBuilder.listing(listing);
        geoLocationBuilder.geocodingService("Nominatim");

        var request = new NominatimRequest(AddressCleaner.clean(listing.getAddress()));

        log.info("Finding geolocation for listing {} with address: {}, using cleaned address: {}.",
            listing.getId(), listing.getAddress(), request.q());

        var searchResult = nominatimClient.search(request);

        if (searchResult.length == 0) {
            geoLocationBuilder.queryResult("FAILURE");
        } else {
            var first = searchResult[0];

            geoLocationBuilder.queryResult("SUCCESS");
            geoLocationBuilder.lat(first.lat());
            geoLocationBuilder.lon(first.lon());
        }

        var geoLocation = geoLocationBuilder.build();

        return geoLocation;
    }

    @Transactional
    private void saveListings(List<Listing> listings) {
        listingsRepository.saveAll(listings);
    }
}
