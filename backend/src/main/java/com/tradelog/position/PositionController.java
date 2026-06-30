package com.tradelog.position;

import com.tradelog.position.dto.PositionResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionRepository positionRepository;

    public PositionController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @GetMapping
    public List<PositionResponse> list(@RequestParam Long campaignId) {
        return positionRepository.findByCampaignId(campaignId)
                .stream()
                .map(p -> new PositionResponse(
                        p.getId(), p.getCampaignId(), p.getInstrumentType(), p.getTicker(),
                        p.getOptionType(), p.getStrike(), p.getExpiry(), p.getOpenAction(),
                        p.getOpenQuantity(), p.getAvgPrice(), p.getStatus(),
                        p.getOpenedAt(), p.getClosedAt(), p.getOpeningLegId()))
                .toList();
    }
}
