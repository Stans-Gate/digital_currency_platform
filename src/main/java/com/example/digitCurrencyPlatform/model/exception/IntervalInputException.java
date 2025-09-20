package com.example.digitCurrencyPlatform.model.exception;

public class IntervalInputException extends RuntimeException {
    private final String interval;
    private final String exchange;

    public IntervalInputException(String message, String interval) {
        super(message);
        this.interval = interval;
        this.exchange = null;
    }

    public IntervalInputException(String message, String interval, String exchange) {
        super(message);
        this.interval = interval;
        this.exchange = exchange;
    }

    public String getInterval() {
        return interval;
    }

    public String getExchange() {
        return exchange;
    }
}
