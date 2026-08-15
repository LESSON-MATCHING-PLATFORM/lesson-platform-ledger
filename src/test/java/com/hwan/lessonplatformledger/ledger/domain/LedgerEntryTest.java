package com.hwan.lessonplatformledger.ledger.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@DisplayName("Ledger 원장 도메인 테스트")
class LedgerEntryTest {

    @Test
    @DisplayName("원장을 POSTED 상태로 생성한다")
    void createsPostedEntry() {
        LedgerEntry entry = createEntry();

        assertThat(entry.getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(entry.getAmount()).isEqualByComparingTo("80000");
        assertThat(entry.getReversedEntryId()).isNull();
    }

    @Test
    @DisplayName("원장을 reversal하고 원본 금액은 유지한다")
    void marksEntryAsReversedWithoutChangingOriginalAmount() {
        LedgerEntry entry = createEntry();

        entry.markReversed("ledger-reversal-1");

        assertThat(entry.getStatus()).isEqualTo(LedgerStatus.REVERSED);
        assertThat(entry.getReversedEntryId()).isEqualTo("ledger-reversal-1");
        assertThat(entry.getAmount()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("이미 reversal된 원장은 다시 reversal할 수 없다")
    void rejectsSecondReversal() {
        LedgerEntry entry = createEntry();
        entry.markReversed("ledger-reversal-1");

        assertThatIllegalStateException()
                .isThrownBy(() -> entry.markReversed("ledger-reversal-2"));
    }

    @Test
    @DisplayName("0 이하의 금액을 가진 원장은 생성할 수 없다")
    void rejectsNonPositiveAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LedgerEntry.create(
                        "ledger-1",
                        "payment:payment-1:completed",
                        LedgerTransactionType.PAYMENT,
                        "payment-1",
                        "order-1",
                        "user-1",
                        "seller-1",
                        BigDecimal.ZERO,
                        "KRW",
                        LedgerDirection.CREDIT,
                        "강의 결제",
                        Instant.parse("2026-08-15T00:00:00Z")
                ));
    }

    private LedgerEntry createEntry() {
        return LedgerEntry.create(
                "ledger-1",
                "payment:payment-1:completed",
                LedgerTransactionType.PAYMENT,
                "payment-1",
                "order-1",
                "user-1",
                "seller-1",
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.CREDIT,
                "강의 결제",
                Instant.parse("2026-08-15T00:00:00Z")
        );
    }
}
