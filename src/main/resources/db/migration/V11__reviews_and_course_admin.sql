ALTER TABLE edu_user_account ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STUDENT';
UPDATE edu_user_account SET role='ADMIN' WHERE username='student001';

CREATE TABLE edu_course_review (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_review_user_course (user_id, course_id),
    KEY idx_review_course (course_id, created_at)
);
