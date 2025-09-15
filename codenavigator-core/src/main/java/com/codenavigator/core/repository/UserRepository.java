package com.codenavigator.core.repository;

import com.codenavigator.core.entity.User;
import com.codenavigator.common.enums.UserLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户（使用索引优化）
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户（使用索引优化）
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在（优化查询，只返回boolean）
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在（优化查询，只返回boolean）
     */
    boolean existsByEmail(String email);

    /**
     * 根据用户等级查找用户（分页查询）
     */
    Page<User> findByLevel(UserLevel level, Pageable pageable);

    /**
     * 查找活跃用户（最近30天有活动）
     * 使用原生SQL查询优化性能
     */
    @Query(value = """
        SELECT u.* FROM users u 
        WHERE EXISTS (
            SELECT 1 FROM user_progress up 
            WHERE up.user_id = u.id 
            AND up.last_active_at >= :activeDate
        )
        """, nativeQuery = true)
    List<User> findActiveUsers(@Param("activeDate") LocalDateTime activeDate);

    /**
     * 查找用户的基本信息（不加载关联数据）
     * 优化查询性能，避免N+1问题
     */
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findBasicInfoById(@Param("userId") Long userId);

    /**
     * 查找用户及其进度信息（使用JOIN FETCH优化）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.progressList WHERE u.id = :userId")
    Optional<User> findByIdWithProgress(@Param("userId") Long userId);

    /**
     * 查找用户及其笔记信息（使用JOIN FETCH优化）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.notes WHERE u.id = :userId")
    Optional<User> findByIdWithNotes(@Param("userId") Long userId);

    /**
     * 批量更新用户等级
     */
    @Modifying
    @Query("UPDATE User u SET u.level = :newLevel WHERE u.level = :oldLevel")
    int updateUserLevel(@Param("oldLevel") UserLevel oldLevel, @Param("newLevel") UserLevel newLevel);

    /**
     * 查找顶级学习者（完成路径数量最多的用户）
     */
    @Query(value = """
        SELECT u.* FROM users u
        INNER JOIN (
            SELECT user_id, COUNT(*) as completed_count
            FROM user_progress 
            WHERE status = 'COMPLETED'
            GROUP BY user_id
            ORDER BY completed_count DESC
            LIMIT :limit
        ) top_users ON u.id = top_users.user_id
        ORDER BY top_users.completed_count DESC
        """, nativeQuery = true)
    List<User> findTopLearners(@Param("limit") int limit);

    /**
     * 统计用户按等级分布
     */
    @Query("SELECT u.level, COUNT(u) FROM User u GROUP BY u.level")
    List<Object[]> getUserLevelDistribution();

    /**
     * 查找新注册用户（指定天数内）
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :fromDate ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(@Param("fromDate") LocalDateTime fromDate);

    /**
     * 搜索用户（用户名或邮箱模糊匹配）
     * 使用全文搜索优化
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取用户统计信息（避免加载完整实体）
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_users,
            COUNT(CASE WHEN level = 'BEGINNER' THEN 1 END) as beginners,
            COUNT(CASE WHEN level = 'INTERMEDIATE' THEN 1 END) as intermediate,
            COUNT(CASE WHEN level = 'ADVANCED' THEN 1 END) as advanced,
            COUNT(CASE WHEN created_at >= :recentDate THEN 1 END) as recent_users
        FROM users
        """, nativeQuery = true)
    Object[] getUserStatistics(@Param("recentDate") LocalDateTime recentDate);
}