package com.tradelog.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TradeLegRepository extends JpaRepository<TradeLeg, Long> {

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.campaignId = :campaignId")
    Double findNetCashFlowByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(CASE WHEN tl.action IN ('BTO', 'ASSIGNED') THEN tl.quantity WHEN tl.action = 'STC' THEN -tl.quantity ELSE 0 END) FROM TradeLeg tl WHERE tl.campaignId = :campaignId AND tl.instrumentType = 'STOCK'")
    Long findSharesHeldByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.campaignId = :campaignId AND tl.instrumentType = 'STOCK'")
    Double findStockNetCashFlowByCampaignId(@Param("campaignId") Long campaignId);

    List<TradeLeg> findByCampaignIdOrderByTradedAtAsc(Long campaignId);

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.instrumentType = 'OPTION' AND tl.netCashFlow > 0")
    Double findTotalPremium();

    @Query("SELECT SUM(tl.netCashFlow) FROM TradeLeg tl WHERE tl.instrumentType = 'OPTION'")
    Double findNetOptionsPnl();

    @Query("SELECT tl FROM TradeLeg tl WHERE tl.instrumentType = 'OPTION' ORDER BY tl.tradedAt ASC")
    List<TradeLeg> findAllOptionLegsOrderedByDate();

    @Query("""
        SELECT SUM(tl.netCashFlow) FROM TradeLeg tl, Campaign c
        WHERE tl.campaignId = c.id
          AND tl.instrumentType = 'OPTION' AND tl.netCashFlow > 0
          AND (:unassigned = true  AND c.account IS NULL
               OR :unassigned = false AND (:accountId IS NULL OR c.account.id = :accountId))
        """)
    Double findTotalPremiumFiltered(@Param("accountId") Long accountId,
                                    @Param("unassigned") boolean unassigned);

    @Query("""
        SELECT SUM(tl.netCashFlow) FROM TradeLeg tl, Campaign c
        WHERE tl.campaignId = c.id
          AND tl.instrumentType = 'OPTION'
          AND (:unassigned = true  AND c.account IS NULL
               OR :unassigned = false AND (:accountId IS NULL OR c.account.id = :accountId))
        """)
    Double findNetOptionsPnlFiltered(@Param("accountId") Long accountId,
                                     @Param("unassigned") boolean unassigned);

    @Query("""
        SELECT tl FROM TradeLeg tl, Campaign c
        WHERE tl.campaignId = c.id
          AND tl.instrumentType = 'OPTION'
          AND (:unassigned = true  AND c.account IS NULL
               OR :unassigned = false AND (:accountId IS NULL OR c.account.id = :accountId))
        ORDER BY tl.tradedAt ASC
        """)
    List<TradeLeg> findAllOptionLegsFilteredByDate(@Param("accountId") Long accountId,
                                                   @Param("unassigned") boolean unassigned);

    @Query("""
        SELECT SUM(tl.netCashFlow) FROM TradeLeg tl
        WHERE tl.campaignId  = :campaignId
        AND   tl.ticker      = :ticker
        AND   tl.instrumentType = 'OPTION'
        AND   tl.optionType  = :optionType
        AND   tl.strike      = :strike
        AND   tl.expiry      = :expiry
        """)
    Double findNetCashFlowByOptionPositionKey(
        @Param("campaignId") Long campaignId,
        @Param("ticker")     String ticker,
        @Param("optionType") String optionType,
        @Param("strike")     Double strike,
        @Param("expiry")     LocalDate expiry
    );
}
