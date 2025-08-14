package com.example.digitCurrencyPlatform.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Validated
public class Kline {
    @NotBlank
    private String symbol;
    @NotNull
    private Long openTime;
    private Long closeTime;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private Long numberOfTrades;

//    // Constructor
//    public Kline(String symbol, long openTime, long closeTime, BigDecimal openPrice,
//                 BigDecimal closePrice, BigDecimal highPrice, BigDecimal lowPrice,
//                 BigDecimal volume, long numberOfTrades) {
//        this.symbol = symbol;
//        this.openTime = openTime;
//        this.closeTime = closeTime;
//        this.openPrice = openPrice;
//        this.closePrice = closePrice;
//        this.highPrice = highPrice;
//        this.lowPrice = lowPrice;
//        this.volume = volume;
//        this.numberOfTrades = numberOfTrades;
//    }
//
//    public Kline() {}
//
//    // Getters and Setters
//    public String getSymbol() {
//        return symbol;
//    }
//    public void setSymbol(String symbol) {
//        this.symbol = symbol;
//    }
//
//    public long getOpenTime() {
//        return openTime;
//    }
//    public void setOpenTime(long openTime) {
//        this.openTime = openTime;
//    }
//
//    public long getCloseTime() {
//        return closeTime;
//    }
//    public void setCloseTime(long closeTime) {
//        this.closeTime = closeTime;
//    }
//
//    public BigDecimal getOpenPrice() {
//        return openPrice;
//    }
//    public void setOpenPrice(BigDecimal openPrice) {
//        this.openPrice = openPrice;
//    }
//
//    public BigDecimal getClosePrice() {
//        return closePrice;
//    }
//    public void setClosePrice(BigDecimal closePrice) {
//        this.closePrice = closePrice;
//    }
//
//    public BigDecimal getHighPrice() {
//        return highPrice;
//    }
//    public void setHighPrice(BigDecimal highPrice) {
//        this.highPrice = highPrice;
//    }
//
//    public BigDecimal getLowPrice() {
//        return lowPrice;
//    }
//    public void setLowPrice(BigDecimal lowPrice) {
//        this.lowPrice = lowPrice;
//    }
//
//    public BigDecimal getVolume() {
//        return volume;
//    }
//    public void setVolume(BigDecimal volume) {
//        this.volume = volume;
//    }
//
//    public long getNumberOfTrades() {
//        return numberOfTrades;
//    }
//    public void setNumberOfTrades(long numberOfTrades) {
//        this.numberOfTrades = numberOfTrades;
//    }


}
