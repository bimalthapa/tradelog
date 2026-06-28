package com.tradelog.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeEntryRepository extends JpaRepository<TradeEntry, Long> {

    @Query("SELECT COUNT(te) FROM TradeEntry te")
    long findTotalCount();

    @Query("""
        SELECT te.strategyTag, SUM(tl.netCashFlow)
        FROM TradeEntry te, TradeLeg tl
        WHERE tl.tradeEntryId = te.id
        AND   te.strategyTag IS NOT NULL
        AND   te.strategyTag <> ''
        GROUP BY te.strategyTag
        ORDER BY SUM(tl.netCashFlow) DESC
        """)
    List<Object[]> findPnlByStrategy();

    @Query("""
        SELECT te.strategyTag, SUM(tl.netCashFlow)
        FROM TradeEntry te, TradeLeg tl, Campaign c
        WHERE tl.tradeEntryId = te.id
        AND   tl.campaignId = c.id
        AND   te.strategyTag IS NOT NULL
        AND   te.strategyTag <> ''
        AND   (:unassigned = true  AND c.account IS NULL
               OR :unassigned = false AND (:accountId IS NULL OR c.account.id = :accountId))
        GROUP BY te.strategyTag
        ORDER BY SUM(tl.netCashFlow) DESC
        """)
    List<Object[]> findPnlByStrategyFiltered(@Param("accountId") Long accountId,
                                              @Param("unassigned") boolean unassigned);
}
