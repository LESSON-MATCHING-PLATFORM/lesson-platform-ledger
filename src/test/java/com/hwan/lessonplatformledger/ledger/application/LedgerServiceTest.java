package com.hwan.lessonplatformledger.ledger.application;

import com.hwan.lessonplatformledger.ledger.adapter.LedgerRepository;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;
import com.hwan.lessonplatformledger.ledger.domain.LedgerStatus;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ledger 기록 서비스 단위 테스트")
class LedgerServiceTest {

    private static final String IDEMPOTENCY_KEY = "payment:payment-1:completed";

    @Mock
    private LedgerRepository ledgerRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerRepository);
    }

    @Test
    @DisplayName("새 원장을 저장하고 결과 DTO로 반환한다")
    void recordsEntryAndReturnsMappedResult() {
        Instant before = Instant.now();
        LedgerEntry savedEntry = savedEntry();
        when(ledgerRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(ledgerRepository.save(any(LedgerEntry.class))).thenReturn(savedEntry);

        RecordLedgerResult result = ledgerService.record(command());

        Instant after = Instant.now();
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());

        LedgerEntry recordedEntry = captor.getValue();
        assertThat(recordedEntry.getEntryId()).isNotBlank();
        assertThat(UUID.fromString(recordedEntry.getEntryId())).isNotNull();
        assertThat(recordedEntry.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(recordedEntry.getTransactionType()).isEqualTo(LedgerTransactionType.PAYMENT);
        assertThat(recordedEntry.getTransactionId()).isEqualTo("payment-1");
        assertThat(recordedEntry.getOrderId()).isEqualTo("order-1");
        assertThat(recordedEntry.getUserId()).isEqualTo("user-1");
        assertThat(recordedEntry.getAccountId()).isEqualTo("seller-1");
        assertThat(recordedEntry.getAmount()).isEqualByComparingTo("80000");
        assertThat(recordedEntry.getCurrency()).isEqualTo("KRW");
        assertThat(recordedEntry.getDirection()).isEqualTo(LedgerDirection.CREDIT);
        assertThat(recordedEntry.getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(recordedEntry.getDescription()).isEqualTo("강의 결제");
        assertThat(Duration.between(before, recordedEntry.getCreatedAt()).isNegative()).isFalse();
        assertThat(Duration.between(recordedEntry.getCreatedAt(), after).isNegative()).isFalse();

        assertThat(result.entryId()).isEqualTo(savedEntry.getEntryId());
        assertThat(result.transactionId()).isEqualTo(savedEntry.getTransactionId());
        assertThat(result.transactionType()).isEqualTo(savedEntry.getTransactionType());
        assertThat(result.amount()).isEqualByComparingTo(savedEntry.getAmount());
        assertThat(result.status()).isEqualTo(savedEntry.getStatus());
        assertThat(result.createdAt()).isEqualTo(savedEntry.getCreatedAt());
    }

    @Test
    @DisplayName("동일한 멱등성 키가 있으면 기존 원장을 반환하고 저장하지 않는다")
    void returnsExistingEntryWhenIdempotencyKeyWasAlreadyRecorded() {
        LedgerEntry existingEntry = savedEntry();
        when(ledgerRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingEntry));

        RecordLedgerResult result = ledgerService.record(command());

        assertThat(result.entryId()).isEqualTo(existingEntry.getEntryId());
        assertThat(result.status()).isEqualTo(existingEntry.getStatus());
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    private RecordLedgerCommand command() {
        return new RecordLedgerCommand(
                IDEMPOTENCY_KEY,
                LedgerTransactionType.PAYMENT,
                "payment-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.CREDIT,
                "강의 결제"
        );
    }

    private LedgerEntry savedEntry() {
        return new LedgerEntry(
                "ledger-1",
                IDEMPOTENCY_KEY,
                LedgerTransactionType.PAYMENT,
                "payment-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.CREDIT,
                LedgerStatus.POSTED,
                "강의 결제",
                Instant.parse("2026-08-15T00:00:00Z"),
                null,
                0L
        );
    }
}
