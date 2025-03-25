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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = { @Index(name = "idx_url", columnList = "url") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(ListingsDateListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Listing {

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
    private Collection<PriceChange> priceHistory = new ArrayList<>();

    public void updatePriceHistory(Integer value) {
        System.out
                .println("%s setting price to %s. Current value is %s".formatted(this.getAddress(), value, this.price));
        if (value == null && this.price == null) {
            System.out.println("1");
            return;
        }

        var lastPrice = this.priceHistory.stream()
                .collect(Collectors.maxBy(Comparator.comparing(PriceChange::getEffectiveFrom)));

        if (value != null && lastPrice.isPresent()) {
            System.out.println("previous price is %s".formatted(lastPrice.get().getPrice()));
            if (value.equals(lastPrice.get().getPrice())) {
                System.out.println("2");
                return;
            }
        }

        System.out.println("Updating price to %s".formatted(value));

        this.priceHistory.stream()
                .collect(Collectors.maxBy(Comparator.comparing(PriceChange::getEffectiveFrom)))
                .ifPresent(p -> p.setEffectiveTo(this.lastSeen));

        var newPrice = new PriceChange();
        newPrice.setEffectiveFrom(this.lastSeen.plusNanos(1));
        newPrice.setPrice(value);
        newPrice.setListing(this);

        this.priceHistory.add(newPrice);
        this.price = value;
    }

    public boolean equalsByValue(Listing other) {
        return other != null
                && Objects.equals(agency, other.getAgency())
                && Objects.equals(name, other.getName())
                && Objects.equals(address, other.getAddress())
                && Objects.equals(url, other.getUrl())
                && Objects.equals(price, other.getPrice())
                && (size == other.size || size != null && size.equalsByValue(other.getSize()))
                && Objects.equals(buildYear, other.getBuildYear())
                && Objects.equals(monthlyCharge, other.getMonthlyCharge())
                && Objects.equals(rooms, other.getRooms());
    }
}
