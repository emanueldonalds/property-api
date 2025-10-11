package edonalds.persistence;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import edonalds.model.DailyStatistic;

public interface StatisticsRepository extends CrudRepository<DailyStatistic, Long> {
    public List<DailyStatistic> findAll();
}
