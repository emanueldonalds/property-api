package edonalds.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

public class DateListener {
    @PrePersist
    public void firstSeen(Listing l) {
        if (l.getFirstSeen() == null) {
            l.setFirstSeen(OffsetDateTime.now());
        }
    }

    @PreUpdate
    public void lastUpdated(Listing l) {
        l.setLastUpdated(OffsetDateTime.now());
    }
}
