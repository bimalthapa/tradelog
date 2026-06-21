package com.tradelog.trade;

import com.tradelog.campaign.CampaignRepository;
import com.tradelog.common.exception.ResourceNotFoundException;
import com.tradelog.position.PositionService;
import com.tradelog.trade.dto.SaveTradeRequest;
import com.tradelog.trade.dto.TradeLegResponse;
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
        leg.setTradedAt(LocalDate.now());
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
