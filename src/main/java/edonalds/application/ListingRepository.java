package edonalds.application;

import edonalds.model.Listing;

import java.util.List;

public interface ListingRepository {
    List<Listing> findAll();
    List<Listing> saveAll(List<Listing> listings);
}
