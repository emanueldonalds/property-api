package edonalds.application;

import java.util.List;

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
    @Transactional
    public void addGeoLocations(List<Listing> listings) {
        log.info("Geolocation start...");

        var targetListings = listingsRepository.findByIdInAndGeoLocationIsNull(
            listings.stream()
                .map(l -> l.getId())
                .toList()
        );

        log.info("{} listings are missing geolocations. Querying from Nominatim.", targetListings.size());
        int succeeded = 0;
        int failed = 0;

        for (Listing listing : targetListings) {
            var geoLocation = addGeoLocation(listing);

            if ("SUCCESS".equals(geoLocation.getQueryResult())) {
                succeeded++;
            }
            else {
                failed++;
            }
        }

        listingsRepository.saveAll(targetListings);

        log.info("{} nomanitim requests succeeded, {} failed.", succeeded, failed);
        log.info("Fetching geolocations done.");
    }

    public GeoLocation addGeoLocation(Listing listing) {
        var geoLocationBuilder = GeoLocation.builder();
        geoLocationBuilder.listing(listing);
        geoLocationBuilder.geocodingService("Nominatim");

        var request = new NominatimRequest(listing.getAddress() + ", Åland");
        var searchResult = nominatimClient.search(request);

        if (searchResult.length == 0) {
            geoLocationBuilder.queryResult("FAILURE");
        }
        else {
            var first = searchResult[0];

            geoLocationBuilder.queryResult("SUCCESS");
            geoLocationBuilder.lat(first.lat());
            geoLocationBuilder.lon(first.lon());
        }

        var geoLocation = geoLocationBuilder.build();

        listing.setGeoLocation(geoLocation);
        return geoLocation;
    }
}
