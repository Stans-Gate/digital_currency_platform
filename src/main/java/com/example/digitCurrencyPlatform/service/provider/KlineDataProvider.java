package com.example.digitCurrencyPlatform.service.provider;

import com.example.digitCurrencyPlatform.enums.Exchange;
import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public interface KlineDataProvider {

    @NotEmpty
    List<@NotNull @Valid Kline> fetchKlines(
            @NotBlank String symbol,
            @NotNull Interval interval,
            @Min(0) @NotNull Long startTime,
            @NotNull Long endTime);

    Exchange getExchange();

    default String getProviderName() {
        return getExchange().name();
    }

    default int getMaxLimitPerRequest() {
        return getExchange().getMaxKlinesPerRequest();
    }

    default Set<String> getSupportedIntervals() {
        return getExchange().getSupportedIntervals();
    }

    default boolean supportsInterval(Interval interval) {
        return getExchange().supportsInterval(interval.getValue());
    }

    // Delegate the responsibility of fetching available symbols to each exchange
    Set<String> fetchAvailableSymbols();
}
