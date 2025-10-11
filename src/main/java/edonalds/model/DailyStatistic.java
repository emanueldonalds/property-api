package edonalds.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyStatistic {

    @Id
    private LocalDate date;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private long avgPrice;
    private long avgPriceM2;
    private long nListings;
}
