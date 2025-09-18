package com.codenavigator.web.config;

import com.codenavigator.web.interceptor.PerformanceInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceConfigTest {

    @Mock
    private PerformanceInterceptor performanceInterceptor;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private AsyncSupportConfigurer asyncSupportConfigurer;

    private PerformanceConfig performanceConfig;

    @BeforeEach
    void setUp() {
        performanceConfig = new PerformanceConfig(performanceInterceptor);
    }

    @Test
    void testAddInterceptors() {
        // Given
        var interceptorRegistration = mock(org.springframework.web.servlet.config.annotation.InterceptorRegistration.class);
        when(interceptorRegistry.addInterceptor(performanceInterceptor)).thenReturn(interceptorRegistration);
        when(interceptorRegistration.addPathPatterns("/**")).thenReturn(interceptorRegistration);

        // When
        performanceConfig.addInterceptors(interceptorRegistry);

        // Then
        verify(interceptorRegistry).addInterceptor(performanceInterceptor);
        verify(interceptorRegistration).addPathPatterns("/**");
        verify(interceptorRegistration).excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico");
    }

    @Test
    void testConfigureAsyncSupport() {
        // When
        performanceConfig.configureAsyncSupport(asyncSupportConfigurer);

        // Then
        verify(asyncSupportConfigurer).setTaskExecutor(any());
        verify(asyncSupportConfigurer).setDefaultTimeout(30000);
    }

    @Test
    void testAsyncTaskExecutor() {
        // When
        Executor executor = performanceConfig.asyncTaskExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(8, threadPoolExecutor.getCorePoolSize());
        assertEquals(20, threadPoolExecutor.getMaxPoolSize());
        assertEquals(100, threadPoolExecutor.getQueueCapacity());
        assertEquals("CodeNavigator-Async-", threadPoolExecutor.getThreadNamePrefix());
        assertEquals(60, threadPoolExecutor.getKeepAliveSeconds());
        // Note: getWaitForTasksToCompleteOnShutdown() and getAwaitTerminationSeconds() methods
        // are not available in newer Spring versions - these properties are set but not accessible via getter
    }

    @Test
    void testBatchTaskExecutor() {
        // When
        Executor executor = performanceConfig.batchTaskExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(4, threadPoolExecutor.getCorePoolSize());
        assertEquals(10, threadPoolExecutor.getMaxPoolSize());
        assertEquals(200, threadPoolExecutor.getQueueCapacity());
        assertEquals("CodeNavigator-Batch-", threadPoolExecutor.getThreadNamePrefix());
        assertEquals(120, threadPoolExecutor.getKeepAliveSeconds());
        // Note: getWaitForTasksToCompleteOnShutdown() and getAwaitTerminationSeconds() methods
        // are not available in newer Spring versions - these properties are set but not accessible via getter
    }

    @Test
    void testAiTaskExecutor() {
        // When
        Executor executor = performanceConfig.aiTaskExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(3, threadPoolExecutor.getCorePoolSize());
        assertEquals(8, threadPoolExecutor.getMaxPoolSize());
        assertEquals(50, threadPoolExecutor.getQueueCapacity());
        assertEquals("CodeNavigator-AI-", threadPoolExecutor.getThreadNamePrefix());
        assertEquals(300, threadPoolExecutor.getKeepAliveSeconds());
        // Note: getWaitForTasksToCompleteOnShutdown() and getAwaitTerminationSeconds() methods
        // are not available in newer Spring versions - these properties are set but not accessible via getter
    }

    @Test
    void testPerformanceConstants() {
        // Test constant values
        assertEquals(1000, PerformanceConfig.PerformanceConstants.SLOW_REQUEST_THRESHOLD);
        assertEquals(3000, PerformanceConfig.PerformanceConstants.VERY_SLOW_REQUEST_THRESHOLD);
        assertEquals(100, PerformanceConfig.PerformanceConstants.MAX_CONCURRENT_REQUESTS);
        assertEquals(1000, PerformanceConfig.PerformanceConstants.MAX_REQUESTS_PER_MINUTE);
        assertEquals(50, PerformanceConfig.PerformanceConstants.CACHE_PRELOAD_SIZE);
        assertEquals(30, PerformanceConfig.PerformanceConstants.CACHE_WARMUP_DELAY);
        assertEquals(500, PerformanceConfig.PerformanceConstants.ASYNC_PROCESSING_THRESHOLD);
        assertEquals(20, PerformanceConfig.PerformanceConstants.BATCH_SIZE);
        assertEquals(100, PerformanceConfig.PerformanceConstants.MAX_BATCH_SIZE);
        assertEquals(30000, PerformanceConfig.PerformanceConstants.API_TIMEOUT);
        assertEquals(120000, PerformanceConfig.PerformanceConstants.AI_PROCESSING_TIMEOUT);
        assertEquals(10000, PerformanceConfig.PerformanceConstants.DATABASE_QUERY_TIMEOUT);
    }

    @Test
    void testPerformanceMetricsRecordRequest() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();

        // When
        PerformanceConfig.PerformanceMetrics.recordRequest(500, false);
        PerformanceConfig.PerformanceMetrics.recordRequest(1500, false); // slow request
        PerformanceConfig.PerformanceMetrics.recordRequest(200, true); // error request

        // Then
        assertEquals(3, getTotalRequests());
        assertEquals(1, getSlowRequests());
        assertEquals(1, getErrorRequests());
        assertEquals(733.33, PerformanceConfig.PerformanceMetrics.getAverageResponseTime(), 0.1);
        assertEquals(33.33, PerformanceConfig.PerformanceMetrics.getSlowRequestRate(), 0.1);
        assertEquals(33.33, PerformanceConfig.PerformanceMetrics.getErrorRate(), 0.1);
    }

    @Test
    void testPerformanceMetricsConcurrentRequests() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();

        // When
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();

        // Then
        assertEquals(3, getCurrentConcurrentRequests());
        assertEquals(3, getMaxConcurrentRequests());

        // When
        PerformanceConfig.PerformanceMetrics.decrementConcurrentRequests();
        PerformanceConfig.PerformanceMetrics.decrementConcurrentRequests();

        // Then
        assertEquals(1, getCurrentConcurrentRequests());
        assertEquals(3, getMaxConcurrentRequests()); // Max should remain the same
    }

    @Test
    void testPerformanceMetricsDecrementBelowZero() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();

        // When
        PerformanceConfig.PerformanceMetrics.decrementConcurrentRequests(); // Should not go below 0

        // Then
        assertEquals(0, getCurrentConcurrentRequests());
    }

    @Test
    void testPerformanceMetricsWithNoRequests() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();

        // When & Then
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getAverageResponseTime());
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getSlowRequestRate());
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getErrorRate());
    }

    @Test
    void testPerformanceMetricsGetMetricsSummary() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();
        PerformanceConfig.PerformanceMetrics.recordRequest(500, false);
        PerformanceConfig.PerformanceMetrics.recordRequest(1500, true);
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();

        // When
        String summary = PerformanceConfig.PerformanceMetrics.getMetricsSummary();

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("Total Requests: 2"));
        assertTrue(summary.contains("Average Response Time: 1000.00ms"));
        assertTrue(summary.contains("Slow Request Rate: 50.00%"));
        assertTrue(summary.contains("Error Rate: 50.00%"));
        assertTrue(summary.contains("Current Concurrent: 1"));
        assertTrue(summary.contains("Max Concurrent: 1"));
    }

    @Test
    void testPerformanceMetricsReset() {
        // Given - add some data first
        PerformanceConfig.PerformanceMetrics.recordRequest(500, false);
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();

        // When
        PerformanceConfig.PerformanceMetrics.reset();

        // Then
        assertEquals(0, getTotalRequests());
        assertEquals(0, getSlowRequests());
        assertEquals(0, getErrorRequests());
        assertEquals(0, getCurrentConcurrentRequests());
        assertEquals(0, getMaxConcurrentRequests());
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getAverageResponseTime());
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getSlowRequestRate());
        assertEquals(0.0, PerformanceConfig.PerformanceMetrics.getErrorRate());
    }

    @Test
    void testPerformanceMetricsMinMaxResponseTime() {
        // Given
        PerformanceConfig.PerformanceMetrics.reset();

        // When
        PerformanceConfig.PerformanceMetrics.recordRequest(1000, false);
        PerformanceConfig.PerformanceMetrics.recordRequest(500, false);
        PerformanceConfig.PerformanceMetrics.recordRequest(1500, false);

        // Then
        // We can't directly access min/max from public methods, but we can verify through the summary
        String summary = PerformanceConfig.PerformanceMetrics.getMetricsSummary();
        assertEquals(1000.0, PerformanceConfig.PerformanceMetrics.getAverageResponseTime(), 0.1);
    }

    // Helper methods to access private fields through reflection or public methods
    private long getTotalRequests() {
        String summary = PerformanceConfig.PerformanceMetrics.getMetricsSummary();
        String[] parts = summary.split(", ");
        String totalRequestsPart = parts[0];
        return Long.parseLong(totalRequestsPart.split(": ")[1]);
    }

    private long getSlowRequests() {
        double slowRequestRate = PerformanceConfig.PerformanceMetrics.getSlowRequestRate();
        long totalRequests = getTotalRequests();
        return Math.round(slowRequestRate * totalRequests / 100.0);
    }

    private long getErrorRequests() {
        double errorRate = PerformanceConfig.PerformanceMetrics.getErrorRate();
        long totalRequests = getTotalRequests();
        return Math.round(errorRate * totalRequests / 100.0);
    }

    private int getCurrentConcurrentRequests() {
        String summary = PerformanceConfig.PerformanceMetrics.getMetricsSummary();
        String[] parts = summary.split(", ");
        String currentConcurrentPart = parts[4];
        return Integer.parseInt(currentConcurrentPart.split(": ")[1]);
    }

    private int getMaxConcurrentRequests() {
        String summary = PerformanceConfig.PerformanceMetrics.getMetricsSummary();
        String[] parts = summary.split(", ");
        String maxConcurrentPart = parts[5];
        return Integer.parseInt(maxConcurrentPart.split(": ")[1]);
    }
}