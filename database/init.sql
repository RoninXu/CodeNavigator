-- CodeNavigator数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS code_navigator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE code_navigator;

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

-- 插入测试数据
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

COMMIT;

-- 显示创建结果
SHOW TABLES;