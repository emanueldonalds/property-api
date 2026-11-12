package edonalds.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@Table(indexes = {
        @Index(name = "idx_url", columnList = "url"),
        @Index(name = "idx_address", columnList = "address")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(ListingsDateListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Listing implements TemporalRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime firstSeen;

    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime lastSeen;

    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime lastUpdated;

    private String agency;

    private String name;

    private String address;

    @EqualsAndHashCode.Include
    private String url;

    private Integer price;

    @Embedded
    private Size size;

    private Integer buildYear;

    private Float monthlyCharge;

    private Integer rooms;

    @ElementCollection
    @CollectionTable(name = "listing_visibility", joinColumns = @JoinColumn(name = "listing_id"))
    private List<String> visibility;

    private boolean deleted;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    @JsonManagedReference
    @Builder.Default
    private Collection<PriceChange> priceHistory = new ArrayList<>();

    @OneToOne(mappedBy = "listing", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private GeoLocation geoLocation;

    public void updatePriceHistory(Integer value) {
        log.debug("Checking if price history needs to be updated on {}. Current price: {}", this.url, this.price);
        if (value == null && this.price == null) {
            log.debug("Both inputs were null, returning.");
            return;
        }

        var lastPrice = this.priceHistory.stream()
                .collect(Collectors.maxBy(Comparator.comparing(PriceChange::getEffectiveFrom)));

        if (value != null && lastPrice.isPresent()) {
            log.debug("Previous price is {}", lastPrice.get().getPrice());
            if (value.equals(lastPrice.get().getPrice())) {
                return;
            }
        }


        lastPrice.ifPresent(p -> p.setEffectiveTo(this.lastSeen));

        var newPrice = new PriceChange();
        newPrice.setEffectiveFrom(this.lastSeen.plusNanos(1));
        newPrice.setPrice(value);
        newPrice.setListing(this);

        this.priceHistory.add(newPrice);
        this.price = value;
        log.debug("Update price to {}. There are now {} price history entries for this listing", value, this.priceHistory.size());
    }

    public boolean equalsByValue(Listing other) {
        log.debug("Comparing {} to {}", this.getUrl(), other.getUrl());
        var equals = other != null
                && Objects.equals(agency, other.getAgency())
                && Objects.equals(name, other.getName())
                && Objects.equals(address, other.getAddress())
                && Objects.equals(url, other.getUrl())
                && Objects.equals(price, other.getPrice())
                && (size == other.size || size != null && size.equalsByValue(other.getSize()))
                && Objects.equals(buildYear, other.getBuildYear())
                && Objects.equals(monthlyCharge, other.getMonthlyCharge())
                && Objects.equals(rooms, other.getRooms());
        log.debug("Equals? " + equals);
        return equals;
    }

    @Override
    public OffsetDateTime from() {
        return firstSeen;
    }

    @Override
    public OffsetDateTime to() {
        return lastSeen;
    }
}
