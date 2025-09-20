package com.example.digitCurrencyPlatform.model.exception;

public class SymbolInvalidException extends RuntimeException {
    private final String symbol;

    public SymbolInvalidException(String message, String symbol) {
        super(message);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
