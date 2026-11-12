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

        var request = new NominatimRequest(cleanAddress(listing.getAddress()));

        log.info("Finding geolocation for listing {} with address: {}, using cleaned address: {}.", 
                listing.getId(), listing.getAddress(), request.q());

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

    private String cleanAddress(String rawAddress) {
    if (rawAddress == null) {
        return "";
    }
    
    // Remove postal code patterns like "AX-22100" or "22100"
    String cleaned = rawAddress.replaceAll("(?i)\\b(AX-)?\\d{5}\\b", "");
    
    // Remove apartment/staircase designations (e.g., "lgh 5", "B 16", "C 18", "32 C 18" -> keep just street & house number)
    // Common Finnish/Åland housing formats: "Street 12 A 4", "Street 12 lgh 1", "Street 12 C18"
    cleaned = cleaned.replaceAll("(?i)\\b(lgh|lokal)\\b.*$", ""); // Removes "lgh ..." onwards
    cleaned = cleaned.replaceAll("\\s+[A-Ö]\\s+\\d+", "");       // Removes " C 18" or " B 16"
    cleaned = cleaned.replaceAll("\\s+[A-Ö]\\d+", "");           // Removes " C18"
    
    // Clean up extra punctuation, trailing commas, and multiple spaces
    cleaned = cleaned.replaceAll("[,\\.]+", " ");
    cleaned = cleaned.replaceAll("\\s+", " ").trim();
    
    return cleaned;
}
}
