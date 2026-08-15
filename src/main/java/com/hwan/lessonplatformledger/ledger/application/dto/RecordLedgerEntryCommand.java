package com.hwan.lessonplatformledger.ledger.application.dto;

import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerStatus;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;

import java.math.BigDecimal;

public record RecordLedgerEntryCommand(
    String idempotencyKey,
    LedgerTransactionType transactionType,
    String transactionId,
    String orderId,
    String userId,
    String accountId,
    BigDecimal amount,
    String currency,
    LedgerDirection direction,
    String description
) {}
