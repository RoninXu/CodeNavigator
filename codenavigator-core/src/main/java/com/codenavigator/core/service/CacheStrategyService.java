package com.codenavigator.core.service;

import com.codenavigator.core.config.RedisConfig;
import com.codenavigator.core.entity.User;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.core.entity.UserProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStrategyService {

    private final CacheService cacheService;

    /**
     * 缓存用户基本信息
     */
    @Cacheable(value = RedisConfig.CacheNames.USER_CACHE, key = "#userId")
    public User cacheUserInfo(Long userId, User user) {
        log.debug("Caching user info: userId={}", userId);
        return user;
    }
    
    /**
     * 更新用户缓存
     */
    @CachePut(value = RedisConfig.CacheNames.USER_CACHE, key = "#user.id")
    public User updateUserCache(User user) {
        log.debug("Updating user cache: userId={}", user.getId());
        return user;
    }
    
    /**
     * 清除用户缓存
     */
    @CacheEvict(value = RedisConfig.CacheNames.USER_CACHE, key = "#userId")
    public void evictUserCache(Long userId) {
        log.debug("Evicting user cache: userId={}", userId);
    }
    
    /**
     * 清除所有用户缓存
     */
    @CacheEvict(value = RedisConfig.CacheNames.USER_CACHE, allEntries = true)
    public void evictAllUserCache() {
        log.debug("Evicting all user cache");
    }

    /**
     * 缓存学习路径信息
     */
    @Cacheable(value = RedisConfig.CacheNames.LEARNING_PATH_CACHE, key = "#pathId")
    public LearningPath cacheLearningPath(String pathId, LearningPath path) {
        log.debug("Caching learning path: pathId={}", pathId);
        return path;
    }
    
    /**
     * 更新学习路径缓存
     */
    @CachePut(value = RedisConfig.CacheNames.LEARNING_PATH_CACHE, key = "#path.id")
    public LearningPath updateLearningPathCache(LearningPath path) {
        log.debug("Updating learning path cache: pathId={}", path.getId());
        return path;
    }
    
    /**
     * 清除学习路径缓存
     */
    @CacheEvict(value = RedisConfig.CacheNames.LEARNING_PATH_CACHE, key = "#pathId")
    public void evictLearningPathCache(String pathId) {
        log.debug("Evicting learning path cache: pathId={}", pathId);
    }
    
    /**
     * 缓存热门学习路径
     */
    @Cacheable(value = RedisConfig.CacheNames.HOT_CONTENT_CACHE, key = "'popular_paths_' + #limit")
    public List<LearningPath> cachePopularPaths(int limit, List<LearningPath> paths) {
        log.debug("Caching popular learning paths: limit={}, count={}", limit, paths.size());
        return paths;
    }

    /**
     * 缓存用户进度
     */
    @Cacheable(value = RedisConfig.CacheNames.USER_PROGRESS_CACHE, 
              key = "#userId + '_' + #pathId")
    public UserProgress cacheUserProgress(Long userId, String pathId, UserProgress progress) {
        log.debug("Caching user progress: userId={}, pathId={}", userId, pathId);
        return progress;
    }
    
    /**
     * 更新用户进度缓存
     */
    @CachePut(value = RedisConfig.CacheNames.USER_PROGRESS_CACHE, 
             key = "#progress.user.id + '_' + #progress.learningPath.id")
    public UserProgress updateUserProgressCache(UserProgress progress) {
        log.debug("Updating user progress cache: userId={}, pathId={}", 
                 progress.getUser().getId(), progress.getLearningPath().getId());
        return progress;
    }
    
    /**
     * 清除用户进度缓存
     */
    @CacheEvict(value = RedisConfig.CacheNames.USER_PROGRESS_CACHE, 
               key = "#userId + '_' + #pathId")
    public void evictUserProgressCache(Long userId, String pathId) {
        log.debug("Evicting user progress cache: userId={}, pathId={}", userId, pathId);
    }
    
    /**
     * 清除用户所有进度缓存
     */
    @CacheEvict(value = RedisConfig.CacheNames.USER_PROGRESS_CACHE, 
               key = "#userId + '_*'")
    public void evictAllUserProgressCache(Long userId) {
        log.debug("Evicting all user progress cache: userId={}", userId);
    }

    /**
     * 统计数据缓存管理
     */
    public void cacheStatistics(String type, String period, Object data) {
        String key = RedisConfig.CacheKeyGenerator.statisticsKey(type, period);
        cacheService.set(key, data, RedisConfig.CacheConfig.STATISTICS_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Cached statistics: type={}, period={}", type, period);
    }

    /**
     * 获取缓存的统计数据
     */
    public <T> T getCachedStatistics(String type, String period, Class<T> clazz) {
        String key = RedisConfig.CacheKeyGenerator.statisticsKey(type, period);
        T data = cacheService.get(key, clazz);
        if (data != null) {
            log.debug("Cache hit for statistics: type={}, period={}", type, period);
        } else {
            log.debug("Cache miss for statistics: type={}, period={}", type, period);
        }
        return data;
    }

    /**
     * 缓存排行榜数据
     */
    public void cacheLeaderboard(String type, int limit, List<?> data) {
        String key = RedisConfig.CacheKeyGenerator.leaderboardKey(type, limit);
        cacheService.set(key, data, RedisConfig.CacheConfig.LEADERBOARD_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Cached leaderboard: type={}, limit={}, size={}", type, limit, data.size());
    }

    /**
     * 获取缓存的排行榜数据
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCachedLeaderboard(String type, int limit, Class<T> clazz) {
        String key = RedisConfig.CacheKeyGenerator.leaderboardKey(type, limit);
        Object data = cacheService.get(key);
        if (data instanceof List) {
            log.debug("Cache hit for leaderboard: type={}, limit={}", type, limit);
            return (List<T>) data;
        }
        log.debug("Cache miss for leaderboard: type={}, limit={}", type, limit);
        return null;
    }

    /**
     * 缓存代码分析结果
     */
    public void cacheCodeAnalysis(Long userId, String codeHash, Object analysisResult) {
        String key = RedisConfig.CacheKeyGenerator.codeAnalysisKey(userId, codeHash);
        cacheService.set(key, analysisResult, RedisConfig.CacheConfig.CODE_ANALYSIS_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Cached code analysis: userId={}, codeHash={}", userId, codeHash);
    }

    /**
     * 获取缓存的代码分析结果
     */
    public <T> T getCachedCodeAnalysis(Long userId, String codeHash, Class<T> clazz) {
        String key = RedisConfig.CacheKeyGenerator.codeAnalysisKey(userId, codeHash);
        T result = cacheService.get(key, clazz);
        if (result != null) {
            log.debug("Cache hit for code analysis: userId={}, codeHash={}", userId, codeHash);
        } else {
            log.debug("Cache miss for code analysis: userId={}, codeHash={}", userId, codeHash);
        }
        return result;
    }

    /**
     * 缓存对话会话
     */
    public void cacheConversation(String sessionId, Object conversationData) {
        String key = RedisConfig.CacheKeyGenerator.conversationKey(sessionId);
        cacheService.set(key, conversationData, RedisConfig.CacheConfig.CONVERSATION_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Cached conversation: sessionId={}", sessionId);
    }

    /**
     * 获取缓存的对话会话
     */
    public <T> T getCachedConversation(String sessionId, Class<T> clazz) {
        String key = RedisConfig.CacheKeyGenerator.conversationKey(sessionId);
        T data = cacheService.get(key, clazz);
        if (data != null) {
            log.debug("Cache hit for conversation: sessionId={}", sessionId);
        } else {
            log.debug("Cache miss for conversation: sessionId={}", sessionId);
        }
        return data;
    }

    /**
     * 缓存预热
     */
    public void warmupCache() {
        log.info("Starting cache warmup...");
        
        try {
            // 预热热门学习路径
            warmupPopularPaths();
            
            // 预热统计数据
            warmupStatistics();
            
            // 预热排行榜数据
            warmupLeaderboards();
            
            log.info("Cache warmup completed successfully");
        } catch (Exception e) {
            log.error("Cache warmup failed", e);
        }
    }

    /**
     * 预热热门学习路径
     */
    private void warmupPopularPaths() {
        // 这里可以预加载热门的学习路径到缓存
        log.debug("Warming up popular learning paths");
    }

    /**
     * 预热统计数据
     */
    private void warmupStatistics() {
        // 这里可以预加载常用的统计数据到缓存
        log.debug("Warming up statistics data");
    }

    /**
     * 预热排行榜数据
     */
    private void warmupLeaderboards() {
        // 这里可以预加载排行榜数据到缓存
        log.debug("Warming up leaderboard data");
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        log.info("Starting expired cache cleanup...");
        
        try {
            // 查找并清理过期的缓存键
            cleanupExpiredKeys("user:*");
            cleanupExpiredKeys("learningPath:*");
            cleanupExpiredKeys("userProgress:*");
            
            log.info("Expired cache cleanup completed");
        } catch (Exception e) {
            log.error("Expired cache cleanup failed", e);
        }
    }

    /**
     * 清理指定模式的过期键
     */
    private void cleanupExpiredKeys(String pattern) {
        var keys = cacheService.keys(pattern);
        for (String key : keys) {
            long ttl = cacheService.getExpire(key);
            if (ttl <= 0) {
                cacheService.delete(key);
                log.debug("Cleaned up expired key: {}", key);
            }
        }
    }

    /**
     * 获取缓存使用情况统计
     */
    public Map<String, Object> getCacheUsageStats() {
        Map<String, Object> stats = Map.of(
            "userCacheCount", cacheService.keys("user:*").size(),
            "learningPathCacheCount", cacheService.keys("learningPath:*").size(),
            "userProgressCacheCount", cacheService.keys("userProgress:*").size(),
            "statisticsCacheCount", cacheService.keys("statistics:*").size(),
            "leaderboardCacheCount", cacheService.keys("leaderboard:*").size()
        );
        
        log.debug("Cache usage stats: {}", stats);
        return stats;
    }

    /**
     * 批量失效相关缓存
     */
    public void invalidateRelatedCaches(Long userId) {
        // 清除用户相关的所有缓存
        evictUserCache(userId);
        
        // 清除用户进度相关缓存
        var progressKeys = cacheService.keys("userProgress:" + userId + "_*");
        if (!progressKeys.isEmpty()) {
            cacheService.delete(progressKeys);
        }
        
        // 清除统计相关缓存（如果用户数据变化影响统计）
        var statsKeys = cacheService.keys("statistics:*");
        if (!statsKeys.isEmpty()) {
            cacheService.delete(statsKeys);
        }
        
        log.debug("Invalidated related caches for user: {}", userId);
    }
}