package edonalds.model;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@EntityListeners(ScrapeEventDateListener.class)
@NoArgsConstructor
public class ScrapeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime date;
    private long added;
    private long updated;
    private long deleted;
    private long totalActive;

    public ScrapeEvent(
            long added,
            long updated,
            long deleted,
            long totalActive)
    {
        this.added = added;
        this.updated = updated;
        this.deleted = deleted;
        this.totalActive = totalActive;
    }
}
