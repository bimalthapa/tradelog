package com.tradelog.trade.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public record BatchSaveTradeRequest(
        Long campaignId,
        List<String> rawInputs,
        String strategyTag,
        String notes,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate tradedAt
) {}
