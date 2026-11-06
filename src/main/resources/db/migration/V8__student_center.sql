ALTER TABLE edu_user_account
    ADD COLUMN email VARCHAR(120) NULL,
    ADD COLUMN bio VARCHAR(300) NULL;

CREATE TABLE edu_cart_item (
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, course_id),
    KEY idx_cart_created (user_id, created_at)
);

CREATE TABLE edu_course_favorite (
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, course_id),
    KEY idx_favorite_created (user_id, created_at)
);
