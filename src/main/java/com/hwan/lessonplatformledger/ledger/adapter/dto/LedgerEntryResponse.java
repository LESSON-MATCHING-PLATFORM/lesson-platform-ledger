package com.hwan.lessonplatformledger.ledger.adapter.dto;

import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        String entryId,
        String idempotencyKey,
        String transactionType,
        String transactionId,
        String orderId,
        String userId,
        String accountId,
        BigDecimal amount,
        String currency,
        String direction,
        String status,
        String description,
        Instant createdAt,
        String reversedEntryId,
        Long version
) {

    public static LedgerEntryResponse of(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getEntryId(),
                entry.getIdempotencyKey(),
                entry.getTransactionType().name(),
                entry.getTransactionId(),
                entry.getOrderId(),
                entry.getUserId(),
                entry.getAccountId(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getDirection().name(),
                entry.getStatus().name(),
                entry.getDescription(),
                entry.getCreatedAt(),
                entry.getReversedEntryId(),
                entry.getVersion()
        );
    }
}
