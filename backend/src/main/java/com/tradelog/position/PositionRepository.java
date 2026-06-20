package com.tradelog.position;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("SELECT COUNT(p) FROM Position p WHERE p.campaignId = :campaignId AND p.status = 'OPEN'")
    long findOpenCountByCampaignId(@Param("campaignId") Long campaignId);
}
