CREATE TABLE edu_learning_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    position_seconds INT NOT NULL,
    duration_seconds INT NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    client_sequence BIGINT NOT NULL,
    reported_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_course_section (user_id, course_id, section_id),
    KEY idx_user_course_updated (user_id, course_id, updated_at)
);
