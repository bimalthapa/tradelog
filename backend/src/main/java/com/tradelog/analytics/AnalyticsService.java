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

    public AnalyticsSummaryResponse getSummary(Long accountId, boolean unassigned) {
        Double raw1 = tradeLegRepo.findTotalPremiumFiltered(accountId, unassigned);
        Double raw2 = tradeLegRepo.findNetOptionsPnlFiltered(accountId, unassigned);
        return new AnalyticsSummaryResponse(
            raw1 != null ? raw1 : 0.0,
            raw2 != null ? raw2 : 0.0,
            computeCampaignWinRate(accountId, unassigned),
            computeTradeWinRate(accountId, unassigned),
            tradeEntryRepo.findTotalCount()
        );
    }

    public List<PnlByStrategyItem> getPnlByStrategy(Long accountId, boolean unassigned) {
        return tradeEntryRepo.findPnlByStrategyFiltered(accountId, unassigned).stream()
            .map(row -> new PnlByStrategyItem(
                (String) row[0],
                ((Number) row[1]).doubleValue()))
            .toList();
    }

    public CumulativeResponse getCumulative(Long accountId, boolean unassigned) {
        List<TradeLeg> legs = tradeLegRepo.findAllOptionLegsFilteredByDate(accountId, unassigned);
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

    private double computeCampaignWinRate(Long accountId, boolean unassigned) {
        List<Long> closedIds = campaignRepo.findClosedCampaignIdsFiltered(accountId, unassigned);
        if (closedIds.isEmpty()) return 0.0;
        long wins = closedIds.stream().filter(id -> {
            Double ncf = tradeLegRepo.findNetCashFlowByCampaignId(id);
            return ncf != null && ncf > 0;
        }).count();
        return (double) wins / closedIds.size();
    }

    private double computeTradeWinRate(Long accountId, boolean unassigned) {
        List<Long> campaignIds;
        if (unassigned) {
            campaignIds = campaignRepo.findClosedCampaignIdsFiltered(null, true);
        } else if (accountId != null) {
            campaignIds = campaignRepo.findClosedCampaignIdsFiltered(accountId, false);
        } else {
            campaignIds = campaignRepo.findAllClosedCampaignIds();
        }
        List<Position> closed = positionRepo.findClosedOptionPositions().stream()
            .filter(p -> campaignIds.contains(p.getCampaignId()))
            .toList();
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
