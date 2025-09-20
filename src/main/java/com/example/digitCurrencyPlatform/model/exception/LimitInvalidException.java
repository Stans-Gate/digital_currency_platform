package com.example.digitCurrencyPlatform.model.exception;

public class LimitInvalidException extends RuntimeException {
    private final Integer limit;
    private final Integer maxAllowed;

    public LimitInvalidException(String message, Integer limit, Integer maxAllowed) {
        super(message);
        this.limit = limit;
        this.maxAllowed = maxAllowed;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getMaxAllowed() {
        return maxAllowed;
    }
}
