package edonalds.model;

import java.time.OffsetDateTime;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class ScrapeEventDateListener {
    @PrePersist
    public void firstSeen(ScrapeEvent s) {
        if (s.getDate() == null) {
            s.setDate(OffsetDateTime.now());
        }
    }
}
