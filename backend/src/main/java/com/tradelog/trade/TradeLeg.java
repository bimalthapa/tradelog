package com.tradelog.trade;

import jakarta.persistence.*;

@Entity
@Table(name = "trade_leg")
public class TradeLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "net_cash_flow", nullable = false)
    private Double netCashFlow;

    @Column(name = "instrument_type", nullable = false)
    private String instrumentType;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private Integer quantity;

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public Double getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(Double netCashFlow) { this.netCashFlow = netCashFlow; }
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
