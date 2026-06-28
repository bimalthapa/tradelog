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
    public AnalyticsSummaryResponse getSummary(
        @RequestParam(required = false) Long accountId,
        @RequestParam(required = false, defaultValue = "false") boolean unassigned) {
        return analyticsService.getSummary(accountId, unassigned);
    }

    @GetMapping("/pnl-by-strategy")
    public List<PnlByStrategyItem> getPnlByStrategy(
        @RequestParam(required = false) Long accountId,
        @RequestParam(required = false, defaultValue = "false") boolean unassigned) {
        return analyticsService.getPnlByStrategy(accountId, unassigned);
    }

    @GetMapping("/cumulative")
    public CumulativeResponse getCumulative(
        @RequestParam(required = false) Long accountId,
        @RequestParam(required = false, defaultValue = "false") boolean unassigned) {
        return analyticsService.getCumulative(accountId, unassigned);
    }
}
