package com.tradelog.price.dto;

import java.time.Instant;

public record PriceResponse(
    String ticker,
    Double price,
    String source,
    Instant fetchedAt
) {}
