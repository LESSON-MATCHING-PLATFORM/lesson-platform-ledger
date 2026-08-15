package com.hwan.lessonplatformledger.ledger.application;

import com.hwan.lessonplatformledger.ledger.adapter.LedgerRepository;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    public RecordLedgerResult record(RecordLedgerCommand command) {
        LedgerEntry entry = ledgerRepository.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> createEntry(command));

        return RecordLedgerResult.of(entry);
    }

    private LedgerEntry createEntry(RecordLedgerCommand command) {
        LedgerEntry newLedger = LedgerEntry.create(
                UUID.randomUUID().toString(),
                command.idempotencyKey(),
                command.transactionType(),
                command.transactionId(),
                command.orderId(),
                command.userId(),
                command.accountId(),
                command.amount(),
                command.currency(),
                command.direction(),
                command.description(),
                Instant.now()
        );

        return ledgerRepository.save(newLedger);
    }

}
