package com.tradelog.position;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("SELECT COUNT(p) FROM Position p WHERE p.campaignId = :campaignId AND p.status = 'OPEN'")
    long findOpenCountByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT p FROM Position p WHERE p.campaignId = :campaignId AND p.instrumentType = 'STOCK' AND p.ticker = :ticker AND p.status = 'OPEN'")
    Optional<Position> findOpenStockPosition(@Param("campaignId") Long campaignId, @Param("ticker") String ticker);

    @Query("SELECT p FROM Position p WHERE p.campaignId = :campaignId AND p.instrumentType = 'OPTION' AND p.ticker = :ticker AND p.optionType = :optionType AND p.strike = :strike AND p.expiry = :expiry AND p.status = 'OPEN'")
    Optional<Position> findOpenOptionPosition(@Param("campaignId") Long campaignId, @Param("ticker") String ticker, @Param("optionType") String optionType, @Param("strike") Double strike, @Param("expiry") LocalDate expiry);

    @Modifying
    @Transactional
    @Query("DELETE FROM Position p WHERE p.campaignId = :campaignId")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);

    List<Position> findByCampaignId(Long campaignId);

    @Query("SELECT p FROM Position p WHERE p.campaignId = :campaignId AND p.status = 'OPEN'")
    List<Position> findOpenPositionsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT p FROM Position p WHERE p.status = 'CLOSED' AND p.instrumentType = 'OPTION'")
    List<Position> findClosedOptionPositions();
}
