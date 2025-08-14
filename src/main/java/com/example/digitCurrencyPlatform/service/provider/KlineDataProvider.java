package com.example.digitCurrencyPlatform.service.provider;

import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface KlineDataProvider {

    @NotEmpty
    List<@NotNull @Valid Kline> fetchKlines(
            @NotBlank String symbol,
            @NotNull Interval interval,
            @Min(0) Long startTime,
            Long endTime);

    String getProviderName();

    int getMaxLimitPerRequest();

    // getValidSymbol -> binance api
}
