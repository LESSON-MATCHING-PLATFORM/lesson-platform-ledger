package com.hwan.lessonplatformledger.ledger.application;

import com.hwan.lessonplatformledger.ledger.adapter.LedgerEntryRepository;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ledger Entry 기록 서비스 단위 테스트")
class LedgerEntryServiceTest {

    private static final String IDEMPOTENCY_KEY = "payment:payment-1:completed";

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private LedgerEntryService ledgerEntryService;

    @BeforeEach
    void setUp() {
        ledgerEntryService = new LedgerEntryService(ledgerEntryRepository);
    }

    @Test
    @DisplayName("새 원장을 저장하고 결과 DTO로 반환한다")
    void recordsEntryAndReturnsMappedResult() {
        Instant before = Instant.now();
        LedgerEntry savedEntry = savedEntry();
        when(ledgerEntryRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(savedEntry);

        RecordLedgerEntryResult result = ledgerEntryService.recordEntry(command());

        Instant after = Instant.now();
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

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
        when(ledgerEntryRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingEntry));

        RecordLedgerEntryResult result = ledgerEntryService.recordEntry(command());

        assertThat(result.entryId()).isEqualTo(existingEntry.getEntryId());
        assertThat(result.status()).isEqualTo(existingEntry.getStatus());
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("create 과정에서 유니크 충돌이 발생하면 기존 원장을 재조회해 반환한다")
    void returnsExistingEntryWhenCreateFailsWithDataIntegrityViolation() {
        LedgerEntry existingEntry = savedEntry();
        when(ledgerEntryRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty(), Optional.of(existingEntry));
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        RecordLedgerEntryResult result = ledgerEntryService.recordEntry(command());

        assertThat(result.entryId()).isEqualTo(existingEntry.getEntryId());
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(ledgerEntryRepository, times(2)).findByIdempotencyKey(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("create 과정의 유니크 충돌 후 기존 원장을 찾지 못하면 예외를 재전파한다")
    void rethrowsDataIntegrityViolationWhenExistingEntryCannotBeFoundAfterCreateConflict() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("duplicate idempotency key");
        when(ledgerEntryRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty(), Optional.empty());
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> ledgerEntryService.recordEntry(command()))
                .isSameAs(exception);

        verify(ledgerEntryRepository, times(2)).findByIdempotencyKey(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("보정 원장을 기록하고 기존 원장을 역분개 상태로 저장한다")
    void recordsAdjustmentAndReversesOriginalEntry() {
        LedgerEntry originalEntry = savedEntry();
        LedgerEntry adjustmentEntry = adjustmentEntry();
        RecordLedgerEntryCommand adjustmentCommand = adjustmentCommand();
        when(ledgerEntryRepository.findByEntryId(originalEntry.getEntryId()))
                .thenReturn(Optional.of(originalEntry));
        when(ledgerEntryRepository.findByIdempotencyKey(adjustmentCommand.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenReturn(adjustmentEntry, originalEntry);

        RecordLedgerEntryResult result = ledgerEntryService.recordAdjustmentEntry(
                originalEntry.getEntryId(), adjustmentCommand
        );

        assertThat(result.entryId()).isEqualTo(adjustmentEntry.getEntryId());
        assertThat(originalEntry.getStatus()).isEqualTo(LedgerStatus.REVERSED);
        assertThat(originalEntry.getReversedEntryId()).isEqualTo(adjustmentEntry.getEntryId());
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(LedgerEntry::getStatus)
                .containsExactly(LedgerStatus.POSTED, LedgerStatus.REVERSED);
    }

    @Test
    @DisplayName("원본 원장이 없으면 보정 원장을 기록하지 않는다")
    void doesNotRecordAdjustmentWhenOriginalEntryDoesNotExist() {
        RecordLedgerEntryCommand adjustmentCommand = adjustmentCommand();
        when(ledgerEntryRepository.findByEntryId("missing-entry"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        ledgerEntryService.recordAdjustmentEntry("missing-entry", adjustmentCommand))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(ledgerEntryRepository, never()).findByIdempotencyKey(adjustmentCommand.idempotencyKey());
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("동일한 보정 멱등성 키가 있으면 기존 보정 원장을 사용한다")
    void reusesExistingAdjustmentForSameIdempotencyKey() {
        LedgerEntry originalEntry = savedEntry();
        LedgerEntry existingAdjustment = adjustmentEntry();
        RecordLedgerEntryCommand adjustmentCommand = adjustmentCommand();
        when(ledgerEntryRepository.findByEntryId(originalEntry.getEntryId()))
                .thenReturn(Optional.of(originalEntry));
        when(ledgerEntryRepository.findByIdempotencyKey(adjustmentCommand.idempotencyKey()))
                .thenReturn(Optional.of(existingAdjustment));
        when(ledgerEntryRepository.save(originalEntry)).thenReturn(originalEntry);

        RecordLedgerEntryResult result = ledgerEntryService.recordAdjustmentEntry(
                originalEntry.getEntryId(), adjustmentCommand
        );

        assertThat(result.entryId()).isEqualTo(existingAdjustment.getEntryId());
        assertThat(originalEntry.getStatus()).isEqualTo(LedgerStatus.REVERSED);
        assertThat(originalEntry.getReversedEntryId()).isEqualTo(existingAdjustment.getEntryId());
        verify(ledgerEntryRepository, never()).save(existingAdjustment);
        verify(ledgerEntryRepository).save(originalEntry);
    }

    @Test
    @DisplayName("entryId로 원장을 조회한다")
    void findsEntryByEntryId() {
        LedgerEntry entry = savedEntry();
        when(ledgerEntryRepository.findByEntryId(entry.getEntryId())).thenReturn(Optional.of(entry));

        assertThat(ledgerEntryService.findEntry(entry.getEntryId())).isSameAs(entry);
    }

    @Test
    @DisplayName("존재하지 않는 entryId를 조회하면 전용 예외를 던진다")
    void throwsNotFoundWhenEntryDoesNotExist() {
        when(ledgerEntryRepository.findByEntryId("missing-entry")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ledgerEntryService.findEntry("missing-entry"))
                .isInstanceOf(LedgerEntryNotFoundException.class);
    }

    @Test
    @DisplayName("transactionId로 원장 목록을 조회한다")
    void findsEntriesByTransactionId() {
        List<LedgerEntry> entries = List.of(savedEntry());
        when(ledgerEntryRepository.findAllByTransactionIdOrderByCreatedAtAsc("payment-1"))
                .thenReturn(entries);

        assertThat(ledgerEntryService.findEntriesByTransactionId("payment-1")).containsExactlyElementsOf(entries);
    }

    @Test
    @DisplayName("orderId로 원장 목록을 조회한다")
    void findsEntriesByOrderId() {
        List<LedgerEntry> entries = List.of(savedEntry());
        when(ledgerEntryRepository.findAllByOrderIdOrderByCreatedAtAsc("order-1"))
                .thenReturn(entries);

        assertThat(ledgerEntryService.findEntriesByOrderId("order-1")).containsExactlyElementsOf(entries);
    }

    @Test
    @DisplayName("accountId로 원장 목록을 조회한다")
    void findsEntriesByAccountId() {
        List<LedgerEntry> entries = List.of(savedEntry());
        when(ledgerEntryRepository.findAllByAccountIdOrderByCreatedAtAsc("seller-1"))
                .thenReturn(entries);

        assertThat(ledgerEntryService.findEntriesByAccountId("seller-1")).containsExactlyElementsOf(entries);
    }

    private RecordLedgerEntryCommand command() {
        return new RecordLedgerEntryCommand(
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

    private RecordLedgerEntryCommand adjustmentCommand() {
        return new RecordLedgerEntryCommand(
                "refund:payment-1:completed",
                LedgerTransactionType.REFUND,
                "refund-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.DEBIT,
                "결제 환불"
        );
    }

    private LedgerEntry adjustmentEntry() {
        return new LedgerEntry(
                "ledger-adjustment-1",
                "refund:payment-1:completed",
                LedgerTransactionType.REFUND,
                "refund-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.DEBIT,
                LedgerStatus.POSTED,
                "결제 환불",
                Instant.parse("2026-08-15T00:01:00Z"),
                null,
                0L
        );
    }
}
