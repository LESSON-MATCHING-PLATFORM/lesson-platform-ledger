package com.hwan.lessonplatformledger.ledger.adapter;

import com.hwan.lessonplatformledger.ledger.domain.LedgerEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerRepository extends CrudRepository<LedgerEntry, String> {

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);
}
