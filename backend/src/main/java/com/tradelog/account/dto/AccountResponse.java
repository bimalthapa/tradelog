package com.tradelog.account.dto;

import com.tradelog.account.Account;

public record AccountResponse(Long id, String name) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getName());
    }
}
