package com.hwan.lessonplatformledger.ledger.application;

public class LedgerEntryNotFoundException extends RuntimeException {

    public LedgerEntryNotFoundException(String entryId) {
        super("Ledger entry를 찾을 수 없습니다: " + entryId);
    }
}
