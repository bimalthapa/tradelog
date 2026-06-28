package com.tradelog.analytics;

import com.tradelog.analytics.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary() {
        return analyticsService.getSummary();
    }

    @GetMapping("/pnl-by-strategy")
    public List<PnlByStrategyItem> getPnlByStrategy() {
        return analyticsService.getPnlByStrategy();
    }

    @GetMapping("/cumulative")
    public CumulativeResponse getCumulative() {
        return analyticsService.getCumulative();
    }
}
