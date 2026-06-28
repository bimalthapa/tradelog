package com.tradelog.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
