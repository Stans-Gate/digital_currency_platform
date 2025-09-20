package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.enums.Exchange;
import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.service.InputValidationService;
import com.example.digitCurrencyPlatform.service.KlineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kline")
public class KlineController {
    private final KlineService klineService;
    private final InputValidationService inputValidationService;

    public KlineController(KlineService klineService, InputValidationService inputValidationService) {
        this.klineService = klineService;
        this.inputValidationService = inputValidationService;
    }

    @PostMapping("/fetch/{exchange}")
    public ResponseEntity<String> fetchKlines(
            @PathVariable String exchange,
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long startTime,
            @RequestParam long endTime) {

        // Comprehensive validation using InputValidationService
        inputValidationService.validateFetchRequest(exchange, symbol, interval, startTime, endTime);

        // Get validated enums
        Exchange exchangeEnum = inputValidationService.validateExchange(exchange);
        Interval intervalEnum = inputValidationService.validateInterval(interval);

        klineService.fetchAndSaveKlines(exchange, symbol, intervalEnum, startTime, endTime, 500);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Kline data fetch initiated for " + exchangeEnum.getDisplayName());
    }


    @GetMapping("/retrieve")
    public ResponseEntity<List<Kline>> retrieveAggregatedKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam String baseInterval,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(defaultValue = "100") int limit) {

        // Comprehensive validation using InputValidationService
        inputValidationService.validateRetrieveRequest(symbol, interval, baseInterval, startTime, endTime, limit);

        // Get validated enums
        Interval intervalEnum = inputValidationService.validateInterval(interval);
        Interval baseIntervalEnum = inputValidationService.validateInterval(baseInterval);

        List<Kline> klines = klineService.retrieveKlinesWithDifferentIntervals(
                symbol, intervalEnum, startTime, endTime, limit, baseIntervalEnum);

        return ResponseEntity.ok(klines);
    }


    @GetMapping("/symbols")
    public ResponseEntity<Map<String, Object>> fetchSymbols() {
        Map<String, Object> response = Map.of(
                "message", "Use /symbols/{exchange} for specific exchange symbols or /symbols/all for all exchanges",
                "availableExchanges", List.of("BINANCE_US")
        );
        return ResponseEntity.ok(response);
    }
}
