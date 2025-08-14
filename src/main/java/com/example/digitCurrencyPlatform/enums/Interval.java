package com.example.digitCurrencyPlatform.enums;

import com.example.digitCurrencyPlatform.model.InputInvalidException;
import lombok.Getter;

@Getter
public enum Interval {
    // Seconds
    ONE_SECOND("1s", 1000L),

    // Minutes
    ONE_MINUTE("1m", 60 * 1000L),
    THREE_MINUTES("3m", 3 * 60 * 1000L),
    FIVE_MINUTES("5m", 5 * 60 * 1000L),
    FIFTEEN_MINUTES("15m", 15 * 60 * 1000L),
    THIRTY_MINUTES("30m", 30 * 60 * 1000L),

    // Hours
    ONE_HOUR("1h", 60 * 60 * 1000L),
    TWO_HOURS("2h", 2 * 60 * 60 * 1000L),
    FOUR_HOURS("4h", 4 * 60 * 60 * 1000L),
    SIX_HOURS("6h", 6 * 60 * 60 * 1000L),
    EIGHT_HOURS("8h", 8 * 60 * 60 * 1000L),
    TWELVE_HOURS("12h", 12 * 60 * 60 * 1000L),

    // Days
    ONE_DAY("1d", 24 * 60 * 60 * 1000L),
    THREE_DAYS("3d", 3 * 24 * 60 * 60 * 1000L),

    // Week
    ONE_WEEK("1w", 7 * 24 * 60 * 60 * 1000L),

    // Month (approximate - 30 days)
    ONE_MONTH("1M", 30 * 24 * 60 * 60 * 1000L);

    private final String value;
    private final long milliseconds;

    Interval(String value, long milliseconds) {
        this.value = value;
        this.milliseconds = milliseconds;
    }

    public static Interval fromString(String value) {
        for (Interval interval : Interval.values()) {
            if (interval.value.equals(value)) {
                return interval;
            }
        }
        throw new InputInvalidException("NOT RIGHT INTERVAL");
    }

    @Override
    public String toString() {
        return value;
    }

}
