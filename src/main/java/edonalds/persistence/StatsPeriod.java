package edonalds.persistence;

import java.time.LocalDate;

public interface StatsPeriod {
    LocalDate getFrom();
    LocalDate getTo(); 
}
