package com.example.digitCurrencyPlatform.service;

import com.example.digitCurrencyPlatform.enums.BinanceInterval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.repository.KlineRepository;
import com.example.digitCurrencyPlatform.service.provider.KlineDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Fetch and save kline data using parallel streams
     */
    public void fetchAndSaveKlines(String providerName, String symbol, BinanceInterval interval, long startTime, long endTime, int limit) {
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

    /**
     * Retrieve limit number of klines with startTime, endTime
     */
    public List<Kline> getKlines(String symbol, long startTime, long endTime, int limit) {
        List<Kline> klines = klineRepository.findRecentKlineData(symbol, startTime, endTime, limit);
        System.out.println("Number of klines retrieved from database: " + klines.size());
        return klines;
    }

    /**
     * Retrieve klines with interval enum support
     */
    public List<Kline> getKlinesByStartTimeAndInterval(String symbol, BinanceInterval interval, long startTime, int limit) {
        long endTime = startTime + interval.getMilliseconds() + 1;
        return getKlines(symbol, startTime, endTime, limit);
    }


    // Helpers:
    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }

    private List<TimeRange> createTimeRanges(long startTime, long endTime, long limit) {
        List<TimeRange> ranges = new ArrayList<>();

        for (long current = startTime; current < endTime; current += limit) {
            long rangeEnd = Math.min(current + limit, endTime);
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
