package edonalds.model;

import java.time.OffsetDateTime;

public interface TemporalRange {
    OffsetDateTime from();
    OffsetDateTime to();
}
