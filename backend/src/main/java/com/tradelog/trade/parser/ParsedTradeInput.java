package com.tradelog.trade.parser;

import java.time.LocalDate;

public record ParsedTradeInput(
        String action,
        int qty,
        String ticker,
        String instrumentType,
        String optionType,
        Double strike,
        LocalDate expiry,
        double price,
        double cashFlow,
        String strategy,
        boolean valid,
        String error
) {
    public static ParsedTradeInput invalid(String error) {
        return new ParsedTradeInput(null, 0, null, null, null, null, null, 0, 0, null, false, error);
    }
}
