CREATE TABLE edu_learning_note (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    video_second INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_note_user_course (user_id, course_id, updated_at)
);

CREATE TABLE edu_refund_request (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    handled_at DATETIME(3) NULL,
    UNIQUE KEY uk_refund_order (order_id),
    KEY idx_refund_user (user_id, created_at)
);
