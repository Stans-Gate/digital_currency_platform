package com.example.digitCurrencyPlatform.model;

public class InputInvalidException extends RuntimeException {
    public InputInvalidException(String message) {
        super(message);
    }
}
