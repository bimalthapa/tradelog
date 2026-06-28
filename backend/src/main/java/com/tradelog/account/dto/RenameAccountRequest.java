package com.tradelog.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAccountRequest(
    @NotBlank @Size(min = 1, max = 50) String name
) {}
