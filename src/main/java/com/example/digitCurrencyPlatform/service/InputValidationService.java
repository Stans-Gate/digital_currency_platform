package com.example.digitCurrencyPlatform.service;

import com.example.digitCurrencyPlatform.enums.Exchange;
import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.exception.*;
import com.example.digitCurrencyPlatform.service.provider.KlineDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class InputValidationService {
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9]{2,20}$");
    private static final long MIN_TIMESTAMP = 946684800000L; // 2000-01-01
    private static final long MAX_TIMESTAMP = 4102444800000L; // 2100-01-01
    private static final long MAX_TIME_RANGE_MS = 365L * 24 * 60 * 60 * 1000;
    private static final int MAX_LIMIT = 5000;

    private final Map<String, KlineDataProvider> providers;
    private final Map<String, Set<String>> symbolCache = new ConcurrentHashMap<>();
    private final Map<String, Long> symbolCacheTimestamps = new ConcurrentHashMap<>();
    private static final long SYMBOL_CACHE_TTL = 24 * 60 * 60 * 1000;

    @Autowired
    public InputValidationService(List<KlineDataProvider> dataProviders) {
        this.providers = new HashMap<>();
        dataProviders.forEach(provider -> {
            this.providers.put(provider.getProviderName().toUpperCase(), provider);
        });
    }

    public void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new SymbolInvalidException("Symbol cannot be null or empty", symbol);
        }

        String trimmedSymbol = symbol.trim().toUpperCase();
        if (!SYMBOL_PATTERN.matcher(trimmedSymbol).matches()) {
            throw new SymbolInvalidException("Symbol must be 2-20 characters containing only uppercase letters and numbers", symbol);
        }

        Set<String> allAvailableSymbols = getAllSymbolsFromAllProviders();

        if (!allAvailableSymbols.isEmpty() && !allAvailableSymbols.contains(trimmedSymbol)) {
            throw new SymbolInvalidException("Symbol '" + trimmedSymbol + "' is not supported by any exchange or not currently trading", symbol);
        }
    }

    private Set<String> getAllSymbolsFromAllProviders() {
        Set<String> allSymbols = new HashSet<>();
        long currentTime = System.currentTimeMillis();

        for (String providerName : providers.keySet()) {
            // Check cache first
            if (symbolCache.containsKey(providerName) &&
                    symbolCacheTimestamps.containsKey(providerName)) {

                long cacheTime = symbolCacheTimestamps.get(providerName);
                if ((currentTime - cacheTime) < SYMBOL_CACHE_TTL) {
                    // Use cached symbols
                    allSymbols.addAll(symbolCache.get(providerName));
                    continue;
                }
            }

            // Cache miss or expired - fetch fresh symbols
            try {
                KlineDataProvider provider = providers.get(providerName);
                Set<String> symbols = provider.fetchAvailableSymbols();

                // Update cache
                symbolCache.put(providerName, new HashSet<>(symbols));
                symbolCacheTimestamps.put(providerName, currentTime);

                allSymbols.addAll(symbols);
                System.out.println("Fetched " + symbols.size() + " symbols from " + providerName);

            } catch (Exception e) {
                System.err.println("Failed to fetch symbols from " + providerName + ": " + e.getMessage());
                // Use cached symbols if available
                Set<String> cachedSymbols = symbolCache.get(providerName);
                if (cachedSymbols != null) {
                    allSymbols.addAll(cachedSymbols);
                }
            }
        }

        return allSymbols;
    }

    public Exchange validateExchange(String exchangeName) {
        if (exchangeName == null || exchangeName.trim().isEmpty()) {
            throw new ExchangeInvalidException("Exchange cannot be null or empty", exchangeName);
        }

        try {
            Exchange exchange = Exchange.fromString(exchangeName.trim());
            if (!exchange.isImplemented()) {
                throw new ExchangeInvalidException("Exchange '" + exchangeName + "' is not yet implemented", exchangeName);
            }
            return exchange;

        } catch (IllegalArgumentException e) {
            throw new ExchangeInvalidException("Invalid exchange '" + exchangeName + "'. Supported exchanges: " + Exchange.getSupportedExchangeNames(), exchangeName);
        }
    }

    public Interval validateInterval(String intervalString) {
        if (intervalString == null || intervalString.trim().isEmpty()) {
            throw new IntervalInputException("Interval cannot be null or empty", intervalString);
        }

        try {
            return Interval.fromString(intervalString.trim());
        } catch (InputInvalidException e) {
            throw new IntervalInputException(
                    "Invalid interval '" + intervalString + "'. Supported intervals: " +
                            "1s, 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M",
                    intervalString);
        }
    }

    public void validateIntervalForExchange(String intervalString, String exchangeName) {
        Interval interval = validateInterval(intervalString);
        Exchange exchange = validateExchange(exchangeName);

        if (!exchange.supportsInterval(intervalString)) {
            throw new IntervalInputException(
                    "Interval '" + intervalString + "' is not supported by " + exchange.getDisplayName() +
                            ". Supported intervals: " + exchange.getSupportedIntervals(),
                    intervalString, exchangeName);
        }
    }

    public void validateTimeRange(Long startTime, Long endTime) {
        if (startTime == null) {
            throw new TimeRangeInvalidException("Start time cannot be null", startTime, endTime);
        }

        if (endTime == null) {
            throw new TimeRangeInvalidException("End time cannot be null", startTime, endTime);
        }

        if (startTime < MIN_TIMESTAMP) {
            throw new TimeRangeInvalidException(
                    "Start time must be after January 1, 2000", startTime, endTime);
        }

        if (endTime > MAX_TIMESTAMP) {
            throw new TimeRangeInvalidException(
                    "End time must be before January 1, 2100", startTime, endTime);
        }

        if (endTime <= startTime) {
            throw new TimeRangeInvalidException(
                    "End time must be after start time", startTime, endTime);
        }

        long timeRange = endTime - startTime;
        if (timeRange > MAX_TIME_RANGE_MS) {
            throw new TimeRangeInvalidException(
                    "Time range cannot exceed 1 year (" + (timeRange / (24 * 60 * 60 * 1000)) + " days requested)",
                    startTime, endTime);
        }

        // Check if times are not too far in the future
        long currentTime = Instant.now().toEpochMilli();
        if (startTime > currentTime + (7 * 24 * 60 * 60 * 1000)) { // 7 days in future
            throw new TimeRangeInvalidException(
                    "Start time cannot be more than 7 days in the future", startTime, endTime);
        }
    }

    public void validateLimit(Integer limit) {
        if (limit == null) {
            throw new LimitInvalidException("Limit cannot be null", limit, MAX_LIMIT);
        }

        if (limit < 1) {
            throw new LimitInvalidException("Limit must be at least 1", limit, MAX_LIMIT);
        }

        if (limit > MAX_LIMIT) {
            throw new LimitInvalidException(
                    "Limit cannot exceed " + MAX_LIMIT, limit, MAX_LIMIT);
        }
    }

    public void validateIntervalCompatibility(String targetInterval, String baseInterval) {
        Interval target = validateInterval(targetInterval);
        Interval base = validateInterval(baseInterval);

        if (target.getMilliseconds() < base.getMilliseconds()) {
            throw new IntervalInputException(
                    "Target interval (" + targetInterval + ") must be greater than or equal to base interval (" + baseInterval + ")",
                    targetInterval);
        }

        // Check if target interval is a multiple of base interval
        long targetMs = target.getMilliseconds();
        long baseMs = base.getMilliseconds();

        if (targetMs % baseMs != 0) {
            throw new IntervalInputException(
                    "Target interval (" + targetInterval + ") must be a multiple of base interval (" + baseInterval + ")",
                    targetInterval);
        }
    }

    public void validateFetchRequest(String exchange, String symbol, String interval,
                                     Long startTime, Long endTime) {
        validateExchange(exchange);
        validateSymbol(symbol);
        validateIntervalForExchange(interval, exchange);
        validateTimeRange(startTime, endTime);
    }

    public void validateRetrieveRequest(String symbol, String interval, String baseInterval,
                                        Long startTime, Long endTime, Integer limit) {
        validateSymbol(symbol);
        validateInterval(interval);
        validateInterval(baseInterval);
        validateIntervalCompatibility(interval, baseInterval);
        validateTimeRange(startTime, endTime);
        validateLimit(limit);
    }

}
