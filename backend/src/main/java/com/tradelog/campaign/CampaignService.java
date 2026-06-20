package com.tradelog.campaign;

import com.tradelog.campaign.dto.CampaignResponse;
import com.tradelog.campaign.dto.CreateCampaignRequest;
import com.tradelog.common.exception.ResourceNotFoundException;
import com.tradelog.position.PositionRepository;
import com.tradelog.trade.TradeLegRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final TradeLegRepository tradeLegRepository;
    private final PositionRepository positionRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           TradeLegRepository tradeLegRepository,
                           PositionRepository positionRepository) {
        this.campaignRepository = campaignRepository;
        this.tradeLegRepository = tradeLegRepository;
        this.positionRepository = positionRepository;
    }

    public List<CampaignResponse> listAll() {
        return campaignRepository.findAllByOrderByOpenedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CampaignResponse getById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
        return toResponse(campaign);
    }

    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setTicker(request.ticker());
        campaign.setLabel(request.label());
        campaign.setNotes(request.notes());
        campaign.setOpenedAt(request.openedAt());
        Campaign saved = campaignRepository.save(campaign);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse close(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
        campaign.setStatus("CLOSED");
        campaign.setClosedAt(LocalDate.now());
        return toResponse(campaignRepository.save(campaign));
    }

    private CampaignResponse toResponse(Campaign campaign) {
        Long cid = campaign.getId();

        Double rawCashFlow = tradeLegRepository.findNetCashFlowByCampaignId(cid);
        double netCashFlow = rawCashFlow != null ? rawCashFlow : 0.0;

        Long rawShares = tradeLegRepository.findSharesHeldByCampaignId(cid);
        long sharesHeld = rawShares != null ? rawShares : 0L;

        Double costBasis = null;
        if (sharesHeld > 0) {
            Double stockCashFlow = tradeLegRepository.findStockNetCashFlowByCampaignId(cid);
            if (stockCashFlow != null) {
                costBasis = -stockCashFlow / sharesHeld;
            }
        }

        int openPositionCount = (int) positionRepository.findOpenCountByCampaignId(cid);

        return new CampaignResponse(
                campaign.getId(),
                campaign.getTicker(),
                campaign.getLabel(),
                campaign.getStatus(),
                campaign.getNotes(),
                campaign.getOpenedAt(),
                campaign.getClosedAt(),
                campaign.getRealizedPnl(),
                netCashFlow,
                costBasis,
                sharesHeld > 0 ? (int) sharesHeld : null,
                openPositionCount
        );
    }
}
