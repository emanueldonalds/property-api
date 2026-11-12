package edonalds.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import edonalds.client.NominatimClient;
import edonalds.client.NominatimResponse;
import edonalds.model.GeoLocation;
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
    void updatesGeoLocationForNewListings() {
        // Add 3 listings. One of them with geolocation.
        var l1 = TestHelper.newListing("l1");
        var l2 = TestHelper.newListing("l2");
        var l3 = TestHelper.newListing("l3");

        var existingGeoLocation = GeoLocation.builder()
            .listing(l1)
            .lat(50.123)
            .lon(60.123)
            .build();

        l1.setGeoLocation(existingGeoLocation);

        listingsRepository.save(l1);

        Mockito
            .when(nominatimClient.search(Mockito.any()))
            .thenReturn(new NominatimResponse[]{
                new NominatimResponse(60.0969078, 19.1234567)
            });

        geoLocationService.addGeoLocation(l1);
        geoLocationService.addGeoLocation(l2);
        geoLocationService.addGeoLocation(l3);

        assertNotNull(l1.getGeoLocation());
        assertNotNull(l2.getGeoLocation());
        assertNotNull(l3.getGeoLocation());

        assertEquals(existingGeoLocation.getLat(), l1.getGeoLocation().getLat());
        assertEquals(existingGeoLocation.getLon(), l1.getGeoLocation().getLon());
    }
}
