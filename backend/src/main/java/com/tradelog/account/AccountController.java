package com.tradelog.account;

import com.tradelog.account.dto.AccountResponse;
import com.tradelog.account.dto.CreateAccountRequest;
import com.tradelog.account.dto.RenameAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    public List<AccountResponse> list() { return service.listAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    public AccountResponse rename(@PathVariable Long id,
                                  @Valid @RequestBody RenameAccountRequest req) {
        return service.rename(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
