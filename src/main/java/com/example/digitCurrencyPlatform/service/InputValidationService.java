package com.example.digitCurrencyPlatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

public class InputValidationService {
    // Symbol not empty nor null, in the
    // Interval not empty nor null
    // starttime, endtime

    final String BINANCE_SYMBOL_LIST_URL = "https://api.binance.com/api/v3/exchangeInfo?permissions=SPOT";

    public Set<String> validateBinanceSymbol() throws Exception {
        RestTemplate rest = new RestTemplate();
        String json = rest.getForObject(BINANCE_SYMBOL_LIST_URL, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        Set<String> symbols = new LinkedHashSet<>(); // dedupe automatically
        for (JsonNode s : root.get("symbols")) {
            // Keep only symbols currently tradeable on spot
            if ("TRADING".equals(s.get("status").asText())) {
                symbols.add(s.get("symbol").asText());
            }
        }
        return symbols;
    }

}
