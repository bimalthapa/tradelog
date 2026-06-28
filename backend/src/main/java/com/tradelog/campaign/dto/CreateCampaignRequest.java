package com.tradelog.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateCampaignRequest(
    @NotBlank @Size(min = 1, max = 5) @Pattern(regexp = "[A-Z]+", message = "ticker must be 1–5 uppercase letters") String ticker,
    String label,
    String notes,
    @NotNull LocalDate openedAt,
    Long accountId
) {}
