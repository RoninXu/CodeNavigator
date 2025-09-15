-- CodeNavigator 数据库性能优化索引
-- 用于提升查询性能和数据库操作效率

-- ================================
-- 用户表索引
-- ================================

-- 用户名唯一索引（已在实体中定义，这里作为备份）
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 邮箱唯一索引（已在实体中定义，这里作为备份）
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 用户等级索引（用于按等级查询用户）
CREATE INDEX IF NOT EXISTS idx_users_level ON users(level);

-- 创建时间索引（用于查询最新注册用户）
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 组合索引：等级+创建时间（优化复合查询）
CREATE INDEX IF NOT EXISTS idx_users_level_created_at ON users(level, created_at);

-- ================================
-- 学习路径表索引
-- ================================

-- 框架索引（用于按技术框架过滤）
CREATE INDEX IF NOT EXISTS idx_learning_paths_framework ON learning_paths(framework);

-- 难度等级索引
CREATE INDEX IF NOT EXISTS idx_learning_paths_difficulty ON learning_paths(difficulty);

-- 目标用户等级索引
CREATE INDEX IF NOT EXISTS idx_learning_paths_target_level ON learning_paths(target_level);

-- 是否活跃索引
CREATE INDEX IF NOT EXISTS idx_learning_paths_is_active ON learning_paths(is_active);

-- 完成数索引（用于排序热门路径）
CREATE INDEX IF NOT EXISTS idx_learning_paths_completion_count ON learning_paths(completion_count DESC);

-- 平均评分索引
CREATE INDEX IF NOT EXISTS idx_learning_paths_average_rating ON learning_paths(average_rating DESC);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_learning_paths_created_at ON learning_paths(created_at DESC);

-- 组合索引：活跃状态+完成数（优化热门路径查询）
CREATE INDEX IF NOT EXISTS idx_learning_paths_active_completion 
ON learning_paths(is_active, completion_count DESC);

-- 组合索引：框架+活跃状态+评分（优化按框架查询）
CREATE INDEX IF NOT EXISTS idx_learning_paths_framework_active_rating 
ON learning_paths(framework, is_active, average_rating DESC);

-- 全文搜索索引（标题和描述）
CREATE FULLTEXT INDEX IF NOT EXISTS idx_learning_paths_search 
ON learning_paths(title, description);

-- 标签索引（用于标签搜索）
CREATE INDEX IF NOT EXISTS idx_learning_paths_tags ON learning_paths(tags);

-- ================================
-- 学习模块表索引
-- ================================

-- 学习路径ID索引
CREATE INDEX IF NOT EXISTS idx_learning_modules_path_id ON learning_modules(learning_path_id);

-- 排序索引
CREATE INDEX IF NOT EXISTS idx_learning_modules_order_index ON learning_modules(order_index);

-- 组合索引：路径ID+排序（优化模块列表查询）
CREATE INDEX IF NOT EXISTS idx_learning_modules_path_order 
ON learning_modules(learning_path_id, order_index);

-- 模块类型索引
CREATE INDEX IF NOT EXISTS idx_learning_modules_type ON learning_modules(type);

-- 是否必需索引
CREATE INDEX IF NOT EXISTS idx_learning_modules_required ON learning_modules(is_required);

-- ================================
-- 用户进度表索引
-- ================================

-- 用户ID索引
CREATE INDEX IF NOT EXISTS idx_user_progress_user_id ON user_progress(user_id);

-- 学习路径ID索引
CREATE INDEX IF NOT EXISTS idx_user_progress_learning_path_id ON user_progress(learning_path_id);

-- 当前模块ID索引
CREATE INDEX IF NOT EXISTS idx_user_progress_current_module_id ON user_progress(current_module_id);

-- 状态索引
CREATE INDEX IF NOT EXISTS idx_user_progress_status ON user_progress(status);

-- 完成百分比索引
CREATE INDEX IF NOT EXISTS idx_user_progress_completion_percentage ON user_progress(completion_percentage);

-- 最后活跃时间索引（用于查找活跃用户）
CREATE INDEX IF NOT EXISTS idx_user_progress_last_active_at ON user_progress(last_active_at DESC);

-- 开始时间索引
CREATE INDEX IF NOT EXISTS idx_user_progress_started_at ON user_progress(started_at);

-- 完成时间索引
CREATE INDEX IF NOT EXISTS idx_user_progress_completed_at ON user_progress(completed_at DESC);

-- 组合索引：用户ID+状态（优化用户进度查询）
CREATE INDEX IF NOT EXISTS idx_user_progress_user_status 
ON user_progress(user_id, status);

-- 组合索引：路径ID+状态（优化路径统计查询）
CREATE INDEX IF NOT EXISTS idx_user_progress_path_status 
ON user_progress(learning_path_id, status);

-- 组合索引：用户ID+最后活跃时间（优化用户活动查询）
CREATE INDEX IF NOT EXISTS idx_user_progress_user_active 
ON user_progress(user_id, last_active_at DESC);

-- 唯一组合索引：用户ID+路径ID（防止重复记录）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_progress_user_path_unique 
ON user_progress(user_id, learning_path_id);

-- ================================
-- 代码分析结果表索引
-- ================================

-- 用户ID索引
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_user_id ON code_analysis_results(user_id);

-- 分析类型索引
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_analysis_type ON code_analysis_results(analysis_type);

-- 编程语言索引
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_language ON code_analysis_results(language);

-- 质量评分索引
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_quality_score ON code_analysis_results(quality_score);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_created_at ON code_analysis_results(created_at DESC);

-- 组合索引：用户ID+创建时间（优化用户分析历史查询）
CREATE INDEX IF NOT EXISTS idx_code_analysis_results_user_created 
ON code_analysis_results(user_id, created_at DESC);

-- ================================
-- 对话会话表索引
-- ================================

-- 用户ID索引
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_user_id ON conversation_sessions(user_id);

-- 会话类型索引
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_type ON conversation_sessions(type);

-- 状态索引
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_status ON conversation_sessions(status);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_created_at ON conversation_sessions(created_at DESC);

-- 最后消息时间索引
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_last_message_at ON conversation_sessions(last_message_at DESC);

-- 组合索引：用户ID+状态（优化活跃会话查询）
CREATE INDEX IF NOT EXISTS idx_conversation_sessions_user_status 
ON conversation_sessions(user_id, status);

-- ================================
-- 对话消息表索引
-- ================================

-- 会话ID索引
CREATE INDEX IF NOT EXISTS idx_conversation_messages_session_id ON conversation_messages(session_id);

-- 发送者类型索引
CREATE INDEX IF NOT EXISTS idx_conversation_messages_sender_type ON conversation_messages(sender_type);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_conversation_messages_created_at ON conversation_messages(created_at);

-- 组合索引：会话ID+创建时间（优化消息历史查询）
CREATE INDEX IF NOT EXISTS idx_conversation_messages_session_created 
ON conversation_messages(session_id, created_at);

-- 全文搜索索引（消息内容）
CREATE FULLTEXT INDEX IF NOT EXISTS idx_conversation_messages_content_search 
ON conversation_messages(content);

-- ================================
-- 学习笔记表索引
-- ================================

-- 用户ID索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_user_id ON learning_notes(user_id);

-- 学习路径ID索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_learning_path_id ON learning_notes(learning_path_id);

-- 模块ID索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_module_id ON learning_notes(module_id);

-- 笔记类型索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_note_type ON learning_notes(note_type);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_created_at ON learning_notes(created_at DESC);

-- 更新时间索引
CREATE INDEX IF NOT EXISTS idx_learning_notes_updated_at ON learning_notes(updated_at DESC);

-- 组合索引：用户ID+路径ID（优化用户笔记查询）
CREATE INDEX IF NOT EXISTS idx_learning_notes_user_path 
ON learning_notes(user_id, learning_path_id);

-- 全文搜索索引（标题和内容）
CREATE FULLTEXT INDEX IF NOT EXISTS idx_learning_notes_search 
ON learning_notes(title, content);

-- ================================
-- 性能监控和分析
-- ================================

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

-- 创建慢查询监控表
CREATE TABLE IF NOT EXISTS slow_query_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_time DECIMAL(10,6) NOT NULL,
    lock_time DECIMAL(10,6) NOT NULL,
    rows_sent INT NOT NULL,
    rows_examined INT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_host VARCHAR(255),
    sql_text TEXT NOT NULL,
    INDEX idx_slow_query_timestamp (timestamp),
    INDEX idx_slow_query_time (query_time)
);

-- ================================
-- 索引使用情况统计
-- ================================

-- 查询当前数据库的索引使用情况
-- （需要在应用运行一段时间后执行以获取有意义的统计）

/*
-- 查看索引使用统计的SQL（仅供参考，不自动执行）
SELECT 
    t.table_name,
    s.index_name,
    s.cardinality,
    s.pages,
    CASE 
        WHEN s.cardinality = 0 THEN 'UNUSED'
        WHEN s.cardinality < 100 THEN 'LOW_USAGE'
        WHEN s.cardinality < 1000 THEN 'MEDIUM_USAGE'
        ELSE 'HIGH_USAGE'
    END as usage_level
FROM information_schema.tables t
LEFT JOIN information_schema.statistics s ON t.table_name = s.table_name
WHERE t.table_schema = DATABASE()
AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name, s.seq_in_index;

-- 查看表大小和行数统计
SELECT 
    table_name,
    table_rows,
    ROUND((data_length + index_length) / 1024 / 1024, 2) as size_mb,
    ROUND(data_length / 1024 / 1024, 2) as data_mb,
    ROUND(index_length / 1024 / 1024, 2) as index_mb
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_type = 'BASE TABLE'
ORDER BY (data_length + index_length) DESC;
*/

-- ================================
-- 维护脚本
-- ================================

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

-- 注释：定期执行 CALL OptimizeTables(); 来优化表性能