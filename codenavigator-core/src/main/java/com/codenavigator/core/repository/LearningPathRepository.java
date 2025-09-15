package com.codenavigator.core.repository;

import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.UserLevel;
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
public interface LearningPathRepository extends JpaRepository<LearningPath, String> {

    /**
     * 查找活跃的学习路径（分页）
     */
    Page<LearningPath> findByIsActiveTrueOrderByCompletionCountDesc(Pageable pageable);

    /**
     * 根据框架查找学习路径
     */
    Page<LearningPath> findByFrameworkAndIsActiveTrueOrderByAverageRatingDesc(
            String framework, Pageable pageable);

    /**
     * 根据难度等级查找学习路径
     */
    Page<LearningPath> findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(
            DifficultyLevel difficulty, Pageable pageable);

    /**
     * 根据目标用户等级查找学习路径
     */
    Page<LearningPath> findByTargetLevelAndIsActiveTrueOrderByCompletionCountDesc(
            UserLevel targetLevel, Pageable pageable);

    /**
     * 查找学习路径及其模块（优化查询，避免N+1问题）
     */
    @Query("SELECT lp FROM LearningPath lp LEFT JOIN FETCH lp.modules WHERE lp.id = :pathId")
    Optional<LearningPath> findByIdWithModules(@Param("pathId") String pathId);

    /**
     * 搜索学习路径（标题、描述、标签）
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true AND " +
           "(LOWER(lp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(lp.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(lp.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<LearningPath> searchPaths(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找热门学习路径（按完成数排序）
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "ORDER BY lp.completionCount DESC, lp.averageRating DESC")
    List<LearningPath> findPopularPaths(Pageable pageable);

    /**
     * 查找推荐的学习路径（根据用户等级和完成数）
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "AND (lp.targetLevel = :userLevel OR lp.targetLevel IS NULL) " +
           "ORDER BY lp.averageRating DESC, lp.completionCount DESC")
    List<LearningPath> findRecommendedPaths(@Param("userLevel") UserLevel userLevel, Pageable pageable);

    /**
     * 查找最新的学习路径
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "AND lp.createdAt >= :fromDate ORDER BY lp.createdAt DESC")
    List<LearningPath> findRecentPaths(@Param("fromDate") LocalDateTime fromDate);

    /**
     * 根据标签查找学习路径
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "AND LOWER(lp.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Page<LearningPath> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 批量更新完成数
     */
    @Modifying
    @Query("UPDATE LearningPath lp SET lp.completionCount = lp.completionCount + 1 WHERE lp.id = :pathId")
    int incrementCompletionCount(@Param("pathId") String pathId);

    /**
     * 更新平均评分
     */
    @Modifying
    @Query("UPDATE LearningPath lp SET lp.averageRating = :rating WHERE lp.id = :pathId")
    int updateAverageRating(@Param("pathId") String pathId, @Param("rating") Double rating);

    /**
     * 获取学习路径统计信息
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_paths,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_paths,
            COUNT(CASE WHEN difficulty = 'BEGINNER' THEN 1 END) as beginner_paths,
            COUNT(CASE WHEN difficulty = 'INTERMEDIATE' THEN 1 END) as intermediate_paths,
            COUNT(CASE WHEN difficulty = 'ADVANCED' THEN 1 END) as advanced_paths,
            AVG(completion_count) as avg_completion_count,
            AVG(average_rating) as avg_rating
        FROM learning_paths
        WHERE is_active = true
        """, nativeQuery = true)
    Object[] getPathStatistics();

    /**
     * 查找相关学习路径（基于标签和框架）
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "AND lp.id != :excludeId " +
           "AND (lp.framework = :framework OR " +
           "LOWER(lp.tags) LIKE LOWER(CONCAT('%', :tag, '%'))) " +
           "ORDER BY lp.averageRating DESC")
    List<LearningPath> findRelatedPaths(@Param("excludeId") String excludeId, 
                                       @Param("framework") String framework,
                                       @Param("tag") String tag, 
                                       Pageable pageable);

    /**
     * 查找用户正在学习的路径
     */
    @Query(value = """
        SELECT lp.* FROM learning_paths lp
        INNER JOIN user_progress up ON lp.id = up.learning_path_id
        WHERE up.user_id = :userId AND up.status = 'IN_PROGRESS'
        ORDER BY up.last_active_at DESC
        """, nativeQuery = true)
    List<LearningPath> findUserActivePaths(@Param("userId") Long userId);

    /**
     * 查找用户已完成的路径
     */
    @Query(value = """
        SELECT lp.* FROM learning_paths lp
        INNER JOIN user_progress up ON lp.id = up.learning_path_id
        WHERE up.user_id = :userId AND up.status = 'COMPLETED'
        ORDER BY up.completed_at DESC
        """, nativeQuery = true)
    List<LearningPath> findUserCompletedPaths(@Param("userId") Long userId);

    /**
     * 获取框架分布统计
     */
    @Query("SELECT lp.framework, COUNT(lp) FROM LearningPath lp " +
           "WHERE lp.isActive = true GROUP BY lp.framework ORDER BY COUNT(lp) DESC")
    List<Object[]> getFrameworkDistribution();

    /**
     * 查找高评分路径（评分大于指定值）
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true " +
           "AND lp.averageRating >= :minRating ORDER BY lp.averageRating DESC")
    List<LearningPath> findHighRatedPaths(@Param("minRating") Double minRating, Pageable pageable);
}