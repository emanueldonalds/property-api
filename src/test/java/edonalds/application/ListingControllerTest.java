package edonalds.application;

import static edonalds.application.TestHelper.newListing;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edonalds.model.Listing;
import edonalds.model.PriceChange;
import edonalds.model.Size;
import edonalds.persistence.ListingsRepository;
import jakarta.transaction.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ListingControllerTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mvc;

    @Autowired
    ListingsRepository listingsRepository;

    @Test
    void AddNewListingsFromBlankSlate() throws Exception {
        var listings = Arrays.asList(
                newListing("l1"),
                newListing("l2"),
                newListing("l3"));

        putListings(listings).andExpect(status().isOk());

        newListings()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name", is(listings.get(0).getName())))
                .andExpect(jsonPath("$.data[1].name", is(listings.get(1).getName())))
                .andExpect(jsonPath("$.data[2].name", is(listings.get(2).getName())));
    }

    @Test
    void AddSameListingsMultipleTimes() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        putListings(Arrays.asList(l1, l2, l3));
        putListings(Arrays.asList(l1, l2, l3));
        putListings(Arrays.asList(l1, l2, l3));

        var entities = listingsRepository.findAll();
        assertEquals(3, entities.size());
    }

    @Test
    void deleteListing() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        putListings(Arrays.asList(l1, l2, l3));
        newListings().andExpect(jsonPath("$.data", hasSize(3)));

        putListings(Arrays.asList(l1, l3));
        newListings()
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is(l1.getName())));
    }

    @Test
    void listingThatReturnsSoonAfterBeingDeletedIsRevived() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        l2.setPrice(250000);
        putListings(Arrays.asList(l1, l2, l3));
        var firstSeen = findByName(l2.getName()).get(0).getFirstSeen();

        putListings(Arrays.asList(l1, l3));
        assertTrue(findByName(l2.getName()).get(0).isDeleted());

        l2.setPrice(230000);
        putListings(Arrays.asList(l1, l2, l3));
        newListings().andExpect(jsonPath("$.data", hasSize(3)));

        var all = listingsRepository.findAll();
        assertEquals(3, all.size());

        var revived = findByName(l2.getName());
        assertEquals(1, revived.size());
        assertFalse(revived.get(0).isDeleted());
        assertEquals(firstSeen, revived.get(0).getFirstSeen());
        assertEquals(230000, revived.get(0).getPrice());
        assertEquals(2, revived.get(0).getPriceHistory().size());
    }

    @Test
    void listingThatReturnsLongAfterBeingDeletedIsANewListing() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        putListings(Arrays.asList(l1, l2, l3));
        putListings(Arrays.asList(l1, l3));

        var deleted = findByName(l2.getName()).get(0);
        deleted.setLastSeen(OffsetDateTime.now().minusDays(8));
        listingsRepository.save(deleted);

        putListings(Arrays.asList(l1, l2, l3));
        newListings().andExpect(jsonPath("$.data", hasSize(3)));

        var all = listingsRepository.findAll();
        assertEquals(4, all.size());
        assertEquals(1, findByName(l1.getName()).size());
        assertEquals(1, findByName(l3.getName()).size());

        var l2s = findByName(l2.getName()).stream()
                .sorted(Comparator.comparing(Listing::getFirstSeen))
                .toList();

        assertEquals(2, l2s.size());
        assertTrue(l2s.get(0).isDeleted());
        assertFalse(l2s.get(1).isDeleted());
    }

    @Test
    void updateListing() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        putListings(Arrays.asList(l1, l2, l3));

        newListings()
                .andExpect(jsonPath("$.data[1].name", is(l2.getName())))
                .andExpect(jsonPath("$.data[1].agency", is(l2.getAgency())))
                .andExpect(jsonPath("$.data[1].address", is(l2.getAddress())))
                .andExpect(jsonPath("$.data[1].url", is(l2.getUrl())))
                .andExpect(jsonPath("$.data[1].price", is(l2.getPrice())))
                .andExpect(jsonPath("$.data[1].size.value", is(Double.valueOf(l2.getSize().getValue()))))
                .andExpect(jsonPath("$.data[1].size.unit", is(l2.getSize().getUnit())))
                .andExpect(jsonPath("$.data[1].buildYear", is(l2.getBuildYear())))
                .andExpect(jsonPath("$.data[1].monthlyCharge", is(Double.valueOf(l2.getMonthlyCharge()))))
                .andExpect(jsonPath("$.data[1].rooms", is(l2.getRooms())))
                .andExpect(jsonPath("$.data[1].visibility", is(l2.getVisibility())));

        // There's no use case for updating agency, so it's not implemented.
        // l2.setAgency("updated agency");
        l2.setName("l2a");
        l2.setAddress("updated address");
        l2.setUrl("https://example.com/updated-url");
        l2.setPrice(85000);
        l2.setSize(new Size(23f, "ha"));
        l2.setBuildYear(2004);
        l2.setMonthlyCharge(44f);
        l2.setRooms(4);
        l2.setVisibility(Arrays.asList("FINLAND"));

        putListings(Arrays.asList(l1, l2, l3));

        newListings()
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[1].name", is(l2.getName())))
                .andExpect(jsonPath("$.data[1].agency", is(l2.getAgency())))
                .andExpect(jsonPath("$.data[1].address", is(l2.getAddress())))
                .andExpect(jsonPath("$.data[1].url", is(l2.getUrl())))
                .andExpect(jsonPath("$.data[1].price", is(l2.getPrice())))
                .andExpect(jsonPath("$.data[1].size.value", is(Double.valueOf(l2.getSize().getValue()))))
                .andExpect(jsonPath("$.data[1].size.unit", is(l2.getSize().getUnit())))
                .andExpect(jsonPath("$.data[1].buildYear", is(l2.getBuildYear())))
                .andExpect(jsonPath("$.data[1].monthlyCharge", is(Double.valueOf(l2.getMonthlyCharge()))))
                .andExpect(jsonPath("$.data[1].rooms", is(l2.getRooms())))
                .andExpect(jsonPath("$.data[1].visibility", is(l2.getVisibility())));
    }

    @Test
    void updateAndDeleteAndAddNewListings() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        putListings(Arrays.asList(l1, l2, l3));

        l2.setName("Hej");

        var updatedL3 = newListing("l3");
        updatedL3.setUrl(l3.getUrl());

        var l4 = newListing("l4");

        putListings(Arrays.asList(l2, updatedL3, l4));
        newListings()
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name", is(l2.getName())))
                .andExpect(jsonPath("$.data[0].agency", is(l2.getAgency())))
                .andExpect(jsonPath("$.data[0].address", is(l2.getAddress())))
                .andExpect(jsonPath("$.data[0].url", is(l2.getUrl())))
                .andExpect(jsonPath("$.data[0].price", is(l2.getPrice())))
                .andExpect(jsonPath("$.data[0].size.value", is(Double.valueOf(l2.getSize().getValue()))))
                .andExpect(jsonPath("$.data[0].size.unit", is(l2.getSize().getUnit())))
                .andExpect(jsonPath("$.data[0].buildYear", is(l2.getBuildYear())))
                .andExpect(jsonPath("$.data[0].monthlyCharge", is(Double.valueOf(l2.getMonthlyCharge()))))
                .andExpect(jsonPath("$.data[0].rooms", is(l2.getRooms())))
                .andExpect(jsonPath("$.data[0].visibility", is(l2.getVisibility())))
                .andExpect(jsonPath("$.data[1].name", is(updatedL3.getName())))
                .andExpect(jsonPath("$.data[1].agency", is(updatedL3.getAgency())))
                .andExpect(jsonPath("$.data[1].address", is(updatedL3.getAddress())))
                .andExpect(jsonPath("$.data[1].url", is(updatedL3.getUrl())))
                .andExpect(jsonPath("$.data[1].price", is(updatedL3.getPrice())))
                .andExpect(jsonPath("$.data[1].size.value", is(Double.valueOf(updatedL3.getSize().getValue()))))
                .andExpect(jsonPath("$.data[1].size.unit", is(updatedL3.getSize().getUnit())))
                .andExpect(jsonPath("$.data[1].buildYear", is(updatedL3.getBuildYear())))
                .andExpect(jsonPath("$.data[1].monthlyCharge", is(Double.valueOf(updatedL3.getMonthlyCharge()))))
                .andExpect(jsonPath("$.data[1].rooms", is(updatedL3.getRooms())))
                .andExpect(jsonPath("$.data[1].visibility", is(updatedL3.getVisibility())))
                .andExpect(jsonPath("$.data[2].name", is(l4.getName())))
                .andExpect(jsonPath("$.data[2].agency", is(l4.getAgency())))
                .andExpect(jsonPath("$.data[2].address", is(l4.getAddress())))
                .andExpect(jsonPath("$.data[2].url", is(l4.getUrl())))
                .andExpect(jsonPath("$.data[2].price", is(l4.getPrice())))
                .andExpect(jsonPath("$.data[2].size.value", is(Double.valueOf(l4.getSize().getValue()))))
                .andExpect(jsonPath("$.data[2].size.unit", is(l4.getSize().getUnit())))
                .andExpect(jsonPath("$.data[2].buildYear", is(l4.getBuildYear())))
                .andExpect(jsonPath("$.data[2].monthlyCharge", is(Double.valueOf(l4.getMonthlyCharge()))))
                .andExpect(jsonPath("$.data[2].rooms", is(l4.getRooms())));
    }

    @Test
    void updatePrice() throws Exception {
        var l1 = newListing("l1");
        var l2 = newListing("l2");
        var l3 = newListing("l3");

        l1.setPrice(250000);
        putListings(Arrays.asList(l1, l2, l3));

        l1.setPrice(230000);
        putListings(Arrays.asList(l1, l2, l3));

        l1.setPrice(190000);
        putListings(Arrays.asList(l1, l2, l3));

        var entity = listingsRepository.findAll().stream()
                .filter(x -> Objects.equals(x.getName(), l1.getName()))
                .findFirst()
                .orElse(null);

        assertEquals(190000, entity.getPrice());

        var priceHistory = entity.getPriceHistory().stream()
                .sorted(Comparator.comparing(PriceChange::getEffectiveFrom))
                .toList();

        assertEquals(3, priceHistory.size());
        assertEquals(250000, priceHistory.get(0).getPrice());
        assertEquals(230000, priceHistory.get(1).getPrice());
        assertEquals(190000, priceHistory.get(2).getPrice());
        assertNotNull(priceHistory.get(0).getEffectiveTo());
        assertNotNull(priceHistory.get(1).getEffectiveTo());
        assertNull(priceHistory.get(2).getEffectiveTo());
    }

    private List<Listing> findByName(String name) {
        return listingsRepository.findAll().stream()
                .filter(x -> Objects.equals(x.getName(), name))
                .toList();
    }

    private ResultActions newListings() throws Exception {
        return mvc.perform(get("/listings")
                .queryParam("orderByAsc", "name")
                .header("x-api-key", "123")
                .header("Accept", "application/json"));
    }

    private ResultActions putListings(Object object) throws JsonProcessingException, Exception {
        return mvc.perform(put("/listings")
                .header("x-api-key", "123")
                .header("Content-Type", "application/json")
                .content(objectMapper.writeValueAsString(object)));

    }
}
