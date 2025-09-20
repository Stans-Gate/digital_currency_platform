package com.example.digitCurrencyPlatform.model.exception;

public class ExchangeInvalidException extends RuntimeException {

    private final String exchange;

    public ExchangeInvalidException(String message, String exchange) {
        super(message);
        this.exchange = exchange;
    }

    public String getExchange() {
        return exchange;
    }
}
