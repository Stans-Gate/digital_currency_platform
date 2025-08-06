package com.example.digitCurrencyPlatform.service.provider;

import com.example.digitCurrencyPlatform.enums.BinanceInterval;
import com.example.digitCurrencyPlatform.model.Kline;

import java.util.List;

public interface KlineDataProvider {

    List<Kline> fetchKlines(String symbol, BinanceInterval interval, long startTime, long endTime);

    String getProviderName();

    int getMaxLimitPerRequest();
}
