package com.tradelog.campaign.dto;

public class UpdateCampaignRequest {
    private Long accountId;
    private boolean clearAccount;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public boolean isClearAccount() { return clearAccount; }
    public void setClearAccount(boolean clearAccount) { this.clearAccount = clearAccount; }
}
