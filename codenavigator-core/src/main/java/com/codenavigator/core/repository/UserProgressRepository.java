package com.codenavigator.core.repository;

import com.codenavigator.core.entity.UserProgress;
import com.codenavigator.common.enums.ProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    /**
     * 查找用户的学习进度（根据用户ID和路径ID）
     */
    Optional<UserProgress> findByUserIdAndLearningPathId(Long userId, String learningPathId);

    /**
     * 查找用户的所有学习进度
     */
    List<UserProgress> findByUserIdOrderByLastActiveAtDesc(Long userId);

    /**
     * 查找用户正在进行的学习路径
     */
    List<UserProgress> findByUserIdAndStatusOrderByLastActiveAtDesc(Long userId, ProgressStatus status);

    /**
     * 查找用户的学习进度（包含关联数据）
     */
    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN FETCH up.learningPath " +
           "LEFT JOIN FETCH up.currentModule " +
           "WHERE up.userId = :userId")
    List<UserProgress> findByUserIdWithDetails(@Param("userId") Long userId);

    /**
     * 查找特定路径的所有学习者进度
     */
    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN FETCH up.user " +
           "WHERE up.learningPath.id = :pathId " +
           "ORDER BY up.completionPercentage DESC")
    List<UserProgress> findByLearningPathIdWithUsers(@Param("pathId") String pathId);

    /**
     * 获取用户学习统计信息
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_paths,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_paths,
            COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_paths,
            AVG(CASE WHEN status = 'COMPLETED' THEN completion_percentage END) as avg_completion_rate,
            MAX(last_active_at) as last_activity
        FROM user_progress 
        WHERE user_id = :userId
        """, nativeQuery = true)
    Object[] getUserProgressStatistics(@Param("userId") Long userId);

    /**
     * 查找活跃学习者（最近指定天数内有活动）
     */
    @Query("SELECT up FROM UserProgress up " +
           "WHERE up.lastActiveAt >= :activeDate " +
           "ORDER BY up.lastActiveAt DESC")
    List<UserProgress> findActiveLearners(@Param("activeDate") LocalDateTime activeDate);

    /**
     * 查找需要提醒的用户（长时间未活跃）
     */
    @Query("SELECT up FROM UserProgress up " +
           "WHERE up.status = 'IN_PROGRESS' " +
           "AND up.lastActiveAt < :reminderDate " +
           "ORDER BY up.lastActiveAt ASC")
    List<UserProgress> findUsersNeedingReminder(@Param("reminderDate") LocalDateTime reminderDate);

    /**
     * 批量更新最后活跃时间
     */
    @Modifying
    @Query("UPDATE UserProgress up SET up.lastActiveAt = :activeTime " +
           "WHERE up.userId = :userId AND up.learningPath.id = :pathId")
    int updateLastActiveTime(@Param("userId") Long userId, 
                           @Param("pathId") String pathId, 
                           @Param("activeTime") LocalDateTime activeTime);

    /**
     * 更新进度百分比
     */
    @Modifying
    @Query("UPDATE UserProgress up SET " +
           "up.completedModules = :completedModules, " +
           "up.completionPercentage = :percentage, " +
           "up.lastActiveAt = :activeTime " +
           "WHERE up.id = :progressId")
    int updateProgress(@Param("progressId") Long progressId,
                      @Param("completedModules") Integer completedModules,
                      @Param("percentage") Double percentage,
                      @Param("activeTime") LocalDateTime activeTime);

    /**
     * 查找高完成率的学习者（指定路径）
     */
    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN FETCH up.user " +
           "WHERE up.learningPath.id = :pathId " +
           "AND up.completionPercentage >= :minPercentage " +
           "ORDER BY up.completionPercentage DESC")
    List<UserProgress> findHighPerformers(@Param("pathId") String pathId, 
                                         @Param("minPercentage") Double minPercentage);

    /**
     * 查找学习路径的完成统计
     */
    @Query(value = """
        SELECT 
            lp.id as path_id,
            lp.title as path_title,
            COUNT(up.id) as total_learners,
            COUNT(CASE WHEN up.status = 'COMPLETED' THEN 1 END) as completed_count,
            COUNT(CASE WHEN up.status = 'IN_PROGRESS' THEN 1 END) as in_progress_count,
            AVG(up.completion_percentage) as avg_progress,
            AVG(CASE WHEN up.completed_at IS NOT NULL 
                THEN DATEDIFF(up.completed_at, up.started_at) END) as avg_completion_days
        FROM learning_paths lp
        LEFT JOIN user_progress up ON lp.id = up.learning_path_id
        WHERE lp.is_active = true
        GROUP BY lp.id, lp.title
        ORDER BY completed_count DESC
        """, nativeQuery = true)
    List<Object[]> getPathCompletionStatistics();

    /**
     * 查找用户最近的学习活动
     */
    @Query("SELECT up FROM UserProgress up " +
           "LEFT JOIN FETCH up.learningPath " +
           "WHERE up.userId = :userId " +
           "AND up.lastActiveAt >= :fromDate " +
           "ORDER BY up.lastActiveAt DESC")
    List<UserProgress> findRecentActivity(@Param("userId") Long userId, 
                                         @Param("fromDate") LocalDateTime fromDate);

    /**
     * 检查用户是否已开始某个学习路径
     */
    boolean existsByUserIdAndLearningPathId(Long userId, String learningPathId);

    /**
     * 查找排行榜数据（按完成路径数排序）
     */
    @Query(value = """
        SELECT 
            u.id,
            u.username,
            u.avatar,
            COUNT(up.id) as completed_paths,
            AVG(up.completion_percentage) as avg_progress
        FROM users u
        INNER JOIN user_progress up ON u.id = up.user_id
        WHERE up.status = 'COMPLETED'
        GROUP BY u.id, u.username, u.avatar
        ORDER BY completed_paths DESC, avg_progress DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getLeaderboard(@Param("limit") int limit);

    /**
     * 查找学习路径的每日完成统计
     */
    @Query(value = """
        SELECT 
            DATE(completed_at) as completion_date,
            COUNT(*) as daily_completions
        FROM user_progress 
        WHERE status = 'COMPLETED' 
        AND completed_at >= :fromDate
        GROUP BY DATE(completed_at)
        ORDER BY completion_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyCompletionStats(@Param("fromDate") LocalDateTime fromDate);

    /**
     * 查找用户学习连续天数
     */
    @Query(value = """
        SELECT 
            user_id,
            MAX(consecutive_days) as max_streak
        FROM (
            SELECT 
                user_id,
                COUNT(*) as consecutive_days
            FROM (
                SELECT 
                    user_id,
                    DATE(last_active_at) as activity_date,
                    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY DATE(last_active_at)) -
                    DATEDIFF(DATE(last_active_at), :baseDate) as group_id
                FROM user_progress 
                WHERE last_active_at >= :fromDate
            ) grouped_dates
            GROUP BY user_id, group_id
        ) streaks
        WHERE user_id = :userId
        GROUP BY user_id
        """, nativeQuery = true)
    Object[] getUserLearningStreak(@Param("userId") Long userId, 
                                  @Param("fromDate") LocalDateTime fromDate,
                                  @Param("baseDate") LocalDateTime baseDate);

    /**
     * 删除用户的特定学习进度
     */
    @Modifying
    @Query("DELETE FROM UserProgress up WHERE up.userId = :userId AND up.learningPath.id = :pathId")
    int deleteByUserIdAndLearningPathId(@Param("userId") Long userId, @Param("pathId") String pathId);
}