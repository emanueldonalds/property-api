package edonalds.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class GetListingsResponse {
    private final List<Listing> data;
    private final ScrapeEvent lastUpdate;
}
