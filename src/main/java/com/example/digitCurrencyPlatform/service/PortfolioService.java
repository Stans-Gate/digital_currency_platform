package com.example.digitCurrencyPlatform.service;
// User can give you a list of position (weight + symbol)
// total value (1 Million)
// 0.5 BTC, 0.5 ETH -> 500,000 in BTC, 500,000 in ETH
// Given start time and end time (Time Range)
// Assume: invest total value * weight onto the symbol in the start time -> how many coins hold for this symbol
// Symbol has a kline everyday, kline times num coins -> fluctuation of the total value
// Specify interval for the kline calculation
// return List<Kline> for the entire time range that reflects the fluctuation of the total value

import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.InputInvalidException;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.model.Position;
import com.example.digitCurrencyPlatform.repository.KlineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortfolioService {

    private final KlineRepository klineRepository;

    @Autowired
    public PortfolioService(KlineRepository klineRepository) {
        this.klineRepository = klineRepository;
    }

    // Return a list of kline reflecting, where each kline data represents the
    // total fluctuation of all the coins listed in the positions from the startTime to
    // endTime, where each kline is aggregated based on the interval
    public List<Kline> calculatePortfolioKlines(List<Position> positions,
                                                BigDecimal totalValue,
                                                long startTime,
                                                long endTime, Interval interval) {
        // 1. Calculate the total number of coins owned at the start time given positions and total value
        Map<String, BigDecimal> coinHoldings = calculateInitialHoldings(positions, totalValue, startTime);

        Map<String, List<Kline>> symbolKlines = fetchSymbolKlines(coinHoldings.keySet(), startTime, endTime);

        List<Kline> portfolioKlines = generatePortfolios(coinHoldings, symbolKlines, startTime, endTime, interval);

        return portfolioKlines;
    }

    private List<Kline> generatePortfolios(Map<String, BigDecimal> coinHoldings, Map<String, List<Kline>> symbolKlines, long startTime, long endTime, Interval interval) {

        Set<Long> allTimePoints = symbolKlines.values().stream()
                .flatMap(List::stream)
                .map(Kline::getOpenTime)
                .collect(Collectors.toSet());

        List<Long> sortedTimePoints = allTimePoints.stream()
                .filter(time -> time >= startTime && time <= endTime)
                .sorted()
                .collect(Collectors.toList());

        List<Kline> portfolioKlines = new ArrayList<>();

        for (Long timePoint : sortedTimePoints) {
            Kline portfolioKline = calculatePortfolioKlineAtTime(coinHoldings, symbolKlines, timePoint, interval);
            if (portfolioKline != null) {
                portfolioKlines.add(portfolioKline);
            }
        }

        return portfolioKlines;
    }

    /**
     * Calculate portfolio kline at a specific time point
     */
    private Kline calculatePortfolioKlineAtTime(Map<String, BigDecimal> coinHoldings,
                                                Map<String, List<Kline>> symbolKlines,
                                                Long timePoint,
                                                Interval interval) {

        BigDecimal portfolioOpen = BigDecimal.ZERO;
        BigDecimal portfolioHigh = BigDecimal.ZERO;
        BigDecimal portfolioLow = BigDecimal.ZERO;
        BigDecimal portfolioClose = BigDecimal.ZERO;
        BigDecimal portfolioVolume = BigDecimal.ZERO;
        long totalTrades = 0;

        boolean hasValidData = false;

        for (Map.Entry<String, BigDecimal> holding : coinHoldings.entrySet()) {
            String symbol = holding.getKey();
            BigDecimal coins = holding.getValue();

            // Find kline for this symbol at this time point
            Kline symbolKline = findKlineAtTime(symbolKlines.get(symbol), timePoint);

            if (symbolKline != null) {
                // Calculate position values
                BigDecimal positionOpen = coins.multiply(symbolKline.getOpenPrice());
                BigDecimal positionHigh = coins.multiply(symbolKline.getHighPrice());
                BigDecimal positionLow = coins.multiply(symbolKline.getLowPrice());
                BigDecimal positionClose = coins.multiply(symbolKline.getClosePrice());

                // Add to portfolio totals
                portfolioOpen = portfolioOpen.add(positionOpen);
                portfolioHigh = portfolioHigh.add(positionHigh);
                portfolioLow = portfolioLow.add(positionLow);
                portfolioClose = portfolioClose.add(positionClose);
                portfolioVolume = portfolioVolume.add(symbolKline.getVolume());
                totalTrades += symbolKline.getNumberOfTrades();

                hasValidData = true;
            }
        }

        if (!hasValidData) {
            return null; // Skip this time point if no valid data
        }

        return new Kline(
                "PORTFOLIO", // Special symbol for portfolio
                timePoint,
                timePoint + interval.getMilliseconds() - 1,
                portfolioOpen,
                portfolioClose,
                portfolioHigh,
                portfolioLow,
                portfolioVolume,
                totalTrades
        );
    }

    /**
     * Find kline data at a specific time point
     */
    private Kline findKlineAtTime(List<Kline> klines, Long timePoint) {
        return klines.stream()
                .filter(kline -> kline.getOpenTime().equals(timePoint))
                .findFirst()
                .orElse(null);
    }


    private Map<String, List<Kline>> fetchSymbolKlines(Set<String> symbols, long startTime, long endTime) {
        Map<String, List<Kline>> symbolKlines = new HashMap<>();
        for (String symbol : symbols) {
            List<Kline> klines = klineRepository.retrieveKlineDataWithStartAndEndTime(symbol, startTime, endTime, Integer.MAX_VALUE);
            if (!klines.isEmpty()) {
                throw new InputInvalidException("Klines fetching from symbol error");
            }
            symbolKlines.put(symbol, klines);
        }

        return symbolKlines;
    }


    private Map<String, BigDecimal> calculateInitialHoldings(List<Position> positions, BigDecimal totalValue, long startTime) {
        Map<String, BigDecimal> coinHoldings = new HashMap<>();
        for (Position position : positions) {
            BigDecimal investmentAmount = totalValue.multiply(position.getWeight());

            // Get price of coin at start time
            BigDecimal priceAtStart = getPriceAtTime(position.getSymbol(), startTime);

            // Calculate total coins owned = investmentAmount / priceAtStart
            BigDecimal coins = investmentAmount.divide(priceAtStart, 8, RoundingMode.HALF_UP);
            coinHoldings.put(position.getSymbol(), coins);
        }
        return coinHoldings;
    }

    private BigDecimal getPriceAtTime(String symbol, long startTime) {
        List<Kline> klines = klineRepository.retrieveKlineDataWithStartAndEndTime(
                symbol, startTime - 60000, startTime + 60000, 1);
        if (klines.isEmpty()) {
            klines = klineRepository.retrieveKlineDataWithStartAndEndTime(
                    symbol, startTime - 3600000, startTime + 3600000, 1);
        }
        if (klines.isEmpty()) {
            throw new InputInvalidException("No price data found for symbol: " + symbol + " at time: " + startTime);
        }

        return klines.get(0).getClosePrice();
    }

}
