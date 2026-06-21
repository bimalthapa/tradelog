package com.tradelog.trade;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "trade_leg")
public class TradeLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_entry_id", nullable = false)
    private Long tradeEntryId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "instrument_type", nullable = false)
    private String instrumentType;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;

    @Column(name = "net_cash_flow", nullable = false)
    private Double netCashFlow;

    @Column(name = "option_type")
    private String optionType;

    private Double strike;

    private LocalDate expiry;

    @Column(name = "closes_leg_id")
    private Long closesLegId;

    @Column(name = "traded_at", nullable = false)
    private LocalDate tradedAt;

    public Long getId() { return id; }
    public Long getTradeEntryId() { return tradeEntryId; }
    public void setTradeEntryId(Long tradeEntryId) { this.tradeEntryId = tradeEntryId; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(Double netCashFlow) { this.netCashFlow = netCashFlow; }
    public String getOptionType() { return optionType; }
    public void setOptionType(String optionType) { this.optionType = optionType; }
    public Double getStrike() { return strike; }
    public void setStrike(Double strike) { this.strike = strike; }
    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }
    public Long getClosesLegId() { return closesLegId; }
    public void setClosesLegId(Long closesLegId) { this.closesLegId = closesLegId; }
    public LocalDate getTradedAt() { return tradedAt; }
    public void setTradedAt(LocalDate tradedAt) { this.tradedAt = tradedAt; }
}
