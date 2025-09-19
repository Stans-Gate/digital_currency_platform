package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.enums.Exchange;
import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.model.exception.InputInvalidException;
import com.example.digitCurrencyPlatform.service.InputValidationService;
import com.example.digitCurrencyPlatform.service.KlineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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
    public ResponseEntity fetchBinanceKlines(
            @PathVariable String exchange,
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long startTime,
            @RequestParam long endTime) {

        if (symbol == null || symbol == "") {
            throw new InputInvalidException("symbol is empty"); // who to handle -> KlineControllerExceptionHandler
        }

        Exchange exchangeEnum;
        try {
            exchangeEnum = Exchange.fromString(exchange);
        } catch (IllegalArgumentException e) {
            throw new InputInvalidException("Invalid exchange: " + exchange);
        }

        Interval intervalEnum;
        try {
            intervalEnum = Interval.fromString(interval);
        } catch (InputInvalidException e) {
            throw new InputInvalidException("Invalid interval: " + interval);
        }

        if (!exchangeEnum.supportsInterval(interval)) {
            throw new InputInvalidException("Interval " + interval +
                    " not supported by " + exchangeEnum.getDisplayName());
        }

        klineService.fetchAndSaveKlines(exchange, symbol, intervalEnum, startTime, endTime, 500);
        return ResponseEntity.status(HttpStatus.CREATED).body("Kline data fetch initiated for " + exchangeEnum.getDisplayName());
    }


    @GetMapping("/retrieve")
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

        return ResponseEntity.ok(inputValidationService.validateBinanceSymbol());

    }
}
