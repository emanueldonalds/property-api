package edonalds.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Summary {
    private final int items;
    private final ScrapeEvent lastUpdate;
}
