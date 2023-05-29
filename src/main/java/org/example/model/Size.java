package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {
    @Column(name = "size_value")
    private Float value;
    @Column(name = "size_name")
    private String unit;
}