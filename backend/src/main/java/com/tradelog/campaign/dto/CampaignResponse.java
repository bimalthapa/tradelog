package com.tradelog.campaign.dto;

import java.time.LocalDate;

public record CampaignResponse(
    Long id,
    String ticker,
    String label,
    String status,
    String notes,
    LocalDate openedAt,
    LocalDate closedAt,
    Double realizedPnl,
    double netCashFlow,
    Double costBasis,
    Integer sharesHeld,
    int openPositionCount
) {}
