package com.codenavigator.core.service;

import com.codenavigator.core.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheStrategyServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheStrategyService cacheStrategyService;

    @BeforeEach
    void setUp() {
        // Setup common mocks
    }

    @Test
    void testCacheStatistics() {
        String type = "user";
        String period = "daily";
        Object data = Map.of("count", 100, "average", 5.5);

        cacheStrategyService.cacheStatistics(type, period, data);

        String expectedKey = RedisConfig.CacheKeyGenerator.statisticsKey(type, period);
        verify(cacheService).set(expectedKey, data, RedisConfig.CacheConfig.STATISTICS_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    @Test
    void testGetCachedStatistics() {
        String type = "user";
        String period = "daily";
        Map<String, Object> expectedData = Map.of("count", 100, "average", 5.5);

        String expectedKey = RedisConfig.CacheKeyGenerator.statisticsKey(type, period);
        when(cacheService.get(expectedKey, Map.class)).thenReturn(expectedData);

        Map<String, Object> result = cacheStrategyService.getCachedStatistics(type, period, Map.class);

        assertEquals(expectedData, result);
        verify(cacheService).get(expectedKey, Map.class);
    }

    @Test
    void testGetCachedStatisticsCacheMiss() {
        String type = "user";
        String period = "daily";

        String expectedKey = RedisConfig.CacheKeyGenerator.statisticsKey(type, period);
        when(cacheService.get(expectedKey, Map.class)).thenReturn(null);

        Map<String, Object> result = cacheStrategyService.getCachedStatistics(type, period, Map.class);

        assertNull(result);
        verify(cacheService).get(expectedKey, Map.class);
    }

    @Test
    void testCacheLeaderboard() {
        String type = "top_users";
        int limit = 10;
        List<Object> data = Arrays.asList("user1", "user2", "user3");

        cacheStrategyService.cacheLeaderboard(type, limit, data);

        String expectedKey = RedisConfig.CacheKeyGenerator.leaderboardKey(type, limit);
        verify(cacheService).set(expectedKey, data, RedisConfig.CacheConfig.LEADERBOARD_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    @Test
    void testGetCachedLeaderboard() {
        String type = "top_users";
        int limit = 10;
        List<String> expectedData = Arrays.asList("user1", "user2", "user3");

        String expectedKey = RedisConfig.CacheKeyGenerator.leaderboardKey(type, limit);
        when(cacheService.get(expectedKey)).thenReturn(expectedData);

        List<String> result = cacheStrategyService.getCachedLeaderboard(type, limit, String.class);

        assertEquals(expectedData, result);
        verify(cacheService).get(expectedKey);
    }

    @Test
    void testGetCachedLeaderboardNotList() {
        String type = "top_users";
        int limit = 10;
        String notAList = "not a list";

        String expectedKey = RedisConfig.CacheKeyGenerator.leaderboardKey(type, limit);
        when(cacheService.get(expectedKey)).thenReturn(notAList);

        List<String> result = cacheStrategyService.getCachedLeaderboard(type, limit, String.class);

        assertNull(result);
        verify(cacheService).get(expectedKey);
    }

    @Test
    void testCacheCodeAnalysis() {
        Long userId = 1L;
        String codeHash = "abc123";
        Object analysisResult = Map.of("complexity", 5, "issues", Arrays.asList("issue1", "issue2"));

        cacheStrategyService.cacheCodeAnalysis(userId, codeHash, analysisResult);

        String expectedKey = RedisConfig.CacheKeyGenerator.codeAnalysisKey(userId, codeHash);
        verify(cacheService).set(expectedKey, analysisResult, RedisConfig.CacheConfig.CODE_ANALYSIS_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    @Test
    void testGetCachedCodeAnalysis() {
        Long userId = 1L;
        String codeHash = "abc123";
        Map<String, Object> expectedResult = Map.of("complexity", 5, "issues", Arrays.asList("issue1", "issue2"));

        String expectedKey = RedisConfig.CacheKeyGenerator.codeAnalysisKey(userId, codeHash);
        when(cacheService.get(expectedKey, Map.class)).thenReturn(expectedResult);

        Map<String, Object> result = cacheStrategyService.getCachedCodeAnalysis(userId, codeHash, Map.class);

        assertEquals(expectedResult, result);
        verify(cacheService).get(expectedKey, Map.class);
    }

    @Test
    void testCacheConversation() {
        String sessionId = "session123";
        Object conversationData = Map.of("messages", Arrays.asList("msg1", "msg2"), "context", "test");

        cacheStrategyService.cacheConversation(sessionId, conversationData);

        String expectedKey = RedisConfig.CacheKeyGenerator.conversationKey(sessionId);
        verify(cacheService).set(expectedKey, conversationData, RedisConfig.CacheConfig.CONVERSATION_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    @Test
    void testGetCachedConversation() {
        String sessionId = "session123";
        Map<String, Object> expectedData = Map.of("messages", Arrays.asList("msg1", "msg2"), "context", "test");

        String expectedKey = RedisConfig.CacheKeyGenerator.conversationKey(sessionId);
        when(cacheService.get(expectedKey, Map.class)).thenReturn(expectedData);

        Map<String, Object> result = cacheStrategyService.getCachedConversation(sessionId, Map.class);

        assertEquals(expectedData, result);
        verify(cacheService).get(expectedKey, Map.class);
    }

    @Test
    void testWarmupCache() {
        assertDoesNotThrow(() -> cacheStrategyService.warmupCache());
    }

    @Test
    void testCleanupExpiredCache() {
        Set<String> userKeys = Set.of("user:1", "user:2");
        Set<String> pathKeys = Set.of("learningPath:1", "learningPath:2");
        Set<String> progressKeys = Set.of("userProgress:1_1", "userProgress:1_2");

        when(cacheService.keys("user:*")).thenReturn(userKeys);
        when(cacheService.keys("learningPath:*")).thenReturn(pathKeys);
        when(cacheService.keys("userProgress:*")).thenReturn(progressKeys);

        // Mock some keys as expired
        when(cacheService.getExpire("user:1")).thenReturn(-1L); // expired
        when(cacheService.getExpire("user:2")).thenReturn(3600L); // not expired
        when(cacheService.getExpire("learningPath:1")).thenReturn(0L); // expired
        when(cacheService.getExpire("learningPath:2")).thenReturn(7200L); // not expired
        when(cacheService.getExpire("userProgress:1_1")).thenReturn(-2L); // expired
        when(cacheService.getExpire("userProgress:1_2")).thenReturn(900L); // not expired

        cacheStrategyService.cleanupExpiredCache();

        // Verify expired keys are deleted
        verify(cacheService).delete("user:1");
        verify(cacheService).delete("learningPath:1");
        verify(cacheService).delete("userProgress:1_1");

        // Verify non-expired keys are not deleted
        verify(cacheService, never()).delete("user:2");
        verify(cacheService, never()).delete("learningPath:2");
        verify(cacheService, never()).delete("userProgress:1_2");
    }

    @Test
    void testGetCacheUsageStats() {
        when(cacheService.keys("user:*")).thenReturn(Set.of("user:1", "user:2"));
        when(cacheService.keys("learningPath:*")).thenReturn(Set.of("learningPath:1"));
        when(cacheService.keys("userProgress:*")).thenReturn(Set.of("userProgress:1_1", "userProgress:1_2", "userProgress:2_1"));
        when(cacheService.keys("statistics:*")).thenReturn(Set.of("statistics:user_daily"));
        when(cacheService.keys("leaderboard:*")).thenReturn(Set.of("leaderboard:top_users_10"));

        Map<String, Object> stats = cacheStrategyService.getCacheUsageStats();

        assertEquals(2, stats.get("userCacheCount"));
        assertEquals(1, stats.get("learningPathCacheCount"));
        assertEquals(3, stats.get("userProgressCacheCount"));
        assertEquals(1, stats.get("statisticsCacheCount"));
        assertEquals(1, stats.get("leaderboardCacheCount"));
    }

    @Test
    void testInvalidateRelatedCaches() {
        Long userId = 1L;
        Set<String> progressKeys = Set.of("userProgress:1_path1", "userProgress:1_path2");
        Set<String> statsKeys = Set.of("statistics:user_daily", "statistics:system_hourly");

        when(cacheService.keys("userProgress:" + userId + "_*")).thenReturn(progressKeys);
        when(cacheService.keys("statistics:*")).thenReturn(statsKeys);

        // Test the method execution
        assertDoesNotThrow(() -> cacheStrategyService.invalidateRelatedCaches(userId));

        verify(cacheService).delete(progressKeys);
        verify(cacheService).delete(statsKeys);
    }

    @Test
    void testErrorHandlingInCleanup() {
        Set<String> userKeys = Set.of("user:1", "user:2");
        when(cacheService.keys("user:*")).thenReturn(userKeys);
        when(cacheService.getExpire("user:1")).thenThrow(new RuntimeException("Redis error"));
        when(cacheService.getExpire("user:2")).thenReturn(-1L);

        // Should not throw exception even if individual operations fail
        assertDoesNotThrow(() -> cacheStrategyService.cleanupExpiredCache());

        // Should still process other keys
        verify(cacheService).delete("user:2");
    }
}