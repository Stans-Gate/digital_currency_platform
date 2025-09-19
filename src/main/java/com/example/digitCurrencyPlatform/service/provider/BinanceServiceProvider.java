package com.example.digitCurrencyPlatform.service.provider;

import com.example.digitCurrencyPlatform.enums.Exchange;
import com.example.digitCurrencyPlatform.enums.Interval;
import com.example.digitCurrencyPlatform.model.Kline;
import com.example.digitCurrencyPlatform.model.exception.DataProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


// retrieve kline data from Binance
@Validated
@Service
public class BinanceServiceProvider implements KlineDataProvider {

    private final Exchange exchange;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public BinanceServiceProvider(RestTemplate restTemplate, @Value("${app.exchange.default}") String exchangeName) {
        this.restTemplate = restTemplate;
        this.exchange = Exchange.fromString(exchangeName);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    @Retryable(value = {DataProviderException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public List<Kline> fetchKlines(String symbol, Interval interval, Long startTime, Long endTime) {
        if (!supportsInterval(interval)) {
            throw new DataProviderException("Interval " + interval + " not supported by " + exchange.getDisplayName());
        }
        try {
            String requestUrl = exchange.getKlineUrl(symbol, interval.getValue(), startTime, endTime, exchange.getMaxKlinesPerRequest());

            ResponseEntity<List> response = restTemplate.getForEntity(requestUrl, List.class);
            List<List<Object>> rawKlines = response.getBody();

            System.out.println("Number of kline data retrieved from " + exchange.getDisplayName() + ": " +
                    (rawKlines != null ? rawKlines.size() : 0));

            if (rawKlines == null || rawKlines.isEmpty()) {
                System.out.println("No data returned from " + exchange.getDisplayName() + " API.");
                return new ArrayList<>();
            }

            return parseKlineData(rawKlines, symbol);

        } catch (Exception e) {
            throw new DataProviderException("Failed to fetch klines from " + exchange.getDisplayName() +
                    " for symbol: " + symbol);
        }
    }

    private List<Kline> parseKlineData(List<List<Object>> rawKlines, String symbol) {
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
                klines.add(kline);

            } catch (Exception e) {
                System.err.println("Failed to parse kline from " + exchange.getDisplayName() + ": " + item);
            }
        }

        return klines;
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Set<String> fetchAvailableSymbols() {
        try {
            String json = restTemplate.getForObject(exchange.getSymbolListEndpoint(), String.class);

            if (json == null || json.trim().isEmpty()) {
                throw new DataProviderException("Empty response from " + exchange.getDisplayName() + " symbol API");
            }

            JsonNode root = objectMapper.readTree(json);
            Set<String> symbols = new LinkedHashSet<>();
            for (JsonNode symbolNode : root.get("symbols")) {
                if ("TRADING".equals(symbolNode.get("status").asText())) {
                    symbols.add(symbolNode.get("symbol").asText());
                }
            }

            if (symbols.isEmpty()) {
                throw new DataProviderException("No trading symbols found from " + exchange.getDisplayName() + " API");
            }

            return symbols;

        } catch (JsonProcessingException e) {
            throw new DataProviderException("Failed to parse " + exchange.getDisplayName() + " symbol response");
        } catch (Exception e) {
            throw new DataProviderException("Failed to fetch symbols from " + exchange.getDisplayName());
        }
    }

    @Override
    public String getProviderName() {
        return "BINANCE_US";
    }

    @Override
    public int getMaxLimitPerRequest() {
        return exchange.getMaxKlinesPerRequest();
    }
}

