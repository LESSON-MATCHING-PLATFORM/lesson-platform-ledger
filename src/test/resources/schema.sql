CREATE TABLE "ledger_entries" (
    entry_id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    transaction_type VARCHAR(30) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255),
    user_id VARCHAR(255),
    account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reversed_entry_id VARCHAR(36),
    version BIGINT
);
