package com.hwan.lessonplatformledger.ledger.application.dto;

import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;
import com.hwan.lessonplatformledger.ledger.domain.LedgerStatus;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordLedgerEntryResult(
    String entryId,
    String transactionId,
    LedgerTransactionType transactionType,
    BigDecimal amount,
    String currency,
    LedgerDirection direction,
    LedgerStatus status,
    Instant createdAt
) {
    public static RecordLedgerEntryResult of(LedgerEntry entry) {
        return new RecordLedgerEntryResult(
            entry.getEntryId(),
            entry.getTransactionId(),
            entry.getTransactionType(),
            entry.getAmount(),
            entry.getCurrency(),
            entry.getDirection(),
            entry.getStatus(),
            entry.getCreatedAt()
        );
    }
}
