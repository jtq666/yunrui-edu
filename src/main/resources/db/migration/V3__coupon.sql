CREATE TABLE edu_coupon (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    threshold_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    total_quantity INT NOT NULL,
    claimed_quantity INT NOT NULL DEFAULT 0,
    valid_from DATETIME(3) NOT NULL,
    valid_until DATETIME(3) NOT NULL,
    stackable TINYINT(1) NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CHECK (threshold_amount >= 0),
    CHECK (discount_amount > 0),
    CHECK (claimed_quantity <= total_quantity)
);

CREATE TABLE edu_user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    claimed_at DATETIME(3) NOT NULL,
    used_order_id BIGINT,
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    KEY idx_user_status (user_id, status)
);

INSERT INTO edu_coupon(
    id, name, threshold_amount, discount_amount, total_quantity,
    valid_from, valid_until, stackable)
VALUES
    (3001, '满100减20', 100.00, 20.00, 1000, CURRENT_TIMESTAMP(3), DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 365 DAY), 1),
    (3002, '满200减50', 200.00, 50.00, 500, CURRENT_TIMESTAMP(3), DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 365 DAY), 1),
    (3003, '新人立减15', 0.00, 15.00, 1000, CURRENT_TIMESTAMP(3), DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 365 DAY), 0);
