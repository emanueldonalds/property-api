package edonalds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(CreatedListener.class)
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime created;
    private String agency;
    private String name;
    private String address;
    private String url;
    private Integer price;
    @Embedded
    private Size size;
    private Integer year;
    private Float monthlyCharge;
    private Integer rooms;

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object.getClass() != this.getClass()) {
            return false;
        }
        Listing other = (Listing) object;
        if (this.id != null && this.id.equals(other.id)) {
            return true;
        }
        if (this.url != null && this.url.equals(other.url)) {
            return true;
        }
        return false;
    }


}
