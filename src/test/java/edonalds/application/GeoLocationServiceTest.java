package edonalds.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import edonalds.client.NominatimClient;
import edonalds.client.NominatimResponse;
import edonalds.model.GeoLocation;
import edonalds.model.Listing;
import edonalds.persistence.ListingsRepository;

@ActiveProfiles("test")
@SpringBootTest
public class GeoLocationServiceTest {

    @Autowired
    GeoLocationService geoLocationService;

    @Autowired
    ListingsRepository listingsRepository;

    @MockBean
    NominatimClient nominatimClient;

    @Test
    void updatesGeoLocationForNewListings() throws InterruptedException, ExecutionException, TimeoutException {
        var l1 = TestHelper.newListing("l1");
        var l2 = TestHelper.newListing("l2");
        var l3 = TestHelper.newListing("l3");

        var existingGeoLocation = GeoLocation.builder()
            .listing(l1)
            .lat(50.123)
            .lon(60.123)
            .build();

        l1.setGeoLocation(existingGeoLocation);

        var saved = listingsRepository.saveAll(List.of(l1, l2, l3));
        var ids = StreamSupport.stream(saved.spliterator(), false).map(Listing::getId).toList();

        var mockResponse = new NominatimResponse(60.0969078, 19.1234567);
        Mockito
            .when(nominatimClient.search(Mockito.any()))
            .thenReturn(new NominatimResponse[]{mockResponse});

        geoLocationService.addGeoLocations(ids).get(10, TimeUnit.SECONDS);

        Mockito.verify(nominatimClient, Mockito.times(2)).search(Mockito.any());

        var gl1 = findListing(l1).getGeoLocation();
        var gl2 = findListing(l2).getGeoLocation();
        var gl3 = findListing(l3).getGeoLocation();

        assertEquals(existingGeoLocation.getLat(), gl1.getLat());
        assertEquals(existingGeoLocation.getLon(), gl1.getLon());
        assertEquals(mockResponse.lat(), gl2.getLat());
        assertEquals(mockResponse.lon(), gl2.getLon());
        assertEquals(mockResponse.lat(), gl3.getLat());
        assertEquals(mockResponse.lon(), gl3.getLon());
    }

    private Listing findListing(Listing listing) {
        return listingsRepository.findAll().stream()
            .filter(l -> l.getUrl().equals(listing.getUrl()))
            .findFirst()
            .orElseThrow();
    }
}
