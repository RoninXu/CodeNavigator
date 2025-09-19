-- ====================================================================
-- CodeNavigator 数据库完整初始化脚本
-- 包含数据库创建、表结构、性能索引和测试数据
-- ====================================================================

-- ====================================================================
-- 1. 数据库创建和基础配置
-- ====================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS code_navigator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE code_navigator;

-- ====================================================================
-- 2. 表结构创建
-- ====================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url VARCHAR(255),
    skill_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') DEFAULT 'BEGINNER',
    preferred_language VARCHAR(20) DEFAULT 'java',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习路径表
CREATE TABLE IF NOT EXISTS learning_paths (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    difficulty_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NOT NULL,
    estimated_duration INT, -- 预计完成时间（小时）
    created_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习模块表
CREATE TABLE IF NOT EXISTS learning_modules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    module_type ENUM('THEORY', 'PRACTICE', 'PROJECT', 'TUTORIAL', 'QUIZ') NOT NULL,
    order_index INT NOT NULL,
    estimated_duration INT, -- 预计完成时间（分钟）
    difficulty_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NOT NULL,
    content_url VARCHAR(255),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (path_id) REFERENCES learning_paths(id),
    INDEX idx_path_order (path_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户学习进度表
CREATE TABLE IF NOT EXISTS user_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED') DEFAULT 'NOT_STARTED',
    progress_percentage INT DEFAULT 0,
    started_time DATETIME,
    completed_time DATETIME,
    last_accessed_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (module_id) REFERENCES learning_modules(id),
    UNIQUE KEY unique_user_module (user_id, module_id),
    INDEX idx_user_progress (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码分析结果表
CREATE TABLE IF NOT EXISTS code_analysis_results (
    id VARCHAR(50) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module_id BIGINT,
    analysis_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    language VARCHAR(20) NOT NULL,
    overall_score INT NOT NULL,
    quality_level ENUM('EXCELLENT', 'GOOD', 'AVERAGE', 'POOR', 'VERY_POOR') NOT NULL,
    code_style_score INT,
    readability_score INT,
    maintainability_score INT,
    performance_score INT,
    security_score INT,
    best_practices_score INT,
    issue_count INT DEFAULT 0,
    suggestion_count INT DEFAULT 0,
    summary TEXT,
    metadata JSON,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (module_id) REFERENCES learning_modules(id),
    INDEX idx_user_analysis (user_id, analysis_time),
    INDEX idx_module_analysis (module_id, analysis_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码问题表
CREATE TABLE IF NOT EXISTS code_issues (
    id VARCHAR(50) PRIMARY KEY,
    analysis_id VARCHAR(50) NOT NULL,
    issue_type ENUM('SYNTAX_ERROR', 'STYLE_VIOLATION', 'CODE_SMELL', 'PERFORMANCE_ISSUE', 'SECURITY_VULNERABILITY', 'BEST_PRACTICE_VIOLATION') NOT NULL,
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    line_number INT,
    column_number INT,
    code_snippet TEXT,
    rule_name VARCHAR(100),
    FOREIGN KEY (analysis_id) REFERENCES code_analysis_results(id),
    INDEX idx_analysis_severity (analysis_id, severity),
    INDEX idx_type_category (issue_type, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码建议表
CREATE TABLE IF NOT EXISTS code_suggestions (
    id VARCHAR(50) PRIMARY KEY,
    analysis_id VARCHAR(50) NOT NULL,
    suggestion_type ENUM('REFACTOR', 'OPTIMIZATION', 'STYLE_IMPROVEMENT', 'BEST_PRACTICE', 'ALTERNATIVE_APPROACH') NOT NULL,
    priority ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    original_code TEXT,
    improved_code TEXT,
    code_example TEXT,
    explanation TEXT,
    difficulty_level ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
    line_number INT,
    estimated_impact INT,
    tags JSON,
    FOREIGN KEY (analysis_id) REFERENCES code_analysis_results(id),
    INDEX idx_analysis_priority (analysis_id, priority),
    INDEX idx_type_category (suggestion_type, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对话会话表
CREATE TABLE IF NOT EXISTS conversation_sessions (
    id VARCHAR(50) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    status ENUM('ACTIVE', 'COMPLETED', 'ARCHIVED') DEFAULT 'ACTIVE',
    session_type ENUM('LEARNING', 'CODE_REVIEW', 'QA', 'GUIDANCE') NOT NULL,
    context_data JSON,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ended_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_sessions (user_id, status, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对话消息表
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50) NOT NULL,
    message_type ENUM('USER', 'ASSISTANT', 'SYSTEM') NOT NULL,
    content TEXT NOT NULL,
    metadata JSON,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES conversation_sessions(id),
    INDEX idx_session_time (session_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习笔记表
CREATE TABLE IF NOT EXISTS learning_notes (
    id VARCHAR(50) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    focus_area VARCHAR(100),
    user_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NOT NULL,
    estimated_study_time INT,
    tags JSON,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_notes (user_id, created_time),
    INDEX idx_focus_area (focus_area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- 3. 性能优化索引
-- ====================================================================

-- ================================
-- 用户表性能索引
-- ================================

-- 技能等级索引
CREATE INDEX idx_users_skill_level ON users(skill_level);

-- 最后登录时间索引
CREATE INDEX idx_users_last_login_time ON users(last_login_time);

-- 首选编程语言索引
CREATE INDEX idx_users_preferred_language ON users(preferred_language);

-- 组合索引：技能等级+是否激活
CREATE INDEX idx_users_skill_active ON users(skill_level, is_active);

-- 组合索引：创建时间+是否激活
CREATE INDEX idx_users_created_active ON users(created_time, is_active);

-- ================================
-- 学习路径表性能索引
-- ================================

-- 名称索引（用于搜索）
CREATE INDEX idx_learning_paths_name ON learning_paths(name);

-- 估计时长索引
CREATE INDEX idx_learning_paths_estimated_duration ON learning_paths(estimated_duration);

-- 创建者索引
CREATE INDEX idx_learning_paths_created_by ON learning_paths(created_by);

-- 组合索引：难度+是否激活
CREATE INDEX idx_learning_paths_diff_active ON learning_paths(difficulty_level, is_active);

-- 组合索引：创建者+创建时间
CREATE INDEX idx_learning_paths_creator_time ON learning_paths(created_by, created_time);

-- ================================
-- 学习模块表性能索引
-- ================================

-- 模块类型索引
CREATE INDEX idx_learning_modules_module_type ON learning_modules(module_type);

-- 难度等级索引
CREATE INDEX idx_learning_modules_difficulty_level ON learning_modules(difficulty_level);

-- 组合索引：路径ID+是否激活
CREATE INDEX idx_learning_modules_path_active ON learning_modules(path_id, is_active);

-- ================================
-- 用户进度表性能索引
-- ================================

-- 进度百分比索引
CREATE INDEX idx_user_progress_progress_percentage ON user_progress(progress_percentage);

-- 开始时间索引
CREATE INDEX idx_user_progress_started_time ON user_progress(started_time);

-- 组合索引：模块ID+状态
CREATE INDEX idx_user_progress_module_status ON user_progress(module_id, status);

-- 组合索引：用户ID+最后访问时间
CREATE INDEX idx_user_progress_user_accessed ON user_progress(user_id, last_accessed_time);

-- ================================
-- 代码分析结果表性能索引
-- ================================

-- 编程语言索引
CREATE INDEX idx_code_analysis_results_language ON code_analysis_results(language);

-- 质量评分索引
CREATE INDEX idx_code_analysis_results_quality_score ON code_analysis_results(overall_score);

-- 组合索引：语言+质量评分
CREATE INDEX idx_code_analysis_results_lang_score ON code_analysis_results(language, overall_score);

-- ================================
-- 代码问题表性能索引
-- ================================

-- 分析结果ID索引
CREATE INDEX idx_code_issues_analysis_id ON code_issues(analysis_id);

-- 问题类型索引
CREATE INDEX idx_code_issues_issue_type ON code_issues(issue_type);

-- 严重程度索引
CREATE INDEX idx_code_issues_severity ON code_issues(severity);

-- 行号索引
CREATE INDEX idx_code_issues_line_number ON code_issues(line_number);

-- 组合索引：分析结果ID+严重程度
CREATE INDEX idx_code_issues_result_severity ON code_issues(analysis_id, severity);

-- ================================
-- 代码建议表性能索引
-- ================================

-- 分析结果ID索引
CREATE INDEX idx_code_suggestions_analysis_id ON code_suggestions(analysis_id);

-- 建议类型索引
CREATE INDEX idx_code_suggestions_suggestion_type ON code_suggestions(suggestion_type);

-- 优先级索引
CREATE INDEX idx_code_suggestions_priority ON code_suggestions(priority);

-- 组合索引：分析结果ID+优先级
CREATE INDEX idx_code_suggestions_result_priority ON code_suggestions(analysis_id, priority);

-- ================================
-- 对话会话表性能索引
-- ================================

-- 会话类型索引
CREATE INDEX idx_conversation_sessions_session_type ON conversation_sessions(session_type);

-- 状态索引
CREATE INDEX idx_conversation_sessions_status ON conversation_sessions(status);

-- 结束时间索引
CREATE INDEX idx_conversation_sessions_ended_time ON conversation_sessions(ended_time);

-- 组合索引：用户ID+状态
CREATE INDEX idx_conversation_sessions_user_status ON conversation_sessions(user_id, status);

-- ================================
-- 对话消息表性能索引
-- ================================

-- 消息类型索引
CREATE INDEX idx_conversation_messages_message_type ON conversation_messages(message_type);

-- 组合索引：会话ID+创建时间
CREATE INDEX idx_conversation_messages_session_time ON conversation_messages(session_id, created_time);

-- ================================
-- 学习笔记表性能索引
-- ================================

-- 焦点领域索引
CREATE INDEX idx_learning_notes_focus_area ON learning_notes(focus_area);

-- 用户等级索引
CREATE INDEX idx_learning_notes_user_level ON learning_notes(user_level);

-- 估计学习时间索引
CREATE INDEX idx_learning_notes_estimated_study_time ON learning_notes(estimated_study_time);

-- 组合索引：用户ID+创建时间
CREATE INDEX idx_learning_notes_user_created ON learning_notes(user_id, created_time);

-- ====================================================================
-- 4. 测试数据插入
-- ====================================================================

-- 插入测试用户
INSERT IGNORE INTO users (username, email, password, full_name, skill_level) VALUES
('testuser', 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFObbZh/8XfckFa7hFcNHK6', 'Test User', 'INTERMEDIATE'),
('admin', 'admin@codenavigator.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFObbZh/8XfckFa7hFcNHK6', 'Administrator', 'ADVANCED');

-- 插入示例学习路径
INSERT IGNORE INTO learning_paths (id, name, description, difficulty_level, estimated_duration, created_by) VALUES
(1, 'Java基础入门', 'Java编程语言基础知识学习路径', 'BEGINNER', 40, 2),
(2, 'Spring框架进阶', 'Spring框架深入学习和实践', 'INTERMEDIATE', 60, 2),
(3, '微服务架构实战', '基于Spring Cloud的微服务架构设计与实现', 'ADVANCED', 80, 2);

-- 插入示例学习模块
INSERT IGNORE INTO learning_modules (path_id, name, description, module_type, order_index, estimated_duration, difficulty_level) VALUES
(1, 'Java语法基础', 'Java基本语法、数据类型、控制结构', 'THEORY', 1, 120, 'BEGINNER'),
(1, '面向对象编程', '类、对象、继承、多态、封装', 'THEORY', 2, 150, 'BEGINNER'),
(1, '集合框架', 'List、Set、Map等集合类的使用', 'PRACTICE', 3, 180, 'BEGINNER'),
(1, '异常处理', 'try-catch、自定义异常', 'PRACTICE', 4, 90, 'BEGINNER'),
(2, 'Spring Core', 'IoC容器、依赖注入、AOP', 'THEORY', 1, 200, 'INTERMEDIATE'),
(2, 'Spring MVC', 'Web开发、REST API设计', 'PRACTICE', 2, 240, 'INTERMEDIATE'),
(2, 'Spring Data', '数据访问层、JPA、事务管理', 'PRACTICE', 3, 180, 'INTERMEDIATE');

-- ====================================================================
-- 5. 数据库维护和监控
-- ====================================================================

-- 创建查询性能分析视图
CREATE OR REPLACE VIEW v_query_performance AS
SELECT
    table_name,
    index_name,
    cardinality,
    pages,
    filtered
FROM information_schema.statistics
WHERE table_schema = DATABASE()
ORDER BY table_name, seq_in_index;

-- 定期优化表的存储过程
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS OptimizeTables()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE table_name VARCHAR(255);
    DECLARE cur CURSOR FOR
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = DATABASE()
        AND t.table_type = 'BASE TABLE';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO table_name;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET @sql = CONCAT('OPTIMIZE TABLE ', table_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- ====================================================================
-- 6. 提交事务
-- ====================================================================

COMMIT;

-- ====================================================================
-- 7. 验证数据库创建结果
-- ====================================================================

-- 显示所有表
SHOW TABLES;

-- 显示表结构统计
SELECT
    TABLE_NAME as '表名',
    ENGINE as '引擎',
    TABLE_ROWS as '行数',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'code_navigator'
AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- 显示所有索引
SELECT
    TABLE_NAME as '表名',
    INDEX_NAME as '索引名',
    COLUMN_NAME as '字段名',
    CARDINALITY as '基数',
    INDEX_TYPE as '索引类型',
    CASE NON_UNIQUE WHEN 0 THEN '唯一' ELSE '非唯一' END as '唯一性'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'code_navigator'
AND INDEX_NAME != 'PRIMARY'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ====================================================================
-- 初始化完成
-- ====================================================================

SELECT 'CodeNavigator数据库初始化完成' as '状态';