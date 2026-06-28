package com.tradelog.analytics;

import com.tradelog.analytics.dto.*;
import com.tradelog.campaign.CampaignRepository;
import com.tradelog.position.Position;
import com.tradelog.position.PositionRepository;
import com.tradelog.trade.TradeLeg;
import com.tradelog.trade.TradeLegRepository;
import com.tradelog.trade.TradeEntryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalyticsService {

    private final TradeLegRepository   tradeLegRepo;
    private final TradeEntryRepository tradeEntryRepo;
    private final PositionRepository   positionRepo;
    private final CampaignRepository   campaignRepo;

    public AnalyticsService(TradeLegRepository tradeLegRepo,
                            TradeEntryRepository tradeEntryRepo,
                            PositionRepository positionRepo,
                            CampaignRepository campaignRepo) {
        this.tradeLegRepo   = tradeLegRepo;
        this.tradeEntryRepo = tradeEntryRepo;
        this.positionRepo   = positionRepo;
        this.campaignRepo   = campaignRepo;
    }

    public AnalyticsSummaryResponse getSummary() {
        Double raw1 = tradeLegRepo.findTotalPremium();
        Double raw2 = tradeLegRepo.findNetOptionsPnl();
        return new AnalyticsSummaryResponse(
            raw1 != null ? raw1 : 0.0,
            raw2 != null ? raw2 : 0.0,
            computeCampaignWinRate(),
            computeTradeWinRate(),
            tradeEntryRepo.findTotalCount()
        );
    }

    public List<PnlByStrategyItem> getPnlByStrategy() {
        return tradeEntryRepo.findPnlByStrategy().stream()
            .map(row -> new PnlByStrategyItem(
                (String) row[0],
                ((Number) row[1]).doubleValue()))
            .toList();
    }

    public CumulativeResponse getCumulative() {
        List<TradeLeg> legs = tradeLegRepo.findAllOptionLegsOrderedByDate();
        TreeMap<String, Double> premiumMap = new TreeMap<>();
        TreeMap<String, Double> pnlMap     = new TreeMap<>();

        for (TradeLeg leg : legs) {
            String month = leg.getTradedAt().toString().substring(0, 7);
            pnlMap.merge(month, leg.getNetCashFlow(), Double::sum);
            if (leg.getNetCashFlow() > 0) {
                premiumMap.merge(month, leg.getNetCashFlow(), Double::sum);
            }
        }

        return new CumulativeResponse(toCumulative(premiumMap), toCumulative(pnlMap));
    }

    private List<MonthPoint> toCumulative(TreeMap<String, Double> monthly) {
        List<MonthPoint> result  = new ArrayList<>();
        double           running = 0.0;
        for (Map.Entry<String, Double> e : monthly.entrySet()) {
            running += e.getValue();
            result.add(new MonthPoint(e.getKey(), running));
        }
        return result;
    }

    private double computeCampaignWinRate() {
        List<Long> closedIds = campaignRepo.findAllClosedCampaignIds();
        if (closedIds.isEmpty()) return 0.0;
        long wins = closedIds.stream().filter(id -> {
            Double ncf = tradeLegRepo.findNetCashFlowByCampaignId(id);
            return ncf != null && ncf > 0;
        }).count();
        return (double) wins / closedIds.size();
    }

    private double computeTradeWinRate() {
        List<Position> closed = positionRepo.findClosedOptionPositions();
        if (closed.isEmpty()) return 0.0;
        long wins = closed.stream().filter(p -> {
            Double ncf = tradeLegRepo.findNetCashFlowByOptionPositionKey(
                p.getCampaignId(), p.getTicker(), p.getOptionType(), p.getStrike(), p.getExpiry()
            );
            return ncf != null && ncf > 0;
        }).count();
        return (double) wins / closed.size();
    }
}
