package com.tradelog.trade;

import com.tradelog.campaign.Campaign;
import com.tradelog.campaign.CampaignRepository;
import com.tradelog.common.exception.BadRequestException;
import com.tradelog.common.exception.ResourceNotFoundException;
import com.tradelog.position.PositionService;
import com.tradelog.trade.dto.SaveTradeRequest;
import com.tradelog.trade.dto.TradeLegResponse;
import com.tradelog.trade.dto.UpdateTradeRequest;
import com.tradelog.trade.parser.ParsedTradeInput;
import com.tradelog.trade.parser.TradeInputParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeEntryService {

    private final TradeInputParser parser;
    private final CampaignRepository campaignRepository;
    private final TradeEntryRepository tradeEntryRepository;
    private final TradeLegRepository tradeLegRepository;
    private final PositionService positionService;

    public TradeEntryService(TradeInputParser parser,
                             CampaignRepository campaignRepository,
                             TradeEntryRepository tradeEntryRepository,
                             TradeLegRepository tradeLegRepository,
                             PositionService positionService) {
        this.parser = parser;
        this.campaignRepository = campaignRepository;
        this.tradeEntryRepository = tradeEntryRepository;
        this.tradeLegRepository = tradeLegRepository;
        this.positionService = positionService;
    }

    @Transactional
    public TradeLegResponse save(SaveTradeRequest req) {
        campaignRepository.findById(req.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + req.campaignId()));

        ParsedTradeInput parsed = parser.parse(req.rawInput());
        if (!parsed.valid()) throw new IllegalArgumentException(parsed.error());

        TradeEntry entry = new TradeEntry();
        entry.setCampaignId(req.campaignId());
        entry.setEnteredAt(LocalDateTime.now());
        entry.setRawInput(req.rawInput());
        entry.setNotes(req.notes());
        entry.setStrategyTag(req.strategyTag() != null && !req.strategyTag().isBlank() ? req.strategyTag() : parsed.strategy());
        TradeEntry savedEntry = tradeEntryRepository.save(entry);

        TradeLeg leg = new TradeLeg();
        leg.setTradeEntryId(savedEntry.getId());
        leg.setCampaignId(req.campaignId());
        leg.setInstrumentType(parsed.instrumentType());
        leg.setAction(parsed.action());
        leg.setTicker(parsed.ticker());
        leg.setQuantity(parsed.qty());
        leg.setPrice(parsed.price());
        leg.setNetCashFlow(parsed.cashFlow());
        leg.setOptionType(parsed.optionType());
        leg.setStrike(parsed.strike());
        leg.setExpiry(parsed.expiry());
        leg.setTradedAt(req.tradedAt() != null ? req.tradedAt() : LocalDate.now());
        TradeLeg savedLeg = tradeLegRepository.save(leg);

        positionService.applyLeg(savedLeg);

        return toResponse(savedLeg, savedEntry);
    }

    public List<TradeLegResponse> listByCampaign(Long campaignId) {
        return tradeLegRepository.findByCampaignIdOrderByTradedAtAsc(campaignId)
                .stream()
                .map(leg -> {
                    TradeEntry entry = tradeEntryRepository.findById(leg.getTradeEntryId()).orElse(null);
                    return toResponse(leg, entry);
                })
                .toList();
    }

    @Transactional
    public TradeLegResponse update(Long legId, UpdateTradeRequest req) {
        TradeLeg leg = tradeLegRepository.findById(legId)
            .orElseThrow(() -> new ResourceNotFoundException("Trade not found: " + legId));

        Campaign campaign = campaignRepository.findById(leg.getCampaignId())
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        if (!"OPEN".equals(campaign.getStatus())) {
            throw new BadRequestException("Cannot edit trades in a closed campaign");
        }

        TradeEntry entry = tradeEntryRepository.findById(leg.getTradeEntryId())
            .orElseThrow(() -> new ResourceNotFoundException("Trade entry not found"));

        boolean positionsAffected = req.quantity() != null || req.price() != null || req.tradedAt() != null;

        if (req.quantity() != null) {
            if (req.quantity() < 1) throw new BadRequestException("Quantity must be ≥ 1");
            leg.setQuantity(req.quantity());
        }
        if (req.price() != null) {
            if (req.price() <= 0) throw new BadRequestException("Price must be > 0");
            leg.setPrice(req.price());
        }
        if (req.tradedAt() != null) leg.setTradedAt(req.tradedAt());

        if (req.quantity() != null || req.price() != null) {
            double qty = req.quantity() != null ? req.quantity() : leg.getQuantity();
            double price = req.price() != null ? req.price() : leg.getPrice();
            double cashFlow = "STOCK".equals(leg.getInstrumentType()) ? qty * price : qty * price * 100;
            String action = leg.getAction();
            if ("BTO".equals(action) || "BTC".equals(action) || "ASSIGNED".equals(action)) {
                cashFlow = -cashFlow;
            }
            leg.setNetCashFlow(cashFlow);
        }

        if (req.strategyTag() != null) entry.setStrategyTag(req.strategyTag());
        if (req.notes() != null) entry.setNotes(req.notes());

        tradeLegRepository.save(leg);
        tradeEntryRepository.save(entry);

        if (positionsAffected) {
            positionService.rebuildPositions(leg.getCampaignId());
        }

        return toResponse(leg, entry);
    }

    private TradeLegResponse toResponse(TradeLeg leg, TradeEntry entry) {
        return new TradeLegResponse(
                leg.getId(),
                leg.getTradeEntryId(),
                leg.getCampaignId(),
                leg.getInstrumentType(),
                leg.getAction(),
                leg.getTicker(),
                leg.getQuantity(),
                leg.getPrice(),
                leg.getNetCashFlow(),
                leg.getOptionType(),
                leg.getStrike(),
                leg.getExpiry(),
                leg.getTradedAt(),
                entry != null ? entry.getStrategyTag() : null,
                entry != null ? entry.getNotes() : null
        );
    }
}
