package com.tradelog.campaign;

import com.tradelog.campaign.dto.CampaignResponse;
import com.tradelog.campaign.dto.CreateCampaignRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public List<CampaignResponse> listAll() {
        return campaignService.listAll();
    }

    @GetMapping("/{id}")
    public CampaignResponse getById(@PathVariable Long id) {
        return campaignService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(@Valid @RequestBody CreateCampaignRequest request) {
        return campaignService.create(request);
    }

    @PatchMapping("/{id}/close")
    public CampaignResponse close(@PathVariable Long id) {
        return campaignService.close(id);
    }
}
