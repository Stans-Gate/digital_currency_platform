package com.example.digitCurrencyPlatform.controller;


import com.example.digitCurrencyPlatform.model.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({InputInvalidException.class})
    public ResponseEntity<String> handleException(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SymbolInvalidException.class)
    public ResponseEntity<ErrorResponse> handleSymbolInvalidException(SymbolInvalidException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("invalidSymbol", e.getSymbol());

        ErrorResponse errorResponse = new ErrorResponse(
                "SYMBOL_INVALID",
                e.getMessage(),
                LocalDateTime.now(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExchangeInvalidException.class)
    public ResponseEntity<ErrorResponse> handleExchangeInvalidException(ExchangeInvalidException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("invalidExchange", e.getExchange());
        details.put("supportedExchanges", "BINANCE, BINANCE_US, COINBASE, KRAKEN");

        ErrorResponse errorResponse = new ErrorResponse(
                "EXCHANGE_INVALID",
                e.getMessage(),
                LocalDateTime.now(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IntervalInputException.class)
    public ResponseEntity<ErrorResponse> handleIntervalInputException(IntervalInputException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("invalidInterval", e.getInterval());
        if (e.getExchange() != null) {
            details.put("exchange", e.getExchange());
        }
        details.put("supportedIntervals", "1s, 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M");

        ErrorResponse errorResponse = new ErrorResponse(
                "INTERVAL_INVALID",
                e.getMessage(),
                LocalDateTime.now(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TimeRangeInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTimeRangeInvalidException(TimeRangeInvalidException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("startTime", e.getStartTime());
        details.put("endTime", e.getEndTime());

        if (e.getStartTime() != null && e.getEndTime() != null) {
            long durationDays = (e.getEndTime() - e.getStartTime()) / (24 * 60 * 60 * 1000);
            details.put("requestedDurationDays", durationDays);
            details.put("maxAllowedDurationDays", 365);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                "TIME_RANGE_INVALID",
                e.getMessage(),
                LocalDateTime.now(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LimitInvalidException.class)
    public ResponseEntity<ErrorResponse> handleLimitInvalidException(LimitInvalidException e) {
        Map<String, Object> details = new HashMap<>();
        details.put("requestedLimit", e.getLimit());
        details.put("maxAllowedLimit", e.getMaxAllowed());

        ErrorResponse errorResponse = new ErrorResponse(
                "LIMIT_INVALID",
                e.getMessage(),
                LocalDateTime.now(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataProviderException.class)
    public ResponseEntity<ErrorResponse> handleDataProviderException(DataProviderException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                "DATA_PROVIDER_ERROR",
                e.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }


    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, Object> details;

        public ErrorResponse(String errorCode, String message, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
        }

        public ErrorResponse(String errorCode, String message, LocalDateTime timestamp, Map<String, Object> details) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
            this.details = details;
        }

        // Getters and setters
        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }

}
