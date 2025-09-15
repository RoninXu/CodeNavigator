-- CodeNavigator 最终性能优化索引
-- 基于实际数据库表结构

USE code_navigator;

-- ================================
-- 用户表索引 (users)
-- ================================

-- 技能等级索引
CREATE INDEX idx_users_skill_level ON users(skill_level);

-- 创建时间索引
CREATE INDEX idx_users_created_time ON users(created_time);

-- 最后登录时间索引
CREATE INDEX idx_users_last_login_time ON users(last_login_time);

-- 是否激活索引
CREATE INDEX idx_users_is_active ON users(is_active);

-- 首选编程语言索引
CREATE INDEX idx_users_preferred_language ON users(preferred_language);

-- 组合索引：技能等级+是否激活
CREATE INDEX idx_users_skill_active ON users(skill_level, is_active);

-- 组合索引：创建时间+是否激活（用于查询活跃新用户）
CREATE INDEX idx_users_created_active ON users(created_time, is_active);

-- ================================
-- 学习路径表索引 (learning_paths)
-- ================================

-- 名称索引（用于搜索）
CREATE INDEX idx_learning_paths_name ON learning_paths(name);

-- 难度等级索引
CREATE INDEX idx_learning_paths_difficulty_level ON learning_paths(difficulty_level);

-- 估计时长索引
CREATE INDEX idx_learning_paths_estimated_duration ON learning_paths(estimated_duration);

-- 创建者索引
CREATE INDEX idx_learning_paths_created_by ON learning_paths(created_by);

-- 创建时间索引
CREATE INDEX idx_learning_paths_created_time ON learning_paths(created_time);

-- 是否激活索引
CREATE INDEX idx_learning_paths_is_active ON learning_paths(is_active);

-- 组合索引：难度+是否激活
CREATE INDEX idx_learning_paths_diff_active 
ON learning_paths(difficulty_level, is_active);

-- 组合索引：创建者+创建时间
CREATE INDEX idx_learning_paths_creator_time 
ON learning_paths(created_by, created_time);

-- ================================
-- 学习模块表索引 (learning_modules)
-- ================================

-- 学习路径ID索引
CREATE INDEX idx_learning_modules_path_id ON learning_modules(path_id);

-- 模块类型索引
CREATE INDEX idx_learning_modules_module_type ON learning_modules(module_type);

-- 排序索引
CREATE INDEX idx_learning_modules_order_index ON learning_modules(order_index);

-- 难度等级索引
CREATE INDEX idx_learning_modules_difficulty_level ON learning_modules(difficulty_level);

-- 是否激活索引
CREATE INDEX idx_learning_modules_is_active ON learning_modules(is_active);

-- 组合索引：路径ID+排序
CREATE INDEX idx_learning_modules_path_order 
ON learning_modules(path_id, order_index);

-- 组合索引：路径ID+是否激活
CREATE INDEX idx_learning_modules_path_active 
ON learning_modules(path_id, is_active);

-- ================================
-- 用户进度表索引 (user_progress)
-- ================================

-- 用户ID索引
CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);

-- 模块ID索引
CREATE INDEX idx_user_progress_module_id ON user_progress(module_id);

-- 状态索引
CREATE INDEX idx_user_progress_status ON user_progress(status);

-- 进度百分比索引
CREATE INDEX idx_user_progress_progress_percentage ON user_progress(progress_percentage);

-- 最后访问时间索引
CREATE INDEX idx_user_progress_last_accessed_time ON user_progress(last_accessed_time);

-- 开始时间索引
CREATE INDEX idx_user_progress_started_time ON user_progress(started_time);

-- 完成时间索引
CREATE INDEX idx_user_progress_completed_time ON user_progress(completed_time);

-- 组合索引：用户ID+状态
CREATE INDEX idx_user_progress_user_status 
ON user_progress(user_id, status);

-- 组合索引：模块ID+状态
CREATE INDEX idx_user_progress_module_status 
ON user_progress(module_id, status);

-- 组合索引：用户ID+最后访问时间
CREATE INDEX idx_user_progress_user_accessed 
ON user_progress(user_id, last_accessed_time);

-- 唯一组合索引：用户ID+模块ID
CREATE UNIQUE INDEX idx_user_progress_user_module_unique 
ON user_progress(user_id, module_id);

-- ================================
-- 代码分析结果表索引 (code_analysis_results)
-- ================================

-- 用户ID索引
CREATE INDEX idx_code_analysis_results_user_id ON code_analysis_results(user_id);

-- 分析类型索引
CREATE INDEX idx_code_analysis_results_analysis_type ON code_analysis_results(analysis_type);

-- 编程语言索引
CREATE INDEX idx_code_analysis_results_language ON code_analysis_results(language);

-- 质量评分索引
CREATE INDEX idx_code_analysis_results_quality_score ON code_analysis_results(quality_score);

-- 分析时间索引
CREATE INDEX idx_code_analysis_results_analysis_time ON code_analysis_results(analysis_time);

-- 组合索引：用户ID+分析时间
CREATE INDEX idx_code_analysis_results_user_time 
ON code_analysis_results(user_id, analysis_time);

-- 组合索引：语言+质量评分
CREATE INDEX idx_code_analysis_results_lang_score 
ON code_analysis_results(language, quality_score);

-- ================================
-- 代码问题表索引 (code_issues)
-- ================================

-- 分析结果ID索引
CREATE INDEX idx_code_issues_analysis_result_id ON code_issues(analysis_result_id);

-- 问题类型索引
CREATE INDEX idx_code_issues_issue_type ON code_issues(issue_type);

-- 严重程度索引
CREATE INDEX idx_code_issues_severity ON code_issues(severity);

-- 行号索引
CREATE INDEX idx_code_issues_line_number ON code_issues(line_number);

-- 组合索引：分析结果ID+严重程度
CREATE INDEX idx_code_issues_result_severity 
ON code_issues(analysis_result_id, severity);

-- ================================
-- 代码建议表索引 (code_suggestions)
-- ================================

-- 分析结果ID索引
CREATE INDEX idx_code_suggestions_analysis_result_id ON code_suggestions(analysis_result_id);

-- 建议类型索引
CREATE INDEX idx_code_suggestions_suggestion_type ON code_suggestions(suggestion_type);

-- 优先级索引
CREATE INDEX idx_code_suggestions_priority ON code_suggestions(priority);

-- 组合索引：分析结果ID+优先级
CREATE INDEX idx_code_suggestions_result_priority 
ON code_suggestions(analysis_result_id, priority);

-- ================================
-- 对话会话表索引 (conversation_sessions)
-- ================================

-- 用户ID索引
CREATE INDEX idx_conversation_sessions_user_id ON conversation_sessions(user_id);

-- 会话类型索引
CREATE INDEX idx_conversation_sessions_session_type ON conversation_sessions(session_type);

-- 状态索引
CREATE INDEX idx_conversation_sessions_status ON conversation_sessions(status);

-- 创建时间索引
CREATE INDEX idx_conversation_sessions_created_time ON conversation_sessions(created_time);

-- 最后活跃时间索引
CREATE INDEX idx_conversation_sessions_last_active_time ON conversation_sessions(last_active_time);

-- 组合索引：用户ID+状态
CREATE INDEX idx_conversation_sessions_user_status 
ON conversation_sessions(user_id, status);

-- 组合索引：用户ID+最后活跃时间
CREATE INDEX idx_conversation_sessions_user_active 
ON conversation_sessions(user_id, last_active_time);

-- ================================
-- 对话消息表索引 (conversation_messages)
-- ================================

-- 会话ID索引
CREATE INDEX idx_conversation_messages_session_id ON conversation_messages(session_id);

-- 发送者类型索引
CREATE INDEX idx_conversation_messages_sender_type ON conversation_messages(sender_type);

-- 创建时间索引
CREATE INDEX idx_conversation_messages_created_time ON conversation_messages(created_time);

-- 组合索引：会话ID+创建时间
CREATE INDEX idx_conversation_messages_session_time 
ON conversation_messages(session_id, created_time);

-- ================================
-- 学习笔记表索引 (learning_notes)
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
CREATE INDEX idx_learning_notes_created_time ON learning_notes(created_time);

-- 更新时间索引
CREATE INDEX idx_learning_notes_updated_time ON learning_notes(updated_time);

-- 组合索引：用户ID+学习路径ID
CREATE INDEX idx_learning_notes_user_path 
ON learning_notes(user_id, learning_path_id);

-- 组合索引：用户ID+创建时间
CREATE INDEX idx_learning_notes_user_created 
ON learning_notes(user_id, created_time);

-- ================================
-- 性能分析查询
-- ================================

-- 显示所有创建的索引
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

-- 显示表大小和索引统计
SELECT 
    TABLE_NAME as '表名',
    ENGINE as '引擎',
    TABLE_ROWS as '行数',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
    CASE 
        WHEN DATA_LENGTH = 0 THEN 0
        ELSE ROUND((INDEX_LENGTH / DATA_LENGTH) * 100, 2) 
    END AS '索引占比(%)'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'code_navigator'
AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- 显示重复索引检查
SELECT 
    TABLE_NAME,
    GROUP_CONCAT(INDEX_NAME) as '可能重复的索引',
    GROUP_CONCAT(COLUMN_NAME) as '字段'
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'code_navigator'
GROUP BY TABLE_NAME, COLUMN_NAME
HAVING COUNT(*) > 2
ORDER BY TABLE_NAME;