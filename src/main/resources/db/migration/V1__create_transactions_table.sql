CREATE TABLE transactions (
                              id                      VARCHAR(36)     PRIMARY KEY,
                              merchant_id             VARCHAR(36)     NOT NULL,
                              customer_id             VARCHAR(36)     NOT NULL,
                              amount                  DECIMAL(19,4)   NOT NULL,
                              currency                VARCHAR(3)      NOT NULL DEFAULT 'INR',
                              status                  VARCHAR(50)     NOT NULL,
                              payment_method          VARCHAR(50)     NOT NULL,
                              gateway_transaction_id  VARCHAR(100),
                              idempotency_key         VARCHAR(255)    NOT NULL UNIQUE,
                              initiated_at            TIMESTAMP       NOT NULL,
                              updated_at              TIMESTAMP,
                              failure_reason          VARCHAR(500),

                              CONSTRAINT amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_merchant_id   ON transactions(merchant_id);
CREATE INDEX idx_status        ON transactions(status);
CREATE INDEX idx_initiated_at  ON transactions(initiated_at);