package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.enums.BinanceInterval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.service.KlineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kline")
public class KlineController {
    private final KlineService klineService;

    public KlineController(KlineService klineService) {
        this.klineService = klineService;
    }

    @PostMapping("/fetch/binance")
    public ResponseEntity<String> fetchBinanceKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        try {
            BinanceInterval intervalEnum = BinanceInterval.fromString(interval);
            klineService.fetchAndSaveKlines("BINANCE", symbol, intervalEnum, startTime, endTime, 500);
            return ResponseEntity.ok("Kline data fetched and inserted.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/retrieve")
    public ResponseEntity<List<Kline>> retrieveKlines(
            @RequestParam String symbol,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<Kline> klines = klineService.getKlines(symbol, startTime, endTime, limit);
            return ResponseEntity.ok(klines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    @GetMapping("/retrieve-by-interval")
    public ResponseEntity<List<Kline>> retrieveKlinesByInterval(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long startTime,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            BinanceInterval intervalEnum = BinanceInterval.fromString(interval);
            List<Kline> klines = klineService.getKlinesByStartTimeAndInterval(symbol, intervalEnum, startTime, limit);
            return ResponseEntity.ok(klines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
