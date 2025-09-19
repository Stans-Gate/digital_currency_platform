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
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.model.Position;
import com.example.digitCurrencyPlatform.model.exception.InputInvalidException;
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
                                                long endTime, Interval targetInterval) {
        System.out.println("=== PORTFOLIO CALCULATION STARTED ===");
        System.out.printf("Total Value: %s, Start: %d, End: %d, Interval: %s%n",
                totalValue, startTime, endTime, targetInterval);

        // 1. Calculate the total number of coins owned at the start time given positions and total value
        Map<String, BigDecimal> coinHoldings = calculateInitialHoldings(positions, totalValue, startTime);

        // 2. All the 1m interval kline data for each symbol in the time range will be fetched
        Map<String, List<Kline>> symbolKlines = fetchSymbolKlines(coinHoldings.keySet(), startTime, endTime);

        // 3. Generate the 1m interval portfolio kline by aggregating kline data across diff symbols
        List<Kline> oneMinutePortfolioKlines = generateOneMinutePortfolioKlines(coinHoldings, symbolKlines, startTime, endTime);

        // 4. Aggregate all 1m interval portfolio kline to the target Interval
        return aggregatePortfolioKlines(oneMinutePortfolioKlines, targetInterval);
    }

    List<Kline> aggregatePortfolioKlines(List<Kline> oneMinutePortfolioKlines, Interval targetInterval) {
        System.out.printf("\n--- AGGREGATING TO %s INTERVALS ---\n", targetInterval);

        if (targetInterval == Interval.ONE_MINUTE) {
            return oneMinutePortfolioKlines;
        }

        long targetIntervalMs = targetInterval.getMilliseconds();
        int timeWindowSize = (int) (targetIntervalMs / Interval.ONE_MINUTE.getMilliseconds());


        if (oneMinutePortfolioKlines.isEmpty()) {
            return new ArrayList<>();
        }

        oneMinutePortfolioKlines.sort(Comparator.comparing(Kline::getOpenTime));
        List<Kline> aggregatePortfolioKlines = new ArrayList<>();

        for (int i = 0; i < oneMinutePortfolioKlines.size(); i += timeWindowSize) {
            int endIndex = Math.min(i + timeWindowSize, oneMinutePortfolioKlines.size());
            List<Kline> windowKlines = oneMinutePortfolioKlines.subList(i, endIndex);

            if (!windowKlines.isEmpty()) {
                Kline aggregatedKline = aggregateKlineWindow(windowKlines, targetIntervalMs);
                aggregatePortfolioKlines.add(aggregatedKline);

                System.out.printf("Aggregated window %d: %d minutes -> O:%s H:%s L:%s C:%s%n",
                        (i / timeWindowSize) + 1, windowKlines.size(),
                        aggregatedKline.getOpenPrice(), aggregatedKline.getHighPrice(),
                        aggregatedKline.getLowPrice(), aggregatedKline.getClosePrice());
            }
        }

        System.out.printf("Final aggregated klines: %d%n", aggregatePortfolioKlines.size());
        return aggregatePortfolioKlines;
    }

    private Kline aggregateKlineWindow(List<Kline> windowKlines, long targetIntervalMs) {
        if (windowKlines.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate empty kline window");
        }

        windowKlines.sort(Comparator.comparing(Kline::getOpenTime));

        String symbol = windowKlines.get(0).getSymbol();
        long windowStart = windowKlines.get(0).getOpenTime();
        long windowEnd = windowStart + targetIntervalMs - 1;

        // Open: first kline's open price
        BigDecimal open = windowKlines.get(0).getOpenPrice();

        // Close: last kline's close price
        BigDecimal close = windowKlines.get(windowKlines.size() - 1).getClosePrice();

        // High: maximum high price across all klines in window
        BigDecimal high = windowKlines.stream()
                .map(Kline::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Low: minimum low price across all klines in window
        BigDecimal low = windowKlines.stream()
                .map(Kline::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Volume: sum of all volumes
        BigDecimal volume = windowKlines.stream()
                .map(Kline::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Number of trades: sum of all trades
        Long numberOfTrades = windowKlines.stream()
                .mapToLong(Kline::getNumberOfTrades)
                .sum();

        return new Kline(symbol, windowStart, windowEnd, open, close, high, low, volume, numberOfTrades);
    }

    private List<Kline> generateOneMinutePortfolioKlines(Map<String, BigDecimal> coinHoldings,
                                                         Map<String, List<Kline>> symbolKlines,
                                                         long starttime,
                                                         long endTime) {
        System.out.println("\n--- GENERATING ONE MINUTE PORTFOLIO KLINES ---");

        Set<Long> allOneMinuteTimestamps = symbolKlines.values().stream().
                flatMap(List::stream).map(Kline::getOpenTime).filter(time -> time >= starttime && time <= endTime)
                .collect(Collectors.toSet());

        List<Long> sortedTimestamps = allOneMinuteTimestamps.stream().sorted().collect(Collectors.toList());

        List<Kline> portfolioKlines = new ArrayList<>();

        for (int i = 0; i < sortedTimestamps.size(); i++) {
            Long timestamp = sortedTimestamps.get(i);
            Kline portfolioKline = calculateMinutePortfolioKline(coinHoldings, symbolKlines, timestamp);
            portfolioKlines.add(portfolioKline);
        }

        System.out.println("Number of portfolio Klines: " + portfolioKlines.size());
        return portfolioKlines;
    }

    private Kline calculateMinutePortfolioKline(Map<String, BigDecimal> coinHoldings,
                                                Map<String, List<Kline>> symbolKlines,
                                                long timestamp) {
        BigDecimal portfolioOpen = BigDecimal.ZERO;
        BigDecimal portfolioHigh = BigDecimal.ZERO;
        BigDecimal portfolioLow = BigDecimal.ZERO;
        BigDecimal portfolioClose = BigDecimal.ZERO;
        BigDecimal portfolioVolume = BigDecimal.ZERO;
        long totalTrades = 0;
        for (Map.Entry<String, BigDecimal> holding : coinHoldings.entrySet()) {
            String symbol = holding.getKey();
            BigDecimal coins = holding.getValue();

            // Find the minute kline for this symbol at this timestamp
            Kline symbolKline = findKlineAtTime(symbolKlines.get(symbol), timestamp);

            if (symbolKline != null) {
                BigDecimal positionOpen = coins.multiply(symbolKline.getOpenPrice());
                BigDecimal positionHigh = coins.multiply(symbolKline.getHighPrice());
                BigDecimal positionLow = coins.multiply(symbolKline.getLowPrice());
                BigDecimal positionClose = coins.multiply(symbolKline.getClosePrice());

                portfolioOpen = portfolioOpen.add(positionOpen);
                portfolioHigh = portfolioHigh.add(positionHigh);
                portfolioLow = portfolioLow.add(positionLow);
                portfolioClose = portfolioClose.add(positionClose);
                portfolioVolume = portfolioVolume.add(symbolKline.getVolume());
                totalTrades += symbolKline.getNumberOfTrades();
            }
        }
        return new Kline("Portfolio", timestamp, timestamp + 60000 - 1, portfolioOpen, portfolioClose, portfolioHigh, portfolioLow, portfolioVolume, totalTrades);
    }

    // GOOD HELPER
    private Kline findKlineAtTime(List<Kline> klines, Long timePoint) {
        return klines.stream()
                .filter(kline -> kline.getOpenTime().equals(timePoint))
                .findFirst()
                .orElse(null);
    }

    // GOOD
    private Map<String, List<Kline>> fetchSymbolKlines(Set<String> symbols, long startTime, long endTime) {
        System.out.println("\n--- FETCHING MINUTE-LEVEL DATA FOR EACH SYMBOL ---");

        Map<String, List<Kline>> symbolKlines = new HashMap<>();
        for (String symbol : symbols) {
            List<Kline> klines = klineRepository.retrieveKlineDataWithStartAndEndTime(symbol, startTime, endTime, Integer.MAX_VALUE);
            if (klines.isEmpty()) {
                throw new InputInvalidException("No minute data found for symbol: " + symbol);
            }
            symbolKlines.put(symbol, klines);
            System.out.printf("Symbol %s: %d minute klines loaded%n", symbol, klines.size());
        }

        return symbolKlines;
    }

    // GOOD
    private Map<String, BigDecimal> calculateInitialHoldings(List<Position> positions, BigDecimal totalValue, long startTime) {
        System.out.println("\n--- CALCULATING INITIAL HOLDINGS ---");
        Map<String, BigDecimal> coinHoldings = new HashMap<>();
        for (Position position : positions) {
            BigDecimal investmentAmount = totalValue.multiply(position.getWeight());

            // Get price of coin at start time
            BigDecimal priceAtStart = getPriceAtTime(position.getSymbol(), startTime);

            // Calculate total coins owned = investmentAmount / priceAtStart
            BigDecimal coins = investmentAmount.divide(priceAtStart, 8, RoundingMode.HALF_UP);
            coinHoldings.put(position.getSymbol(), coins);

            // print statement for debugging
            System.out.printf("Position %s: Weight=%.2f%%, Amount=%s, Price=%s, Quantity=%s%n",
                    position.getSymbol(), position.getWeight().multiply(BigDecimal.valueOf(100)),
                    investmentAmount, priceAtStart, coins);
        }
        return coinHoldings;
    }

    // GOOD
    private BigDecimal getPriceAtTime(String symbol, long startTime) {
        List<Kline> klines = klineRepository.retrieveKlineDataWithStartAndEndTime(
                symbol, startTime, startTime + 60000, 1);
        if (klines.isEmpty()) {
            klines = klineRepository.retrieveKlineDataWithStartAndEndTime(
                    symbol, startTime - 5 * 60000, startTime + 5 * 60000, 1);
        }
        if (klines.isEmpty()) {
            throw new InputInvalidException("No price data found for symbol: " + symbol + " at time: " + startTime);
        }

        return klines.get(0).getClosePrice(); // Open or Close Price?
    }

}
