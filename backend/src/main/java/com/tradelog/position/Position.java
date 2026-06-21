package com.tradelog.position;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "position")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "opening_leg_id")
    private Long openingLegId;

    @Column(name = "instrument_type", nullable = false)
    private String instrumentType;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "option_type")
    private String optionType;

    private Double strike;

    private LocalDate expiry;

    @Column(name = "open_action", nullable = false)
    private String openAction;

    @Column(name = "open_quantity", nullable = false)
    private Integer openQuantity;

    @Column(name = "avg_price", nullable = false)
    private Double avgPrice;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "opened_at", nullable = false)
    private LocalDate openedAt;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public Long getOpeningLegId() { return openingLegId; }
    public void setOpeningLegId(Long openingLegId) { this.openingLegId = openingLegId; }
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getOptionType() { return optionType; }
    public void setOptionType(String optionType) { this.optionType = optionType; }
    public Double getStrike() { return strike; }
    public void setStrike(Double strike) { this.strike = strike; }
    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }
    public String getOpenAction() { return openAction; }
    public void setOpenAction(String openAction) { this.openAction = openAction; }
    public Integer getOpenQuantity() { return openQuantity; }
    public void setOpenQuantity(Integer openQuantity) { this.openQuantity = openQuantity; }
    public Double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(Double avgPrice) { this.avgPrice = avgPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDate openedAt) { this.openedAt = openedAt; }
    public LocalDate getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDate closedAt) { this.closedAt = closedAt; }
}
