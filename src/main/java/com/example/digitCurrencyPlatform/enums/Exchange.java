package com.example.digitCurrencyPlatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum Exchange {
    BINANCE("Binance",
            "https://api.binance.com/api/v3",
            "https://api.binance.com/api/v3/exchangeInfo",
            "https://api.binance.com/api/v3/klines",
            1200,  // requests per minute
            5000,  // max klines per request
            Duration.ofSeconds(30),
            Set.of("1s", "1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d", "1w", "1M"),
            true
    ),

    BINANCE_US("Binance US",
            "https://api.binance.us/api/v3",
            "https://api.binance.us/api/v3/exchangeInfo",
            "https://api.binance.us/api/v3/klines",
            1200,  // requests per minute
            1000,  // max klines per request
            Duration.ofSeconds(30),
            Set.of("1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d", "1w", "1M"),
            true
    ),

    COINBASE(
            "Coinbase Pro",
            "https://api.exchange.coinbase.com",
            "https://api.exchange.coinbase.com/products",
            "https://api.exchange.coinbase.com/products/{symbol}/candles",
            10,    // requests per second (much more restrictive)
            300,   // max candles per request
            Duration.ofSeconds(60),
            Set.of("1m", "5m", "15m", "1h", "6h", "1d"),
            false
    ),

    KRAKEN(
            "Kraken",
            "https://api.kraken.com/0/public",
            "https://api.kraken.com/0/public/AssetPairs",
            "https://api.kraken.com/0/public/OHLC",
            1,     // 1 request per second for public endpoints
            720,   // max data points
            Duration.ofMinutes(1),
            Set.of("1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w"),
            false
    );


    private final String displayName;
    private final String baseUrl;
    private final String symbolListEndpoint;
    private final String klineEndpoint;
    private final int rateLimitPerMinute;
    private final int maxKlinesPerRequest;
    private final Duration requestTimeout;
    private final Set<String> supportedIntervals;
    private final boolean implemented;


    public static Exchange fromString(String name) {
        for (Exchange exchange : Exchange.values()) {
            if (exchange.name().equalsIgnoreCase(name.trim()) ||
                    exchange.displayName.equalsIgnoreCase(name.trim())) {
                return exchange;
            }
        }
        throw new IllegalArgumentException();
    }

    public static Set<String> getSupportedExchangeNames() {
        return Set.of("BINANCE", "BINANCE_US", "COINBASE", "KRAKEN");
    }

    public boolean supportsInterval(String interval) {
        return supportedIntervals.contains(interval);
    }

    // Overload
    public String getKlineUrl(String symbol, String interval, long startTime, long endTime, int limit) {
        return String.format("%s?symbol=%s&interval=%s&startTime=%d&endTime=%d&limit=%d",
                klineEndpoint, symbol, interval, startTime, endTime, Math.min(limit, maxKlinesPerRequest));
    }

    public String getKlineUrl(String symbol, String interval, long startTime, long endTime) {
        switch (this) {
            case BINANCE:
            case BINANCE_US:
                return String.format("%s?symbol=%s&interval=%s&startTime=%d&endTime=%d&limit=%d",
                        klineEndpoint, symbol, interval, startTime, endTime, maxKlinesPerRequest);
            case COINBASE:
                return String.format("%s?start=%d&end=%d&granularity=%s",
                        klineEndpoint.replace("{symbol}", symbol.toLowerCase()),
                        startTime / 1000, endTime / 1000, mapIntervalToCoinbase(interval));
            case KRAKEN:
                return String.format("%s?pair=%s&interval=%s&since=%d",
                        klineEndpoint, symbol, mapIntervalToKraken(interval), startTime / 1000);
            default:
                throw new UnsupportedOperationException("URL generation not implemented for: " + this);
        }
    }

    private String mapIntervalToCoinbase(String interval) {
        return switch (interval) {
            case "1m" -> "60";
            case "5m" -> "300";
            case "15m" -> "900";
            case "1h" -> "3600";
            case "6h" -> "21600";
            case "1d" -> "86400";
            default -> throw new IllegalArgumentException("Unsupported interval for Coinbase: " + interval);
        };
    }

    private String mapIntervalToKraken(String interval) {
        return switch (interval) {
            case "1m" -> "1";
            case "5m" -> "5";
            case "15m" -> "15";
            case "30m" -> "30";
            case "1h" -> "60";
            case "4h" -> "240";
            case "1d" -> "1440";
            case "1w" -> "10080";
            default -> throw new IllegalArgumentException("Unsupported interval for Kraken: " + interval);
        };
    }


}
