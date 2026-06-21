package com.tradelog.trade;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeEntryRepository extends JpaRepository<TradeEntry, Long> {
}
