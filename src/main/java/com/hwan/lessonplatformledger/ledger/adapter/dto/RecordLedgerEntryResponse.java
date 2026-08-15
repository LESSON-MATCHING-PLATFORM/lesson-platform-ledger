package com.hwan.lessonplatformledger.ledger.adapter.dto;

import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
import java.math.BigDecimal;
import java.time.Instant;

public record RecordLedgerEntryResponse(
    String entryId,
    String transactionId,
    String transactionType,
    BigDecimal amount,
    String currency,
    String direction,
    String status,
    Instant createdAt
) {
    public static RecordLedgerEntryResponse of(RecordLedgerEntryResult result) {
        return new RecordLedgerEntryResponse(
            result.entryId(),
            result.transactionId(),
            result.transactionType().name(),
            result.amount(),
            result.currency(),
            result.direction().name(),
            result.status().name(),
            result.createdAt()
        );
    }
}
