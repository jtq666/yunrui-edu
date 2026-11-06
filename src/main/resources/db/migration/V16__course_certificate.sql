CREATE TABLE edu_course_certificate (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,course_id BIGINT NOT NULL,certificate_code VARCHAR(40) NOT NULL,
 issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),UNIQUE KEY uk_certificate_user_course(user_id,course_id),
 UNIQUE KEY uk_certificate_code(certificate_code)
);
