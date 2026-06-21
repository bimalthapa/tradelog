package com.tradelog.position;

import com.tradelog.trade.TradeLeg;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public void applyLeg(TradeLeg leg) {
        switch (leg.getAction()) {
            case "BTO", "STO", "ASSIGNED" -> open(leg);
            case "BTC", "STC" -> close(leg);
            case "EXPIRED" -> expire(leg);
            default -> throw new IllegalArgumentException("Unrecognized action: " + leg.getAction());
        }
    }

    private void open(TradeLeg leg) {
        Optional<Position> existing = findOpen(leg);
        if (existing.isPresent()) {
            Position pos = existing.get();
            double newAvg = ((pos.getOpenQuantity() * pos.getAvgPrice()) + (leg.getQuantity() * leg.getPrice()))
                    / (pos.getOpenQuantity() + leg.getQuantity());
            pos.setOpenQuantity(pos.getOpenQuantity() + leg.getQuantity());
            pos.setAvgPrice(newAvg);
            positionRepository.save(pos);
        } else {
            Position pos = new Position();
            pos.setCampaignId(leg.getCampaignId());
            pos.setOpeningLegId(leg.getId());
            pos.setInstrumentType(leg.getInstrumentType());
            pos.setTicker(leg.getTicker());
            pos.setOptionType(leg.getOptionType());
            pos.setStrike(leg.getStrike());
            pos.setExpiry(leg.getExpiry());
            pos.setOpenAction(leg.getAction().equals("ASSIGNED") ? "BTO" : leg.getAction());
            pos.setOpenQuantity(leg.getQuantity());
            pos.setAvgPrice(leg.getPrice());
            pos.setStatus("OPEN");
            pos.setOpenedAt(leg.getTradedAt());
            positionRepository.save(pos);
        }
    }

    private void close(TradeLeg leg) {
        Position pos = findOpen(leg)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No open position found for " + leg.getTicker() + " to close"));
        if (leg.getQuantity() > pos.getOpenQuantity()) {
            throw new IllegalArgumentException(
                    "Cannot close " + leg.getQuantity() + " contracts — only " + pos.getOpenQuantity() + " open");
        }
        int remaining = pos.getOpenQuantity() - leg.getQuantity();
        pos.setOpenQuantity(remaining);
        if (remaining <= 0) {
            pos.setStatus("CLOSED");
            pos.setClosedAt(leg.getTradedAt());
        }
        positionRepository.save(pos);
    }

    private void expire(TradeLeg leg) {
        Position pos = findOpen(leg)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No open position found for " + leg.getTicker() + " to expire"));
        pos.setStatus("CLOSED");
        pos.setClosedAt(leg.getTradedAt());
        positionRepository.save(pos);
    }

    private Optional<Position> findOpen(TradeLeg leg) {
        if ("STOCK".equals(leg.getInstrumentType())) {
            return positionRepository.findOpenStockPosition(leg.getCampaignId(), leg.getTicker());
        }
        return positionRepository.findOpenOptionPosition(
                leg.getCampaignId(), leg.getTicker(),
                leg.getOptionType(), leg.getStrike(), leg.getExpiry());
    }
}
