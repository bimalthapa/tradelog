package com.tradelog.analytics.dto;

public record AnalyticsSummaryResponse(
    double totalPremium,
    double netOptionsPnl,
    double campaignWinRate,
    double tradeWinRate,
    long   totalTrades
) {}
