package com.hwan.lessonplatformledger.ledger.adapter.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RecordLedgerEntryRequest(
    @NotBlank
    @Size(max = 255)
    String idempotencyKey,

    @NotBlank
    String transactionType,

    @NotBlank
    String transactionId,
    String orderId,
    String userId,

    @NotBlank
    String accountId,

    @NotNull
    @Positive
    BigDecimal amount,

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    String currency,

    @NotBlank
    String direction,

    @Size(max = 1000)
    String description
) {
}
