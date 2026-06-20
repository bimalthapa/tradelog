package com.tradelog.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeLegRepository extends JpaRepository<TradeLeg, Long> {

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.campaignId = :campaignId")
    Double findNetCashFlowByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(CASE WHEN tl.action IN ('BTO', 'ASSIGNED') THEN tl.quantity WHEN tl.action = 'STC' THEN -tl.quantity ELSE 0 END) FROM TradeLeg tl WHERE tl.campaignId = :campaignId AND tl.instrumentType = 'STOCK'")
    Long findSharesHeldByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.campaignId = :campaignId AND tl.instrumentType = 'STOCK'")
    Double findStockNetCashFlowByCampaignId(@Param("campaignId") Long campaignId);
}
