package com.example.digitCurrencyPlatform.service;

import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.InputInvalidException;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.repository.KlineRepository;
import com.example.digitCurrencyPlatform.service.provider.KlineDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KlineService {

    private final KlineRepository klineRepository;
    private final Map<String, KlineDataProvider> providers;

    @Autowired
    public KlineService(KlineRepository klineRepository, List<KlineDataProvider> dataProviders) {
        this.klineRepository = klineRepository;
        this.providers = new HashMap<>();
        dataProviders.forEach(provider -> providers.put(provider.getProviderName().toUpperCase(), provider));
    }

    public void fetchAndSaveKlines(String providerName, String symbol, Interval interval, long startTime, long endTime, int limit) {
        KlineDataProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found");
        }

        long intervalMs = interval.getMilliseconds();
        long gap = (long) limit * intervalMs;

        List<TimeRange> timeRanges = createTimeRanges(startTime, endTime, gap);

        List<Kline> allKlines = timeRanges.parallelStream()
                .map(range -> {
                    System.out.println("Fetching data from " + range.start + " to " + range.end +
                            " on thread: " + Thread.currentThread().getName());
                    return provider.fetchKlines(symbol, interval, range.start, range.end);
                })
                .filter(klines -> !klines.isEmpty())
                .peek(klines -> {
                    // Save each batch immediately to avoid memory issues
                    klineRepository.batchInsert(klines);
                    System.out.println("Inserted " + klines.size() + " records from thread: " +
                            Thread.currentThread().getName());
                })
                .flatMap(List::stream)
                .toList();

        System.out.println("Total fetched and saved: " + allKlines.size() + " kline records");

        //        for (long t = startTime; t < endTime; t += gap) {
//            final long start = t;
//            final long end = Math.min(t + gap, endTime);
//            List<Kline> klines = binanceService.fetchAndSaveKlines(symbol, interval, start, end);
//            System.out.println("Start inserting... " + "count: " + klines.size());
//            klineRepository.batchInsert(klines);
//            System.out.println("Inserted " + klines.size() + " kline records into the database.");
//        }
    }


    public List<Kline> retrieveKlinesWithDifferentIntervals(String symbol, Interval interval, long startTime, long endTime, int limit, Interval baseInterval) {
        int numToRetrieve;
        long targetIntervalMs = interval.getMilliseconds();
        long baseIntervalMs = baseInterval.getMilliseconds();

        if (targetIntervalMs < baseIntervalMs) {
            throw new InputInvalidException("Target interval must be greater than base interval");
        }

        int baseIntervalsPerTarget = (int) (targetIntervalMs / baseIntervalMs);
        numToRetrieve = limit * baseIntervalsPerTarget;

        System.out.println(numToRetrieve);
        List<Kline> klines = klineRepository.retrieveKlineDataWithStartAndEndTime(symbol, startTime, endTime, numToRetrieve);


        if (klines.isEmpty()) {
            return new ArrayList<>();
        }

        if (targetIntervalMs == baseIntervalMs) {
            return klines.parallelStream().limit(limit).collect(Collectors.toList());
        }

        List<Kline> aggregatedKlines = aggregateKlines(klines, interval, baseInterval);

        return aggregatedKlines.parallelStream().limit(limit).collect(Collectors.toList());
    }


    private List<Kline> aggregateKlines(List<Kline> klines, Interval interval, Interval baseInterval) {
        if (interval.getMilliseconds() < baseInterval.getMilliseconds()) {
            throw new IllegalArgumentException();
        }
        long targetIntervalMs = interval.getMilliseconds();
        long baseIntervalMs = baseInterval.getMilliseconds();
        int baseIntervalsPerTarget = (int) (targetIntervalMs / baseIntervalMs);

        klines.sort(Comparator.comparing(Kline::getOpenTime));

        List<Kline> aggregatedKlines = new ArrayList<>();

        long firstKlineTime = klines.get(0).getOpenTime();

        long windowStart = firstKlineTime;

        int currentIndex = 0;

        while (currentIndex < klines.size()) {
            long windowEnd = windowStart + targetIntervalMs;
            List<Kline> windowKlines = new ArrayList<>();

            for (int i = 0; i < baseIntervalsPerTarget; i++) {
                windowKlines.add(klines.get(currentIndex));
                currentIndex++;
            }

            if (!windowKlines.isEmpty()) {
                Kline aggregatedKline = aggregateKlineWindow(windowKlines, windowStart, targetIntervalMs);
                aggregatedKlines.add(aggregatedKline);
            }

            windowStart = windowEnd;
        }

        return aggregatedKlines;
    }


    private Kline aggregateKlineWindow(List<Kline> klines, long windowStart, long intervalMs) {
        if (klines.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate empty kline list");
        }

        String symbol = klines.get(0).getSymbol();
        long openTime = windowStart;
        long closeTime = windowStart + intervalMs - 1;

        // first kline's open price
        BigDecimal openPrice = klines.get(0).getOpenPrice();

        // last kline's open price
        BigDecimal closePrice = klines.get(klines.size() - 1).getClosePrice();

        // High price: max high price of all klines in this timeWindow
        BigDecimal highPrice = klines.stream()
                .map(Kline::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Low price: min low price of all klines in the window
        BigDecimal lowPrice = klines.stream()
                .map(Kline::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Volume: sum of all
        BigDecimal volume = klines.stream().map(Kline::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long numberOfTrades = klines.stream().mapToLong(Kline::getNumberOfTrades).sum();

        return new Kline(symbol, openTime, closeTime, openPrice, closePrice, highPrice, lowPrice, volume, numberOfTrades);
    }


    // Helpers:
    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }

    private List<TimeRange> createTimeRanges(Long startTime, Long endTime, Long limit) {
        List<TimeRange> ranges = new ArrayList<>();

        for (Long current = startTime; current < endTime; current += limit) {
            Long rangeEnd = Math.min(current + limit, endTime);
            ranges.add(new TimeRange(current, rangeEnd));
        }

        return ranges;
    }

    private static class TimeRange {
        final long start;
        final long end;

        TimeRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
