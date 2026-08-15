package com.hwan.lessonplatformledger.ledger.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Table("ledger_entries")
public class LedgerEntry {

    @Id
    private final String entryId;
    private final String idempotencyKey;
    private final LedgerTransactionType transactionType;
    private final String transactionId;
    private final String orderId;
    private final String userId; // 행위 주체 (ex. 결제자)
    private final String accountId; // 판매자 정산 계좌
    private final BigDecimal amount;
    private final String currency;
    private final LedgerDirection direction;
    private LedgerStatus status;
    private final String description;
    private final Instant createdAt;
    private String reversedEntryId;

    @Version
    private Long version;

    public LedgerEntry(
            String entryId,
            String idempotencyKey,
            LedgerTransactionType transactionType,
            String transactionId,
            String orderId,
            String userId,
            String accountId,
            BigDecimal amount,
            String currency,
            LedgerDirection direction,
            LedgerStatus status,
            String description,
            Instant createdAt,
            String reversedEntryId,
            Long version
    ) {
        this.entryId = requireText(entryId, "entryId");
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        this.transactionType = Objects.requireNonNull(transactionType, "transactionType");
        this.transactionId = requireText(transactionId, "transactionId");
        this.orderId = orderId;
        this.userId = userId;
        this.accountId = requireText(accountId, "accountId");
        this.amount = requirePositive(amount);
        this.currency = requireText(currency, "currency");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.status = Objects.requireNonNull(status, "status");
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.reversedEntryId = reversedEntryId;
        this.version = version;
    }

    public static LedgerEntry recordEntry(
            String entryId,
            String idempotencyKey,
            LedgerTransactionType transactionType,
            String transactionId,
            String orderId,
            String userId,
            String accountId,
            BigDecimal amount,
            String currency,
            LedgerDirection direction,
            String description,
            Instant createdAt
    ) {
        return new LedgerEntry(
                entryId,
                idempotencyKey,
                transactionType,
                transactionId,
                orderId,
                userId,
                accountId,
                amount,
                currency,
                direction,
                LedgerStatus.POSTED,
                description,
                createdAt,
                null,
                null
        );
    }

    public void markReversed(String reversalEntryId) {
        if (status == LedgerStatus.REVERSED) {
            throw new IllegalStateException("Ledger entry is already reversed: " + entryId);
        }
        this.status = LedgerStatus.REVERSED;
        this.reversedEntryId = requireText(reversalEntryId, "reversalEntryId");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return value;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LedgerTransactionType getTransactionType() {
        return transactionType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LedgerDirection getDirection() {
        return direction;
    }

    public LedgerStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getReversedEntryId() {
        return reversedEntryId;
    }

    public Long getVersion() {
        return version;
    }
}
