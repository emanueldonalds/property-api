package edonalds.model;

import java.time.OffsetDateTime;

// Needless interface
public interface TemporalRange {

    OffsetDateTime from();
    OffsetDateTime to();
}
