package com.tradelog.trade.dto;

import java.time.LocalDate;

public record ParseTradeResponse(
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
) {}
