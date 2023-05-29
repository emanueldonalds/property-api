package org.example.application;

import org.example.model.Listing;

import java.util.List;

public interface ListingRepository {
    List<Listing> findAll();
    List<Listing> saveAll(List<Listing> listings);
}
