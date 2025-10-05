package edonalds.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.http.HttpRequest.BodyPublishers;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edonalds.model.Listing;
import edonalds.model.Size;
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

    @Test
    void firstTimeRunAddsNewListings() throws Exception {
        var listing = new Listing();
        listing.setAgency("agency1");
        listing.setName("");
        listing.setAddress("");
        listing.setUrl("");
        listing.setPrice(100000);
        listing.setSize(new Size(40f, "m2"));
        listing.setBuildYear(1998);
        listing.setMonthlyCharge(140f);
        listing.setRooms(3);
        listing.setVisibility(Arrays.asList("ALAND"));

        mvc.perform(put("/listings")
                .header("x-api-key", "123")
                .header("Content-Type", "application/json")
                .content(objectMapper.writeValueAsString(Arrays.asList(listing))))
                .andExpect(status().isOk());
    }
}
