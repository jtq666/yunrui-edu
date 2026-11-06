CREATE TABLE edu_purchase_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_transaction_id VARCHAR(128),
    paid_at DATETIME(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_payment_transaction (payment_transaction_id),
    KEY idx_user_created (user_id, created_at)
);

CREATE TABLE edu_outbox_message (
    id CHAR(36) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    last_error VARCHAR(500),
    created_at DATETIME(3) NOT NULL,
    published_at DATETIME(3),
    KEY idx_publishable (status, next_retry_at)
);

CREATE TABLE edu_course_entitlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    source_event_id CHAR(36) NOT NULL,
    granted_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_order (order_id),
    UNIQUE KEY uk_source_event (source_event_id),
    KEY idx_user_course (user_id, course_id)
);
