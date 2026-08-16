package com.hwan.lessonplatformledger.ledger.application;

import com.hwan.lessonplatformledger.ledger.adapter.LedgerEntryRepository;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryCommand;
import com.hwan.lessonplatformledger.ledger.application.dto.RecordLedgerEntryResult;
import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public RecordLedgerEntryResult recordEntry(RecordLedgerEntryCommand command) {
        try {
            LedgerEntry entry = ledgerEntryRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseGet(() -> createEntry(command));

            return RecordLedgerEntryResult.of(entry);
        } catch (DataIntegrityViolationException exception) {
            return ledgerEntryRepository.findByIdempotencyKey(command.idempotencyKey())
                    .map(RecordLedgerEntryResult::of)
                    .orElseThrow(() -> exception);
        }
    }

    @Transactional(readOnly = true)
    public LedgerEntry findEntry(String entryId) {
        return ledgerEntryRepository.findByEntryId(entryId)
                .orElseThrow(() -> new LedgerEntryNotFoundException(entryId));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findEntriesByTransactionId(String transactionId) {
        return ledgerEntryRepository.findAllByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findEntriesByOrderId(String orderId) {
        return ledgerEntryRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findEntriesByAccountId(String accountId) {
        return ledgerEntryRepository.findAllByAccountIdOrderByCreatedAtAsc(accountId);
    }

    @Transactional
    public RecordLedgerEntryResult recordAdjustmentEntry(String entryId, RecordLedgerEntryCommand command) {
        LedgerEntry entry = ledgerEntryRepository.findByEntryId(entryId)
            .orElseThrow();

        RecordLedgerEntryResult recordLedgerEntryResult = recordEntry(command);

        entry.markReversed(recordLedgerEntryResult.entryId());
        ledgerEntryRepository.save(entry);

        return recordLedgerEntryResult;
    }

    private LedgerEntry createEntry(RecordLedgerEntryCommand command) {
        LedgerEntry newLedger = LedgerEntry.recordEntry(
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

        return ledgerEntryRepository.save(newLedger);
    }

}
