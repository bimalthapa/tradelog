package com.tradelog.trade.dto;

public record SaveTradeRequest(Long campaignId, String rawInput, String strategyTag, String notes) {}
