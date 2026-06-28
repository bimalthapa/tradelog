package com.tradelog.trade.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record SaveTradeRequest(
        Long campaignId,
        String rawInput,
        String strategyTag,
        String notes,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate tradedAt
) {}
