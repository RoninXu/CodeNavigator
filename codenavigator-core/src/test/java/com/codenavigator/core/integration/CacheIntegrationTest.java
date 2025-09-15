package com.codenavigator.core.integration;

import com.codenavigator.core.service.CacheService;
import com.codenavigator.core.service.CacheStrategyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {CacheService.class, CacheStrategyService.class})
@ActiveProfiles("test")
class CacheIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheStrategyService cacheStrategyService;

    @Test
    void testCacheServiceIntegration() {
        String key = "integration:test:key";
        String value = "integration test value";

        // Test basic set and get
        cacheService.set(key, value);
        String retrievedValue = cacheService.get(key, String.class);
        assertEquals(value, retrievedValue);

        // Test existence check
        assertTrue(cacheService.exists(key));

        // Test delete
        assertTrue(cacheService.delete(key));
        assertFalse(cacheService.exists(key));
    }

    @Test
    void testCacheStrategyServiceIntegration() {
        String type = "user";
        String period = "daily";
        Map<String, Object> testData = Map.of(
            "totalUsers", 100,
            "activeUsers", 75,
            "newUsers", 25
        );

        // Test statistics caching
        cacheStrategyService.cacheStatistics(type, period, testData);
        
        Map<String, Object> cachedData = cacheStrategyService.getCachedStatistics(type, period, Map.class);
        assertNotNull(cachedData);
        assertEquals(100, cachedData.get("totalUsers"));
        assertEquals(75, cachedData.get("activeUsers"));
        assertEquals(25, cachedData.get("newUsers"));
    }

    @Test
    void testLeaderboardCaching() {
        String type = "top_learners";
        int limit = 5;
        List<String> leaderboard = List.of("user1", "user2", "user3", "user4", "user5");

        // Test leaderboard caching
        cacheStrategyService.cacheLeaderboard(type, limit, leaderboard);
        
        List<String> cachedLeaderboard = cacheStrategyService.getCachedLeaderboard(type, limit, String.class);
        assertNotNull(cachedLeaderboard);
        assertEquals(5, cachedLeaderboard.size());
        assertEquals("user1", cachedLeaderboard.get(0));
        assertEquals("user5", cachedLeaderboard.get(4));
    }

    @Test
    void testCacheUsageStats() {
        // Generate some cache data
        cacheService.set("user:1", "userData1");
        cacheService.set("user:2", "userData2");
        cacheService.set("learningPath:path1", "pathData1");
        cacheService.set("statistics:daily", "statsData");

        Map<String, Object> stats = cacheStrategyService.getCacheUsageStats();
        assertNotNull(stats);
        
        // Verify that the stats contain expected keys
        assertTrue(stats.containsKey("userCacheCount"));
        assertTrue(stats.containsKey("learningPathCacheCount"));
        assertTrue(stats.containsKey("statisticsCacheCount"));
    }

    @Test
    void testCacheCleanup() {
        // Create some test data
        cacheService.set("test:cleanup:1", "data1");
        cacheService.set("test:cleanup:2", "data2");
        
        assertTrue(cacheService.exists("test:cleanup:1"));
        assertTrue(cacheService.exists("test:cleanup:2"));

        // Test cleanup doesn't throw exceptions
        assertDoesNotThrow(() -> cacheStrategyService.cleanupExpiredCache());
    }

    @Test
    void testCacheWarmup() {
        // Test warmup doesn't throw exceptions
        assertDoesNotThrow(() -> cacheStrategyService.warmupCache());
    }
}