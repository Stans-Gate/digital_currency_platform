package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.model.Position;
import com.example.digitCurrencyPlatform.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/test")
    public ResponseEntity<List<Kline>> testPortfolio() {
        List<Position> positions = new ArrayList<>();
        positions.add(new Position("BTCUSDT", new BigDecimal("0.5")));
        positions.add(new Position("ETHUSDT", new BigDecimal("0.5")));
        List<Kline> portfolioKlines = portfolioService.calculatePortfolioKlines(positions, new BigDecimal(100000000), 1752979919000L, 1753066319000L, Interval.FIVE_MINUTES);
        return ResponseEntity.ok(portfolioKlines);
    }

}
