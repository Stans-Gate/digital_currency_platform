package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.InputInvalidException;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.service.InputValidationService;
import com.example.digitCurrencyPlatform.service.KlineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/kline")
public class KlineController {
    private final KlineService klineService;
    private final InputValidationService inputValidationService;

    public KlineController(KlineService klineService) {
        this.klineService = klineService;
        this.inputValidationService = new InputValidationService();
    }

    @PostMapping("/fetch/binance")
    public ResponseEntity<String> fetchBinanceKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        if (symbol == null || symbol == "") {
            throw new InputInvalidException("symbol is empty"); // who to handle -> KlineControllerExceptionHandler
        }
        Interval intervalEnum = Interval.fromString(interval);
        klineService.fetchAndSaveKlines("BINANCE", symbol, intervalEnum, startTime, endTime, 500);
        return ResponseEntity.ok("Kline data fetched and inserted.");

    }

    @GetMapping("/retrieve-aggregated")
    public ResponseEntity<List<Kline>> retrieveAggregatedKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam String baseInterval,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(defaultValue = "100") int limit) {

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new InputInvalidException("symbol is empty");
        }

        Interval intervalEnum = Interval.fromString(interval);
        Interval baseIntervalEnum = Interval.fromString(baseInterval);

        List<Kline> klines = klineService.retrieveKlinesWithDifferentIntervals(symbol, intervalEnum, startTime, endTime, limit, baseIntervalEnum);
        return ResponseEntity.ok(klines);
    }

    @GetMapping("/symbol")
    public ResponseEntity<Set<String>> fetchSymbols() {
        try {
            return ResponseEntity.ok(inputValidationService.validateBinanceSymbol());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

    }
}
