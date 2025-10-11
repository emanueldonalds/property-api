package edonalds.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import edonalds.model.DailyStatistic;
import edonalds.model.PriceChange;
import edonalds.model.Size;
import edonalds.model.TemporalRange;
import edonalds.persistence.ListingsRepository;
import edonalds.persistence.PriceChangeRepository;
import edonalds.persistence.StatisticsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class StatisticsJob {

    private final ListingsRepository listingsRepository;
    private final PriceChangeRepository priceChangeRepository;
    private final StatisticsRepository statisticsRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void run() {
        log.info("Running statistics");

        var listings = listingsRepository.findAll();
        log.info("Count of listings {}", listings.size());

        var priceChanges = priceChangeRepository.findAll();
        log.info("Count of price changes {}", priceChanges.size());

        var period = priceChangeRepository.getPeriod();
        log.info("Period is from {} to {} equals? {}", period.getFrom(), period.getTo(),
                period.getFrom().equals(period.getTo()));

        var stats = new ArrayList<DailyStatistic>();

        for (LocalDate date = period.getFrom(); !date.equals(period.getTo().plusDays(1)); date = date.plusDays(1)) {
            log.info("Running date {}", date);
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

            var nListings = listings.stream()
                    .filter(inRange(date))
                    .count();

            stats.add(DailyStatistic.builder()
                    .date(date)
                    .avgPrice(avgPrice)
                    .avgPriceM2(avgPriceM2)
                    .nListings(nListings)
                    .build());
        }

        statisticsRepository.saveAll(stats);
        log.info("Statistics finished");
    }

    // Stupid function that exist for no reason and should just be a normal lambda.
    // Just practicing some of that needlessly functional stupid fucking abstract
    // mega-indirection quantum entanglement bullshit that my colleagues seem to
    // consider good code at work.
    private Predicate<TemporalRange> inRange(LocalDate date) {
        return range -> contains(range, date);
    }

    // Needlessly generic contains method
    private boolean contains(TemporalRange range, LocalDate date) {
        var from = range.from() == null ? LocalDate.MIN.toEpochDay() : range.from().toLocalDate().toEpochDay();
        var to = range.to() == null ? LocalDate.MAX.toEpochDay() : range.to().toLocalDate().toEpochDay();
        var current = date.toEpochDay();
        return from <= current && to >= current;
    }
}
