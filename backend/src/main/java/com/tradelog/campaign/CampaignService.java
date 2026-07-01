package com.tradelog.campaign;

import com.tradelog.account.Account;
import com.tradelog.account.AccountRepository;
import com.tradelog.campaign.dto.CampaignResponse;
import com.tradelog.campaign.dto.CreateCampaignRequest;
import com.tradelog.campaign.dto.UpdateCampaignRequest;
import com.tradelog.common.exception.ResourceNotFoundException;
import com.tradelog.position.Position;
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
    private final AccountRepository accountRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           TradeLegRepository tradeLegRepository,
                           PositionRepository positionRepository,
                           AccountRepository accountRepository) {
        this.campaignRepository = campaignRepository;
        this.tradeLegRepository = tradeLegRepository;
        this.positionRepository = positionRepository;
        this.accountRepository  = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listFiltered(Long accountId, boolean unassigned) {
        List<Campaign> campaigns;
        if (unassigned) {
            campaigns = campaignRepository.findUnassigned();
        } else if (accountId != null) {
            campaigns = campaignRepository.findByAccountId(accountId);
        } else {
            campaigns = campaignRepository.findAllByOrderByOpenedAtDesc();
        }
        return campaigns.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
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
        if (request.accountId() != null) {
            Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountId()));
            campaign.setAccount(account);
        }
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest req) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
        if (req.isClearAccount()) {
            campaign.setAccount(null);
        } else if (req.getAccountId() != null) {
            Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + req.getAccountId()));
            campaign.setAccount(account);
        }
        return toResponse(campaignRepository.save(campaign));
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

        double openValue = 0.0;
        for (Position p : positionRepository.findOpenPositionsByCampaignId(cid)) {
            double multiplier = "OPTION".equals(p.getInstrumentType()) ? 100.0 : 1.0;
            double sign = "BTO".equals(p.getOpenAction()) ? -1.0 : 1.0;
            openValue += sign * p.getAvgPrice() * p.getOpenQuantity() * multiplier;
        }
        double realizedPnl = netCashFlow - openValue;

        return new CampaignResponse(
                campaign.getId(),
                campaign.getTicker(),
                campaign.getLabel(),
                campaign.getStatus(),
                campaign.getNotes(),
                campaign.getOpenedAt(),
                campaign.getClosedAt(),
                realizedPnl,
                netCashFlow,
                costBasis,
                sharesHeld > 0 ? (int) sharesHeld : null,
                openPositionCount,
                campaign.getAccount() != null ? campaign.getAccount().getId() : null,
                campaign.getAccount() != null ? campaign.getAccount().getName() : null
        );
    }
}
