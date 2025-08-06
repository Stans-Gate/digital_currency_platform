package com.example.digitCurrencyPlatform.service.provider;

import com.example.digitCurrencyPlatform.enums.BinanceInterval;
import com.example.digitCurrencyPlatform.model.Kline;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


// retrieve kline data from Binance
@Service
public class BinanceServiceProvider implements KlineDataProvider {
    private static final String BINANCE_V3_KLINE_API_URL = "https://www.binance.com/api/v3/klines";
    private static final int MAX_LIMIT_PER_REQUEST = 500;
    private final RestTemplate restTemplate;

    public BinanceServiceProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<Kline> fetchKlines(String symbol, BinanceInterval interval, long startTime, long endTime) {
        String requestUrl = String.format(
                "%s?symbol=%s&interval=%s&startTime=%d&endTime=%d",
                BINANCE_V3_KLINE_API_URL, symbol, interval.getValue(), startTime, endTime
        );

        ResponseEntity<List> response = restTemplate.getForEntity(requestUrl, List.class);
        List<List<Object>> rawKlines = response.getBody();
        System.out.println("Number of kline data retrieved: " + rawKlines.size());

        if (rawKlines == null || rawKlines.isEmpty()) {
            System.out.println("No data returned from Binance API.");
            return null;
        }

        List<Kline> klines = new ArrayList<>();
        for (List<Object> item : rawKlines) {
            try {
                Kline kline = new Kline(
                        symbol,
                        Long.parseLong(item.get(0).toString()),
                        Long.parseLong(item.get(6).toString()),
                        new BigDecimal(item.get(1).toString()),
                        new BigDecimal(item.get(4).toString()),
                        new BigDecimal(item.get(2).toString()),
                        new BigDecimal(item.get(3).toString()),
                        new BigDecimal(item.get(5).toString()),
                        Long.parseLong(item.get(8).toString())
                );
                // Insert into DB
                klines.add(kline); // batch insert optimization
            } catch (Exception e) {
                System.err.println("Failed to parse or insert kline: " + item);
                e.printStackTrace();
            }
        }

        return klines;
    }

    @Override
    public String getProviderName() {
        return "BINANCE";
    }

    @Override
    public int getMaxLimitPerRequest() {
        return MAX_LIMIT_PER_REQUEST;
    }
}

