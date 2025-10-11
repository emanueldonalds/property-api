package edonalds.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edonalds.model.DailyStatistic;
import edonalds.model.PriceChange;
import edonalds.model.Size;
import edonalds.model.TemporalRange;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.PriceChangeRepository;
import edonalds.persistence.StatisticsRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StatisticsJob {

    private final boolean startup;
    private final ListingsRepository listingsRepository;
    private final PriceChangeRepository priceChangeRepository;
    private final StatisticsRepository statisticsRepository;

    public StatisticsJob(@Value("${startup.statistics.enabled:true}") boolean startup,
            ListingsRepository listingsRepository,
            PriceChangeRepository priceChangeRepository,
            StatisticsRepository statisticsRepository) {
        this.startup = startup;
        this.listingsRepository = listingsRepository;
        this.priceChangeRepository = priceChangeRepository;
        this.statisticsRepository = statisticsRepository;
    }

    @PostConstruct
    public void startup() {
        if (this.startup) {
            run();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void run() {
        log.info("Running statistics");

        var listings = listingsRepository.findAll();
        log.info("Count of listings {}", listings.size());

        var priceChanges = priceChangeRepository.findAll();
        log.info("Count of price changes {}", priceChanges.size());

        var period = priceChangeRepository.getPeriod();
        log.info("Period is from {} to {}", period.getFrom(), period.getTo());

        var stats = new ArrayList<DailyStatistic>();

        for (LocalDate date = period.getFrom(); !date.equals(period.getTo().plusDays(1)); date = date.plusDays(1)) {
            var avgPrice = (long) Math.round(priceChanges.stream()
                    .filter(inRange(date))
                    .filter(pc -> pc.getPrice() != null)
                    .mapToInt(PriceChange::getPrice)
                    .average()
                    .orElse(0.0));

            var avgPriceM2 = (long) Math.round(priceChanges.stream()
                    .filter(inRange(date))
                    .filter(pc -> pc.getPrice() != null)
                    .filter(pc -> pc.getListing().getSize() != null)
                    .filter(pc -> "m2".equals(pc.getListing().getSize().getUnit()))
                    .mapToInt(pc -> {
                        Size size = pc.getListing().getSize();
                        var res = pc.getPrice() / size.getValue();
                        return (int) Math.round(res);
                    })
                    .average()
                    .orElse(0.0));

            var medPrice = median(priceChanges.stream()
                    .filter(inRange(date))
                    .filter(pc -> pc.getPrice() != null)
                    .mapToInt(pc -> pc.getPrice())
                    .toArray());

            var medPriceM2 = median(priceChanges.stream()
                    .filter(inRange(date))
                    .filter(pc -> pc.getPrice() != null)
                    .filter(pc -> pc.getListing().getSize() != null)
                    .filter(pc -> "m2".equals(pc.getListing().getSize().getUnit()))
                    .mapToInt(pc -> {
                        Size size = pc.getListing().getSize();
                        var res = pc.getPrice() / size.getValue();
                        return (int) Math.round(res);
                    })
                    .toArray());

            var nListings = listings.stream()
                    .filter(inRange(date))
                    .count();

            stats.add(DailyStatistic.builder()
                    .date(date)
                    .avgPrice(avgPrice)
                    .avgPriceM2(avgPriceM2)
                    .medPrice((long) medPrice)
                    .medPriceM2((long) medPriceM2)
                    .nListings(nListings)
                    .build());
        }

        statisticsRepository.saveAll(stats);
        log.info("Statistics finished");
    }

    private Predicate<TemporalRange> inRange(LocalDate date) {
        return range -> contains(range, date);
    }

    private boolean contains(TemporalRange range, LocalDate date) {
        var from = range.from() == null ? LocalDate.MIN.toEpochDay() : range.from().toLocalDate().toEpochDay();
        var to = range.to() == null ? LocalDate.MAX.toEpochDay() : range.to().toLocalDate().toEpochDay();
        var current = date.toEpochDay();
        return from <= current && to >= current;
    }

    public static int median(int[] numbers) {
        int[] sorted = Arrays.copyOf(numbers, numbers.length);
        Arrays.sort(sorted);

        int n = sorted.length;
        if (n % 2 == 1) {
            return sorted[n / 2];
        } else {
            return (sorted[(n / 2) - 1] + sorted[n / 2]) / 2;
        }
    }

}
