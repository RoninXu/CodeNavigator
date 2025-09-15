-- CodeNavigator 数据库性能优化索引 - MySQL 8.0兼容版本
-- 用于提升查询性能和数据库操作效率

-- 创建数据库如果不存在
CREATE DATABASE IF NOT EXISTS code_navigator 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE code_navigator;

-- ================================
-- 用户表索引
-- ================================

-- 检查并创建用户名索引
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- 检查并创建邮箱索引  
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- 用户等级索引
CREATE INDEX idx_users_level ON users(level);

-- 创建时间索引
CREATE INDEX idx_users_created_at ON users(created_at);

-- 组合索引：等级+创建时间
CREATE INDEX idx_users_level_created_at ON users(level, created_at);

-- ================================
-- 学习路径表索引
-- ================================

-- 框架索引
CREATE INDEX idx_learning_paths_framework ON learning_paths(framework);

-- 难度等级索引
CREATE INDEX idx_learning_paths_difficulty ON learning_paths(difficulty);

-- 目标用户等级索引
CREATE INDEX idx_learning_paths_target_level ON learning_paths(target_level);

-- 是否活跃索引
CREATE INDEX idx_learning_paths_is_active ON learning_paths(is_active);

-- 完成数索引
CREATE INDEX idx_learning_paths_completion_count ON learning_paths(completion_count DESC);

-- 平均评分索引
CREATE INDEX idx_learning_paths_average_rating ON learning_paths(average_rating DESC);

-- 创建时间索引
CREATE INDEX idx_learning_paths_created_at ON learning_paths(created_at DESC);

-- 组合索引：活跃状态+完成数
CREATE INDEX idx_learning_paths_active_completion 
ON learning_paths(is_active, completion_count DESC);

-- 组合索引：框架+活跃状态+评分
CREATE INDEX idx_learning_paths_framework_active_rating 
ON learning_paths(framework, is_active, average_rating DESC);

-- 标签索引
CREATE INDEX idx_learning_paths_tags ON learning_paths(tags);

-- ================================
-- 学习模块表索引
-- ================================

-- 学习路径ID索引
CREATE INDEX idx_learning_modules_path_id ON learning_modules(learning_path_id);

-- 排序索引
CREATE INDEX idx_learning_modules_order_index ON learning_modules(order_index);

-- 组合索引：路径ID+排序
CREATE INDEX idx_learning_modules_path_order 
ON learning_modules(learning_path_id, order_index);

-- 模块类型索引
CREATE INDEX idx_learning_modules_type ON learning_modules(type);

-- 是否必需索引
CREATE INDEX idx_learning_modules_required ON learning_modules(is_required);

-- ================================
-- 用户进度表索引
-- ================================

-- 用户ID索引
CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);

-- 学习路径ID索引
CREATE INDEX idx_user_progress_learning_path_id ON user_progress(learning_path_id);

-- 当前模块ID索引
CREATE INDEX idx_user_progress_current_module_id ON user_progress(current_module_id);

-- 状态索引
CREATE INDEX idx_user_progress_status ON user_progress(status);

-- 完成百分比索引
CREATE INDEX idx_user_progress_completion_percentage ON user_progress(completion_percentage);

-- 最后活跃时间索引
CREATE INDEX idx_user_progress_last_active_at ON user_progress(last_active_at DESC);

-- 开始时间索引
CREATE INDEX idx_user_progress_started_at ON user_progress(started_at);

-- 完成时间索引
CREATE INDEX idx_user_progress_completed_at ON user_progress(completed_at DESC);

-- 组合索引：用户ID+状态
CREATE INDEX idx_user_progress_user_status 
ON user_progress(user_id, status);

-- 组合索引：路径ID+状态
CREATE INDEX idx_user_progress_path_status 
ON user_progress(learning_path_id, status);

-- 组合索引：用户ID+最后活跃时间
CREATE INDEX idx_user_progress_user_active 
ON user_progress(user_id, last_active_at DESC);

-- 唯一组合索引：用户ID+路径ID
CREATE UNIQUE INDEX idx_user_progress_user_path_unique 
ON user_progress(user_id, learning_path_id);

-- ================================
-- 代码分析结果表索引
-- ================================

-- 用户ID索引
CREATE INDEX idx_code_analysis_results_user_id ON code_analysis_results(user_id);

-- 分析类型索引
CREATE INDEX idx_code_analysis_results_analysis_type ON code_analysis_results(analysis_type);

-- 编程语言索引
CREATE INDEX idx_code_analysis_results_language ON code_analysis_results(language);

-- 质量评分索引
CREATE INDEX idx_code_analysis_results_quality_score ON code_analysis_results(quality_score);

-- 创建时间索引
CREATE INDEX idx_code_analysis_results_created_at ON code_analysis_results(created_at DESC);

-- 组合索引：用户ID+创建时间
CREATE INDEX idx_code_analysis_results_user_created 
ON code_analysis_results(user_id, created_at DESC);

-- ================================
-- 对话会话表索引
-- ================================

-- 用户ID索引
CREATE INDEX idx_conversation_sessions_user_id ON conversation_sessions(user_id);

-- 会话类型索引
CREATE INDEX idx_conversation_sessions_type ON conversation_sessions(type);

-- 状态索引
CREATE INDEX idx_conversation_sessions_status ON conversation_sessions(status);

-- 创建时间索引
CREATE INDEX idx_conversation_sessions_created_at ON conversation_sessions(created_at DESC);

-- 最后消息时间索引
CREATE INDEX idx_conversation_sessions_last_message_at ON conversation_sessions(last_message_at DESC);

-- 组合索引：用户ID+状态
CREATE INDEX idx_conversation_sessions_user_status 
ON conversation_sessions(user_id, status);

-- ================================
-- 对话消息表索引
-- ================================

-- 会话ID索引
CREATE INDEX idx_conversation_messages_session_id ON conversation_messages(session_id);

-- 发送者类型索引
CREATE INDEX idx_conversation_messages_sender_type ON conversation_messages(sender_type);

-- 创建时间索引
CREATE INDEX idx_conversation_messages_created_at ON conversation_messages(created_at);

-- 组合索引：会话ID+创建时间
CREATE INDEX idx_conversation_messages_session_created 
ON conversation_messages(session_id, created_at);

-- ================================
-- 学习笔记表索引
-- ================================

-- 用户ID索引
CREATE INDEX idx_learning_notes_user_id ON learning_notes(user_id);

-- 学习路径ID索引
CREATE INDEX idx_learning_notes_learning_path_id ON learning_notes(learning_path_id);

-- 模块ID索引
CREATE INDEX idx_learning_notes_module_id ON learning_notes(module_id);

-- 笔记类型索引
CREATE INDEX idx_learning_notes_note_type ON learning_notes(note_type);

-- 创建时间索引
CREATE INDEX idx_learning_notes_created_at ON learning_notes(created_at DESC);

-- 更新时间索引
CREATE INDEX idx_learning_notes_updated_at ON learning_notes(updated_at DESC);

-- 组合索引：用户ID+路径ID
CREATE INDEX idx_learning_notes_user_path 
ON learning_notes(user_id, learning_path_id);

-- ================================
-- 检查索引创建结果
-- ================================

-- 显示所有表的索引信息
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'code_navigator'
AND TABLE_NAME IN (
    'users', 'learning_paths', 'learning_modules', 
    'user_progress', 'code_analysis_results', 
    'conversation_sessions', 'conversation_messages', 
    'learning_notes'
)
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 显示表的存储引擎和字符集信息
SELECT 
    TABLE_NAME,
    ENGINE,
    TABLE_COLLATION,
    TABLE_ROWS,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'Size(MB)'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'code_navigator'
ORDER BY TABLE_NAME;