package com.hwan.lessonplatformledger.ledger.adapter;

import com.hwan.lessonplatformledger.ledger.adapter.dto.RecordLedgerEntryRequest;
import com.hwan.lessonplatformledger.ledger.adapter.dto.RecordLedgerEntryResponse;
import com.hwan.lessonplatformledger.ledger.adapter.dto.LedgerEntryResponse;
import com.hwan.lessonplatformledger.ledger.application.LedgerEntryService;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ledger-entry")
public class LedgerEntryController {

    private final LedgerEntryService ledgerEntryService;

    @GetMapping("/{entryId}")
    public LedgerEntryResponse findLedgerEntry(@PathVariable String entryId) {
        return LedgerEntryResponse.of(ledgerEntryService.findEntry(entryId));
    }

    @GetMapping("/transaction/{transactionId}")
    public List<LedgerEntryResponse> findByTransactionId(@PathVariable String transactionId) {
        return ledgerEntryService.findEntriesByTransactionId(transactionId).stream()
                .map(LedgerEntryResponse::of)
                .toList();
    }

    @GetMapping("/order/{orderId}")
    public List<LedgerEntryResponse> findByOrderId(@PathVariable String orderId) {
        return ledgerEntryService.findEntriesByOrderId(orderId).stream()
                .map(LedgerEntryResponse::of)
                .toList();
    }

    @GetMapping("/account/{accountId}")
    public List<LedgerEntryResponse> findByAccountId(@PathVariable String accountId) {
        return ledgerEntryService.findEntriesByAccountId(accountId).stream()
                .map(LedgerEntryResponse::of)
                .toList();
    }

    @PostMapping
    public RecordLedgerEntryResponse recordLedgerEntry(
        @Valid @RequestBody RecordLedgerEntryRequest request
    ) {
        RecordLedgerEntryResult recordLedgerEntryResult = ledgerEntryService.recordEntry(toCommand(request));

        return RecordLedgerEntryResponse.of(recordLedgerEntryResult);
    }

    @PostMapping("/{entryId}/adjustment")
    public RecordLedgerEntryResponse recordAdjustmentEntry(
            @PathVariable String entryId,
            @Valid @RequestBody RecordLedgerEntryRequest request
    ) {
        RecordLedgerEntryResult recordLedgerEntryResult = ledgerEntryService.recordAdjustmentEntry(
                entryId,
                toCommand(request)
        );

        return RecordLedgerEntryResponse.of(recordLedgerEntryResult);
    }

    private RecordLedgerEntryCommand toCommand(RecordLedgerEntryRequest request) {
        return new RecordLedgerEntryCommand(
                request.idempotencyKey(),
                parseTransactionType(request.transactionType()),
                request.transactionId(),
                request.orderId(),
                request.userId(),
                request.accountId(),
                request.amount(),
                request.currency(),
                parseDirection(request.direction()),
                request.description()
        );
    }

    private LedgerTransactionType parseTransactionType(String value) {
        try {
            return LedgerTransactionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidLedgerRequestException(
                    "지원하지 않는 transactionType입니다: " + value,
                    exception
            );
        }
    }

    private LedgerDirection parseDirection(String value) {
        try {
            return LedgerDirection.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidLedgerRequestException(
                    "지원하지 않는 direction입니다: " + value,
                    exception
            );
        }
    }

}
