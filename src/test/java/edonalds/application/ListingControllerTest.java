package edonalds.application;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import edonalds.model.Listing;
import edonalds.model.Size;
import edonalds.persistence.ListingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class ListingControllerTest {
    Faker faker = new Faker();

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mvc;

    @Autowired
    ListingsRepository listingsRepository;

    @Autowired
    EntityManager em;

    @Test
    void firstTimeRunAddsNewListings() throws Exception {
        var listings = Arrays.asList(
                getListing("l1"),
                getListing("l2"),
                getListing("l3"));

        putListings(listings).andExpect(status().isOk());

        getListings()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name", is(listings.get(0).getName())))
                .andExpect(jsonPath("$.data[1].name", is(listings.get(1).getName())))
                .andExpect(jsonPath("$.data[2].name", is(listings.get(2).getName())));
    }

    @Test
    void deleteListing() throws Exception {
        var l1 = getListing("l1");
        var l2 = getListing("l2");
        var l3 = getListing("l3");

        putListings(Arrays.asList(l1, l2, l3));
        getListings().andExpect(jsonPath("$.data", hasSize(3)));

        putListings(Arrays.asList(l1, l3));
        getListings()
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].name", is(l1.getName())));
    }

    @Test
    void deleteAndAddBackListing() throws Exception {
        var l1 = getListing("l1");
        var l2 = getListing("l2");
        var l3 = getListing("l3");

        putListings(Arrays.asList(l1, l2, l3));
        putListings(Arrays.asList(l1, l3));
        putListings(Arrays.asList(l1, l2, l3));
        getListings().andExpect(jsonPath("$.data", hasSize(3)));

        var all = listingsRepository.findAll();
        assertEquals(4, all.size());
    }

    private Listing getListing(String name) {
        var listing = new Listing();
        em.detach(listing);
        listing.setAgency(faker.lorem().word());
        listing.setName(name);
        listing.setAddress(faker.address().streetAddress());
        listing.setUrl("https://example.com/" + name);
        listing.setPrice(100000);
        listing.setSize(new Size(40f, "m2"));
        listing.setBuildYear(1998);
        listing.setMonthlyCharge(140f);
        listing.setRooms(3);
        listing.setVisibility(Arrays.asList("ALAND"));
        return listing;
    }

    private ResultActions getListings() throws Exception {
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
