package com.hwan.lessonplatformledger.ledger.adapter;

public class InvalidLedgerRequestException extends RuntimeException {

    public InvalidLedgerRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
