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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Import(LedgerService.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = "spring.sql.init.mode=always")
@DisplayName("Ledger 기록 통합 테스트")
class LedgerServiceIntegrationTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
    }

    @Test
    @DisplayName("원장을 실제 데이터베이스에 저장하고 모든 필드를 조회한다")
    void recordsEntryAndPersistsAllFields() {
        RecordLedgerResult result = ledgerService.record(command("payment:payment-1:completed"));

        LedgerEntry saved = ledgerRepository.findById(result.entryId()).orElseThrow();

        assertThat(saved.getEntryId()).isEqualTo(result.entryId());
        assertThat(saved.getIdempotencyKey()).isEqualTo("payment:payment-1:completed");
        assertThat(saved.getTransactionType()).isEqualTo(LedgerTransactionType.PAYMENT);
        assertThat(saved.getTransactionId()).isEqualTo("payment-1");
        assertThat(saved.getOrderId()).isEqualTo("order-1");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getAccountId()).isEqualTo("seller-1");
        assertThat(saved.getAmount()).isEqualByComparingTo("80000");
        assertThat(saved.getCurrency()).isEqualTo("KRW");
        assertThat(saved.getDirection()).isEqualTo(LedgerDirection.CREDIT);
        assertThat(saved.getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(saved.getDescription()).isEqualTo("강의 결제");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일한 멱등성 키 재요청 시 중복 행을 생성하지 않는다")
    void returnsPersistedEntryWhenSameIdempotencyKeyIsRecordedAgain() {
        RecordLedgerResult first = ledgerService.record(command("payment:payment-1:completed"));

        RecordLedgerResult second = ledgerService.record(command("payment:payment-1:completed"));

        assertThat(second.entryId()).isEqualTo(first.entryId());
        assertThat(second.transactionId()).isEqualTo(first.transactionId());
        assertThat(second.amount()).isEqualByComparingTo(first.amount());
        assertThat(second.status()).isEqualTo(first.status());
        assertThat(second.createdAt()).isEqualTo(first.createdAt());
        assertThat(ledgerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 멱등성 키는 별도의 원장으로 저장한다")
    void recordsDifferentTransactionsWithDifferentIdempotencyKeys() {
        RecordLedgerResult first = ledgerService.record(command("payment:payment-1:completed"));
        RecordLedgerResult second = ledgerService.record(command("payment:payment-2:completed"));

        assertThat(second.entryId()).isNotEqualTo(first.entryId());
        assertThat(ledgerRepository.count()).isEqualTo(2);
    }

    private RecordLedgerCommand command(String idempotencyKey) {
        return new RecordLedgerCommand(
                idempotencyKey,
                LedgerTransactionType.PAYMENT,
                idempotencyKey.contains("payment-2") ? "payment-2" : "payment-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.CREDIT,
                "강의 결제"
        );
    }
}
