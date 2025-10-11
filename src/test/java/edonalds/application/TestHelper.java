package edonalds.application;

import java.time.OffsetDateTime;
import java.util.Arrays;

import com.github.javafaker.Faker;

import edonalds.model.Listing;
import edonalds.model.Listing.ListingBuilder;
import edonalds.model.Size;

public final class TestHelper {

    private static final Faker faker = new Faker();

    public static Listing newListing() {
        return newListingBuilder().build();
    }

    public static Listing newListing(String name) {
        return newListingBuilder()
            .name(name)
            .url("https://example.com/" + name)
            .build();
    }

    public static ListingBuilder newListingBuilder() {
        var name = faker.lorem().word();
        return Listing.builder()
        .agency(faker.lorem().word())
        .name(name)
        .lastSeen(OffsetDateTime.now())
        .address(faker.address().streetAddress())
        .url("https://example.com/" + name)
        .price(100000)
        .size(new Size(40f, "m2"))
        .buildYear(1998)
        .monthlyCharge(140f)
        .rooms(3)
        .visibility(Arrays.asList("ALAND"));
    }

}
