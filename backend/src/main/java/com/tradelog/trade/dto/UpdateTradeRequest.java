package com.tradelog.trade.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record UpdateTradeRequest(
    Integer quantity,
    Double price,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate tradedAt,
    String strategyTag,
    String notes
) {}
