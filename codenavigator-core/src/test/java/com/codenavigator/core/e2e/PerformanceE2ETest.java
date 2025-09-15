package com.codenavigator.core.e2e;

import com.codenavigator.core.service.CacheService;
import com.codenavigator.core.service.CacheStrategyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceE2ETest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    void testEndToEndCacheWorkflow() {
        // Setup mocks
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        CacheService cacheService = new CacheService(redisTemplate);
        CacheStrategyService strategyService = new CacheStrategyService(cacheService);

        // Test complete cache workflow
        String testData = "test statistics data";
        when(valueOperations.get(any())).thenReturn(testData);
        
        // 1. Cache statistics
        strategyService.cacheStatistics("user", "daily", testData);
        
        // 2. Retrieve cached statistics
        String cachedData = strategyService.getCachedStatistics("user", "daily", String.class);
        
        // 3. Verify workflow
        assertEquals(testData, cachedData);
        
        // Verify interactions
        verify(cacheService).set(any(), eq(testData), anyLong(), any(TimeUnit.class));
        verify(cacheService).get(any(), eq(String.class));
    }

    @Test
    void testCacheUsageStatsWorkflow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys(anyString())).thenReturn(java.util.Set.of("user:1", "user:2"));
        
        CacheService cacheService = new CacheService(redisTemplate);
        CacheStrategyService strategyService = new CacheStrategyService(cacheService);

        // Test cache usage stats workflow
        Map<String, Object> stats = strategyService.getCacheUsageStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("userCacheCount"));
        assertTrue(stats.containsKey("learningPathCacheCount"));
        assertTrue(stats.containsKey("userProgressCacheCount"));
        assertTrue(stats.containsKey("statisticsCacheCount"));
        assertTrue(stats.containsKey("leaderboardCacheCount"));
    }

    @Test
    void testCacheCleanupWorkflow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys(anyString())).thenReturn(java.util.Set.of("user:1", "user:2"));
        when(redisTemplate.getExpire(anyString())).thenReturn(-1L); // expired
        
        CacheService cacheService = new CacheService(redisTemplate);
        CacheStrategyService strategyService = new CacheStrategyService(cacheService);

        // Test cache cleanup workflow
        assertDoesNotThrow(() -> strategyService.cleanupExpiredCache());
        
        // Verify cleanup process
        verify(redisTemplate, atLeastOnce()).keys(anyString());
        verify(redisTemplate, atLeastOnce()).getExpire(anyString());
    }

    @Test
    void testPerformanceOptimizationWorkflow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        CacheService cacheService = new CacheService(redisTemplate);
        CacheStrategyService strategyService = new CacheStrategyService(cacheService);

        // Simulate performance optimization workflow
        long startTime = System.currentTimeMillis();
        
        // 1. Cache warmup
        strategyService.warmupCache();
        
        // 2. Cache some data
        strategyService.cacheStatistics("performance", "test", "perfData");
        
        // 3. Get cache usage stats
        Map<String, Object> stats = strategyService.getCacheUsageStats();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify workflow completed
        assertNotNull(stats);
        assertTrue(duration < 1000, "Performance workflow should complete quickly");
    }

    @Test
    void testErrorRecoveryWorkflow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection error"));
        
        CacheService cacheService = new CacheService(redisTemplate);
        CacheStrategyService strategyService = new CacheStrategyService(cacheService);

        // Test error recovery - should not throw exceptions
        assertDoesNotThrow(() -> {
            String result = strategyService.getCachedStatistics("test", "error", String.class);
            assertNull(result, "Should return null on error");
        });
        
        // Test that other operations still work after error
        assertDoesNotThrow(() -> strategyService.warmupCache());
    }
}