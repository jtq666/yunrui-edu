CREATE TABLE edu_points_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    points INT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    season CHAR(6) NOT NULL,
    ranked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_event (event_id),
    KEY idx_rank_pending (ranked, season, id)
);

CREATE TABLE edu_points_board_archive (
    season CHAR(6) NOT NULL,
    user_id BIGINT NOT NULL,
    points INT NOT NULL,
    rank_no INT NOT NULL,
    archived_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (season, user_id),
    KEY idx_season_rank (season, rank_no)
);
