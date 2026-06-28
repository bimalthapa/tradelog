package com.tradelog.analytics.dto;

import java.util.List;

public record CumulativeResponse(
    List<MonthPoint> premium,
    List<MonthPoint> optionsPnl
) {}
