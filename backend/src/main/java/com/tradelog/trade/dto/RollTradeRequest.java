package com.tradelog.trade.dto;

import java.time.LocalDate;

public record RollTradeRequest(
        Long campaignId,
        Long positionId,
        int qty,
        double btcPrice,
        double newStrike,
        String newExpiry,
        double stoPrice,
        LocalDate tradedAt,
        String notes
) {}
