package com.hwan.lessonplatformledger.ledger.adapter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = LedgerEntryController.class)
public class LedgerEntryControllerAdvice {

    @ExceptionHandler(InvalidLedgerRequestException.class)
    public ResponseEntity<LedgerEntryErrorResponse> handleInvalidLedgerRequest(
            InvalidLedgerRequestException exception
    ) {
        return ResponseEntity.badRequest()
                .body(new LedgerEntryErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }

    public record LedgerEntryErrorResponse(String code, String message) {
    }
}
