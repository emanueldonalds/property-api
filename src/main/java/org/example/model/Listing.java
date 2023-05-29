package org.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
