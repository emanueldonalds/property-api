package edonalds.model;

import jakarta.persistence.PrePersist;

import java.time.OffsetDateTime;

public class CreatedListener {
    @PrePersist
    public void setLastUpdate(Listing l) {
        if (l.getCreated() == null) {
            l.setCreated(OffsetDateTime.now());
        }
    }
}