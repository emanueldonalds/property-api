package edonalds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.OffsetDateTime;
import java.util.Collection;

@Entity
@Table(indexes = @Index(name = "idx_url", columnList = "url"))
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
    @Column(name = "build_year")
    private Integer year;
    private Float monthlyCharge;
    private Integer rooms;
    private boolean deleted;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Collection<PriceChange> priceHistory;

    public void updatePrice(Integer newPrice) {
        if (newPrice == null && this.price == null) {
            return;
        }
        if (newPrice != null && newPrice.equals(this.price)) {
            return;
        }

        var oldPrice = new PriceChange();
        oldPrice.setLastSeen(lastSeen);
        oldPrice.setPrice(this.price);
        oldPrice.setListing(this);

        this.priceHistory.add(oldPrice);
        this.price = newPrice;

    }
}
