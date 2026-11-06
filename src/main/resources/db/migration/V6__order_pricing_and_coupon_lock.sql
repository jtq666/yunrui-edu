ALTER TABLE edu_purchase_order
    ADD COLUMN original_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER original_amount,
    ADD COLUMN payable_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER discount_amount;

ALTER TABLE edu_user_coupon
    ADD KEY idx_order_status (used_order_id, status);
