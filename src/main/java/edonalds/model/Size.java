package edonalds.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {
    @Column(name = "size_value")
    private Float value;
    @Column(name = "size_name")
    private String unit;

    public boolean equalsByValue(Size other) {
        return Objects.equals(value, other.getValue()) && Objects.equals(unit, other.unit);
    }
}
