package com.tradelog.trade.dto;

import java.time.LocalDate;

public record TradeLegResponse(
        Long id,
        Long tradeEntryId,
        Long campaignId,
        String instrumentType,
        String action,
        String ticker,
        int quantity,
        double price,
        double netCashFlow,
        String optionType,
        Double strike,
        LocalDate expiry,
        LocalDate tradedAt,
        String strategyTag,
        String notes,
        Long closesLegId
) {}
