package com.tradelog.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByOrderByOpenedAtDesc();

    @Query("SELECT c.id FROM Campaign c WHERE c.status = 'CLOSED'")
    List<Long> findAllClosedCampaignIds();

    @Query("""
        SELECT c.id FROM Campaign c WHERE c.status = 'CLOSED'
          AND (:unassigned = true  AND c.account IS NULL
               OR :unassigned = false AND (:accountId IS NULL OR c.account.id = :accountId))
        """)
    List<Long> findClosedCampaignIdsFiltered(@Param("accountId") Long accountId,
                                              @Param("unassigned") boolean unassigned);

    @Query("SELECT c FROM Campaign c WHERE c.account.id = :accountId ORDER BY c.openedAt DESC")
    List<Campaign> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT c FROM Campaign c WHERE c.account IS NULL ORDER BY c.openedAt DESC")
    List<Campaign> findUnassigned();
}
