package com.example.digitCurrencyPlatform.model.exception;

public class TimeRangeInvalidException extends RuntimeException {
    private final Long startTime;
    private final Long endTime;

    public TimeRangeInvalidException(String message, Long startTime, Long endTime) {
        super(message);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }
}
