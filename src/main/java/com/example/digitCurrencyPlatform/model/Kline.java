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
    @NotNull
    private Long closeTime;
    @NotNull
    private BigDecimal openPrice;
    @NotNull
    private BigDecimal closePrice;
    @NotNull
    private BigDecimal highPrice;
    @NotNull
    private BigDecimal lowPrice;
    @NotNull
    private BigDecimal volume;
    @NotNull
    private Long numberOfTrades;
}
