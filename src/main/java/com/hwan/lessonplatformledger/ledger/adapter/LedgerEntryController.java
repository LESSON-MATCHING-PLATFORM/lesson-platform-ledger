package com.hwan.lessonplatformledger.ledger.adapter;

import com.hwan.lessonplatformledger.ledger.adapter.dto.RecordLedgerEntryRequest;
import com.hwan.lessonplatformledger.ledger.adapter.dto.RecordLedgerEntryResponse;
import com.hwan.lessonplatformledger.ledger.application.LedgerEntryService;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerDirection;
import com.hwan.lessonplatformledger.ledger.domain.LedgerTransactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ledger-entry")
public class LedgerEntryController {

    private final LedgerEntryService ledgerEntryService;

    @PostMapping
    public RecordLedgerEntryResponse recordLedgerEntry(
        @Valid @RequestBody RecordLedgerEntryRequest request
    ) {
        RecordLedgerEntryResult recordLedgerEntryResult = ledgerEntryService.recordEntry(
            new RecordLedgerEntryCommand(
                request.idempotencyKey(),
                LedgerTransactionType.valueOf(request.transactionType()),
                request.transactionId(),
                request.orderId(),
                request.userId(),
                request.accountId(),
                request.amount(),
                request.currency(),
                LedgerDirection.valueOf(request.direction()),
                request.description()
            )
        );

        return RecordLedgerEntryResponse.of(recordLedgerEntryResult);
    }

}
