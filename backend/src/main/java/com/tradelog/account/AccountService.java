package com.tradelog.account;

import com.tradelog.account.dto.AccountResponse;
import com.tradelog.account.dto.CreateAccountRequest;
import com.tradelog.account.dto.RenameAccountRequest;
import com.tradelog.common.exception.ConflictException;
import com.tradelog.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository repo;

    public AccountService(AccountRepository repo) { this.repo = repo; }

    public List<AccountResponse> listAll() {
        return repo.findAll(Sort.by("name")).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse create(CreateAccountRequest req) {
        return AccountResponse.from(repo.save(new Account(req.name())));
    }

    public AccountResponse rename(Long id, RenameAccountRequest req) {
        Account a = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        a.setName(req.name());
        return AccountResponse.from(repo.save(a));
    }

    public void delete(Long id) {
        if (!repo.existsById(id))
            throw new ResourceNotFoundException("Account not found: " + id);
        if (repo.countCampaignsByAccountId(id) > 0)
            throw new ConflictException("Cannot delete account with assigned campaigns");
        repo.deleteById(id);
    }
}
