CREATE TABLE edu_course (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(64) NOT NULL,
    teacher_name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    description TEXT,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_status_category (status, category)
);

CREATE TABLE edu_course_section (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    sort_no INT NOT NULL,
    duration_seconds INT NOT NULL,
    UNIQUE KEY uk_course_sort (course_id, sort_no),
    KEY idx_course (course_id)
);

INSERT INTO edu_course(id, title, category, teacher_name, price, status, description)
VALUES
    (2001, 'Java高并发与系统设计', '后端开发', '陈老师', 299.00, 'PUBLISHED', '从交易、缓存和消息可靠性理解高并发系统'),
    (2002, '大模型Agent应用开发', '人工智能', '林老师', 399.00, 'PUBLISHED', '学习Tool Calling、Agent Loop与RAG工程实践');

INSERT INTO edu_course_section(id, course_id, title, sort_no, duration_seconds)
VALUES
    (2101, 2001, '并发问题从哪里产生', 1, 1200),
    (2102, 2001, 'Redis原子操作与分布式锁', 2, 1800),
    (2201, 2002, '从普通对话到工具调用', 1, 1500),
    (2202, 2002, '实现可靠的Agent循环', 2, 2100);
