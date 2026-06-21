package com.tradelog.position.dto;

import java.time.LocalDate;

public record PositionResponse(
        Long id,
        Long campaignId,
        String instrumentType,
        String ticker,
        String optionType,
        Double strike,
        LocalDate expiry,
        String openAction,
        int openQuantity,
        double avgPrice,
        String status,
        LocalDate openedAt,
        LocalDate closedAt
) {}
