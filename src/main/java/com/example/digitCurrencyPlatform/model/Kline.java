package com.example.digitCurrencyPlatform.model;

import jakarta.validation.constraints.*;
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
    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotNull(message = "Open time cannot be null")
    @Min(value = 0, message = "Open time must be non-negative")
    private Long openTime;

    @NotNull(message = "Close time cannot be null")
    @Min(value = 0, message = "Close time must be non-negative")
    private Long closeTime;

    @NotNull(message = "Open price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Open price must be greater than 0")
    private BigDecimal openPrice;

    @NotNull(message = "Close price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Close price must be greater than 0")
    private BigDecimal closePrice;

    @NotNull(message = "High price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "High price must be greater than 0")
    private BigDecimal highPrice;

    @NotNull(message = "Low price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Low price must be greater than 0")
    private BigDecimal lowPrice;

    @NotNull(message = "Volume cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Volume must be non-negative")
    private BigDecimal volume;

    @NotNull(message = "Number of trades cannot be null")
    @Min(value = 0, message = "Number of trades must be non-negative")
    private Long numberOfTrades;

    @AssertTrue(message = "Close time must be after open time")
    public boolean isValidTimeRange() {
        if (openTime == null || closeTime == null) {
            return true; // Let @NotNull handle null cases
        }
        return closeTime > openTime;
    }

    @AssertTrue(message = "Price relationships must be valid (high >= all prices, low <= all prices)")
    public boolean isValidPriceRange() {
        if (openPrice == null || closePrice == null || highPrice == null || lowPrice == null) {
            return true;
        }

        return highPrice.compareTo(openPrice) >= 0 &&
                highPrice.compareTo(closePrice) >= 0 &&
                highPrice.compareTo(lowPrice) >= 0 &&
                lowPrice.compareTo(openPrice) <= 0 &&
                lowPrice.compareTo(closePrice) <= 0;
    }
}
