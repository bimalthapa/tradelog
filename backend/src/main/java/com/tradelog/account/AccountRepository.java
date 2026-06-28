package com.tradelog.account;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAll(Sort sort);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.account.id = :accountId")
    long countCampaignsByAccountId(Long accountId);
}
