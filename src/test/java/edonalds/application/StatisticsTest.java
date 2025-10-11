package edonalds.application;

import static edonalds.application.TestHelper.newListingBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import edonalds.model.PriceChange;
import edonalds.model.Size;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.PriceChangeRepository;
import edonalds.persistence.StatisticsRepository;
import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StatisticsTest {
    Faker faker = new Faker();

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mvc;

    @Autowired
    ListingsRepository listingsRepository;

    @Autowired
    StatisticsRepository statisticsRepository;

    @Autowired
    PriceChangeRepository priceChangeRepository;

    @Autowired
    StatisticsJob statisticsJob;

    @Autowired
    EntityManager em;

    @Test
    void createStats() throws Exception {
        var l1 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-01T12:00:00Z"))
                .price(100000)
                .size(new Size(100f, "m2"))
                .build();
        var l2 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-02T12:00:00Z"))
                .price(200000)
                .size(new Size(100f, "m2"))
                .build();
        var l3 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-03T12:00:00Z"))
                .price(600000)
                .size(new Size(100f, "m2"))
                .build();
        var l4 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-04T12:00:00Z"))
                .price(1500000)
                .size(new Size(100f, "m2"))
                .build();
        var l5 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-05T12:00:00Z"))
                .price(2600000)
                .size(new Size(100f, "m2"))
                .build();
        var l6 = newListingBuilder()
                .firstSeen(OffsetDateTime.parse("2025-01-06T12:00:00Z"))
                .price(7000000)
                .size(new Size(100f, "m2"))
                .build();

        var listings = Arrays.asList(l1, l2, l3, l4, l5, l6);
        listingsRepository.saveAll(listings);

        var priceChanges = listings.stream().map(listing -> {
            var pc = new PriceChange();
            pc.setListing(listing);
            pc.setPrice(listing.getPrice());
            pc.setEffectiveFrom(listing.getFirstSeen());
            return pc;
        }).toList();

        priceChangeRepository.saveAll(priceChanges);

        statisticsJob.run();

        var stats = statisticsRepository.findAll();

        assertEquals(6, stats.size());

        var s1 = stats.get(0);
        var s2 = stats.get(1);
        var s3 = stats.get(2);
        var s4 = stats.get(3);
        var s5 = stats.get(4);
        var s6 = stats.get(5);

        assertEquals(100000, s1.getAvgPrice());
        assertEquals(150000, s2.getAvgPrice());
        assertEquals(300000, s3.getAvgPrice());
        assertEquals(600000, s4.getAvgPrice());
        assertEquals(1000000, s5.getAvgPrice());
        assertEquals(2000000, s6.getAvgPrice());

        assertEquals(1000, s1.getAvgPriceM2());
        assertEquals(1500, s2.getAvgPriceM2());
        assertEquals(3000, s3.getAvgPriceM2());
        assertEquals(6000, s4.getAvgPriceM2());
        assertEquals(10000, s5.getAvgPriceM2());
        assertEquals(20000, s6.getAvgPriceM2());
    }
}
