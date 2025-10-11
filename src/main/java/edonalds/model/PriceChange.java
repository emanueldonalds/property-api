package edonalds.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class PriceChange implements TemporalRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    private Integer price;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="listing_id")
    @JsonBackReference
    private Listing listing;

    @Override
    public OffsetDateTime from() {
        return effectiveFrom;
    }
    @Override
    public OffsetDateTime to() {
        return effectiveTo;
    }
}
