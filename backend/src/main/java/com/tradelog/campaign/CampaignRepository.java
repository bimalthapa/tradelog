package com.tradelog.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByOrderByOpenedAtDesc();

    @Query("SELECT c.id FROM Campaign c WHERE c.status = 'CLOSED'")
    List<Long> findAllClosedCampaignIds();
}
