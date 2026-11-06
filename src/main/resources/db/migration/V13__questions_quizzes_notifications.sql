CREATE TABLE edu_course_question (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,course_id BIGINT NOT NULL,section_id BIGINT NOT NULL,
 content VARCHAR(1000) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'OPEN',answer VARCHAR(1000) NULL,
 answered_by BIGINT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),answered_at DATETIME(3) NULL,
 KEY idx_question_course(course_id,created_at),KEY idx_question_user(user_id,created_at)
);
CREATE TABLE edu_quiz_question (
 id BIGINT PRIMARY KEY,section_id BIGINT NOT NULL,question VARCHAR(500) NOT NULL,
 option_a VARCHAR(300) NOT NULL,option_b VARCHAR(300) NOT NULL,option_c VARCHAR(300) NOT NULL,option_d VARCHAR(300) NOT NULL,
 correct_option CHAR(1) NOT NULL,explanation VARCHAR(1000) NOT NULL,KEY idx_quiz_section(section_id)
);
CREATE TABLE edu_quiz_attempt (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,question_id BIGINT NOT NULL,selected_option CHAR(1) NOT NULL,
 correct TINYINT(1) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),KEY idx_attempt_user(user_id,created_at)
);
CREATE TABLE edu_notification (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,title VARCHAR(200) NOT NULL,content VARCHAR(1000) NOT NULL,
 read_flag TINYINT(1) NOT NULL DEFAULT 0,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_notification_user(user_id,read_flag,created_at)
);
INSERT INTO edu_quiz_question(id,section_id,question,option_a,option_b,option_c,option_d,correct_option,explanation) VALUES
(4101,2101,'多个线程同时修改共享数据时，最核心的风险是什么？','数据竞争','网络延迟','磁盘不足','编译失败','A','多个执行单元同时读写共享状态，可能产生数据竞争与结果覆盖。'),
(4102,2102,'Redis中适合把“判断库存并扣减”合并成原子操作的方式是？','定时任务','Lua脚本','增加日志','创建新数据库','B','Lua脚本在Redis内一次执行，可避免检查和扣减之间被其他请求插入。'),
(4201,2201,'大模型能够调用外部工具的关键是什么？','工具参数结构和调用协议','提高屏幕亮度','增加数据库表','关闭网络','A','模型根据工具说明与参数结构生成调用请求，应用负责执行并返回结果。');
