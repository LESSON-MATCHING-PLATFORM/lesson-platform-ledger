package com.hwan.lessonplatformledger.ledger.adapter;

import com.hwan.lessonplatformledger.ledger.application.LedgerEntryService;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerStatus;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerEntryController.class)
@DisplayName("Ledger Entry Controller 테스트")
class LedgerEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerEntryService ledgerEntryService;

    @Test
    @DisplayName("원장 기록 요청을 서비스에 위임하고 응답을 반환한다")
    void recordsLedgerEntry() throws Exception {
        RecordLedgerEntryResult result = new RecordLedgerEntryResult(
                "ledger-1",
                "payment-1",
                LedgerTransactionType.PAYMENT,
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.CREDIT,
                LedgerStatus.POSTED,
                Instant.parse("2026-08-15T00:00:00Z")
        );
        when(ledgerEntryService.recordEntry(any(RecordLedgerEntryCommand.class)))
                .thenReturn(result);

        mockMvc.perform(post("/ledger-entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "payment:payment-1:completed",
                                  "transactionType": "PAYMENT",
                                  "transactionId": "payment-1",
                                  "orderId": "order-1",
                                  "userId": "user-1",
                                  "accountId": "seller-1",
                                  "amount": 80000,
                                  "currency": "KRW",
                                  "direction": "CREDIT",
                                  "description": "강의 결제"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value("ledger-1"))
                .andExpect(jsonPath("$.transactionType").value("PAYMENT"))
                .andExpect(jsonPath("$.amount").value(80000))
                .andExpect(jsonPath("$.direction").value("CREDIT"))
                .andExpect(jsonPath("$.status").value("POSTED"));

        ArgumentCaptor<RecordLedgerEntryCommand> captor =
                ArgumentCaptor.forClass(RecordLedgerEntryCommand.class);
        verify(ledgerEntryService).recordEntry(captor.capture());

        RecordLedgerEntryCommand command = captor.getValue();
        assertThat(command.idempotencyKey()).isEqualTo("payment:payment-1:completed");
        assertThat(command.transactionType()).isEqualTo(LedgerTransactionType.PAYMENT);
        assertThat(command.direction()).isEqualTo(LedgerDirection.CREDIT);
        assertThat(command.amount()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("보정 원장 요청을 원본 entryId와 함께 서비스에 위임한다")
    void recordsAdjustmentEntry() throws Exception {
        RecordLedgerEntryResult result = new RecordLedgerEntryResult(
                "ledger-adjustment-1",
                "refund-1",
                LedgerTransactionType.REFUND,
                new BigDecimal("80000"),
                "KRW",
                LedgerDirection.DEBIT,
                LedgerStatus.POSTED,
                Instant.parse("2026-08-15T00:01:00Z")
        );
        when(ledgerEntryService.recordAdjustmentEntry(
                eq("ledger-1"), any(RecordLedgerEntryCommand.class)))
                .thenReturn(result);

        mockMvc.perform(post("/ledger-entry/ledger-1/adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJsonWithDetails(
                                "refund:payment-1:completed",
                                "REFUND",
                                "refund-1",
                                "DEBIT",
                                "80000"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value("ledger-adjustment-1"))
                .andExpect(jsonPath("$.transactionType").value("REFUND"))
                .andExpect(jsonPath("$.direction").value("DEBIT"));

        ArgumentCaptor<RecordLedgerEntryCommand> captor =
                ArgumentCaptor.forClass(RecordLedgerEntryCommand.class);
        verify(ledgerEntryService).recordAdjustmentEntry(eq("ledger-1"), captor.capture());

        assertThat(captor.getValue().transactionType()).isEqualTo(LedgerTransactionType.REFUND);
        assertThat(captor.getValue().direction()).isEqualTo(LedgerDirection.DEBIT);
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("refund:payment-1:completed");
    }

    @Test
    @DisplayName("멱등성 키가 비어 있으면 400 응답을 반환하고 서비스를 호출하지 않는다")
    void rejectsBlankIdempotencyKey() throws Exception {
        mockMvc.perform(post("/ledger-entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("", "80000")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ledgerEntryService);
    }

    @Test
    @DisplayName("금액이 0 이하이면 400 응답을 반환하고 서비스를 호출하지 않는다")
    void rejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/ledger-entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("payment:payment-1:completed", "0")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ledgerEntryService);
    }

    @Test
    @DisplayName("지원하지 않는 transactionType enum 값이면 400 응답을 반환하고 서비스를 호출하지 않는다")
    void rejectsInvalidTransactionType() throws Exception {
        mockMvc.perform(post("/ledger-entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "payment:payment-1:completed",
                                  "transactionType": "INVALID_TRANSACTION_TYPE",
                                  "transactionId": "payment-1",
                                  "orderId": "order-1",
                                  "userId": "user-1",
                                  "accountId": "seller-1",
                                  "amount": 80000,
                                  "currency": "KRW",
                                  "direction": "CREDIT",
                                  "description": "강의 결제"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ledgerEntryService);
    }

    @Test
    @DisplayName("entryId로 원장을 조회한다")
    void findsLedgerEntry() throws Exception {
        when(ledgerEntryService.findEntry("ledger-1")).thenReturn(entry());

        mockMvc.perform(get("/ledger-entry/ledger-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value("ledger-1"))
                .andExpect(jsonPath("$.idempotencyKey").value("payment:payment-1:completed"))
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.accountId").value("seller-1"))
                .andExpect(jsonPath("$.version").value(0));

        verify(ledgerEntryService).findEntry("ledger-1");
    }

    @Test
    @DisplayName("거래 식별자로 원장 목록을 조회한다")
    void findsLedgerEntriesByTransactionId() throws Exception {
        when(ledgerEntryService.findEntriesByTransactionId("payment-1")).thenReturn(java.util.List.of(entry()));

        mockMvc.perform(get("/ledger-entry/transaction/payment-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryId").value("ledger-1"))
                .andExpect(jsonPath("$[0].transactionId").value("payment-1"));
    }

    @Test
    @DisplayName("없는 entryId를 조회하면 404 응답을 반환한다")
    void returnsNotFoundWhenLedgerEntryDoesNotExist() throws Exception {
        when(ledgerEntryService.findEntry("missing-entry"))
                .thenThrow(new com.hwan.lessonplatformledger.ledger.application.LedgerEntryNotFoundException("missing-entry"));

        mockMvc.perform(get("/ledger-entry/missing-entry"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEDGER_ENTRY_NOT_FOUND"));
    }

    private com.hwan.lessonplatformledger.ledger.domain.LedgerEntry entry() {
        return new com.hwan.lessonplatformledger.ledger.domain.LedgerEntry(
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
                LedgerStatus.POSTED,
                "강의 결제",
                Instant.parse("2026-08-15T00:00:00Z"),
                null,
                0L
        );
    }

    private String validRequestJson(String idempotencyKey, String amount) {
        return validRequestJsonWithDetails(idempotencyKey, "PAYMENT", "payment-1", "CREDIT", amount);
    }

    private String validRequestJsonWithDetails(
            String idempotencyKey,
            String transactionType,
            String transactionId,
            String direction,
            String amount
    ) {
        return """
                {
                  "idempotencyKey": "%s",
                  "transactionType": "%s",
                  "transactionId": "%s",
                  "orderId": "order-1",
                  "userId": "user-1",
                  "accountId": "seller-1",
                  "amount": %s,
                  "currency": "KRW",
                  "direction": "%s",
                  "description": "강의 결제"
                }
                """.formatted(idempotencyKey, transactionType, transactionId, amount, direction);
    }
}
