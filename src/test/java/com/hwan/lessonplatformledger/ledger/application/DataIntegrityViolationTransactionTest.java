package com.hwan.lessonplatformledger.ledger.application;

import com.hwan.lessonplatformledger.ledger.adapter.LedgerEntryRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("DataIntegrityViolationException 트랜잭션 테스트")
class DataIntegrityViolationTransactionTest {

    private static final String IDEMPOTENCY_KEY = "payment:payment-1:completed";

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        ledgerEntryRepository.save(entry("ledger-1", IDEMPOTENCY_KEY));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("유니크 충돌을 같은 트랜잭션에서 catch하면 커밋 시 롤백된다")
    void rollsBackWhenDataIntegrityViolationIsCaughtInsideSameTransaction() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(definition);

        try {
            ledgerEntryRepository.save(entry("ledger-duplicate", IDEMPOTENCY_KEY));
        } catch (DataIntegrityViolationException exception) {
            // 예외를 catch해도 이미 rollback-only로 표시된 트랜잭션은 정상 커밋할 수 없다.
            assertThat(ledgerEntryRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).isPresent();
            status.setRollbackOnly();
        }

        assertThatCode(() -> transactionManager.commit(status))
                .doesNotThrowAnyException();

        assertThat(ledgerEntryRepository.count()).isEqualTo(1);
    }

    private static LedgerEntry entry(String entryId, String idempotencyKey) {
        return new LedgerEntry(
                entryId,
                idempotencyKey,
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
                null
        );
    }
}
