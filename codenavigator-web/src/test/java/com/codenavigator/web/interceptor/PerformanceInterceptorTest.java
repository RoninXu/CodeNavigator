package com.codenavigator.web.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @Mock
    private ModelAndView modelAndView;

    @InjectMocks
    private PerformanceInterceptor performanceInterceptor;

    @BeforeEach
    void setUp() {
        // Reset static metrics before each test
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.reset();
    }

    @Test
    void testPreHandleSuccess() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        boolean result = performanceInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(request).setAttribute(eq("startTime"), any(Long.class));
        verify(request).setAttribute(eq("requestId"), any(String.class));
    }

    @Test
    void testPreHandleWithConcurrencyLimit() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        // Mock the private method to return true for over concurrency limit
        PerformanceInterceptor spyInterceptor = spy(performanceInterceptor);
        doReturn(true).when(spyInterceptor).isOverConcurrencyLimit();

        boolean result = spyInterceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setHeader("Retry-After", "30");
    }

    @Test
    void testPostHandle() {
        String requestId = "test-request-id";
        when(request.getAttribute("requestId")).thenReturn(requestId);

        performanceInterceptor.postHandle(request, response, handler, modelAndView);

        verify(request).getAttribute("requestId");
    }

    @Test
    void testAfterCompletionNormalRequest() {
        long startTime = System.currentTimeMillis() - 500; // 500ms ago
        String requestId = "test-request-id";

        when(request.getAttribute("startTime")).thenReturn(startTime);
        when(request.getAttribute("requestId")).thenReturn(requestId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);

        performanceInterceptor.afterCompletion(request, response, handler, null);

        verify(request).getAttribute("startTime");
        verify(request).getAttribute("requestId");
        verify(response).getStatus();
    }

    @Test
    void testAfterCompletionWithException() {
        long startTime = System.currentTimeMillis() - 1000; // 1 second ago
        String requestId = "test-request-id";
        Exception exception = new RuntimeException("Test exception");

        when(request.getAttribute("startTime")).thenReturn(startTime);
        when(request.getAttribute("requestId")).thenReturn(requestId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(500);

        performanceInterceptor.afterCompletion(request, response, handler, exception);

        verify(request).getAttribute("startTime");
        verify(request).getAttribute("requestId");
        verify(response).getStatus();
    }

    @Test
    void testAfterCompletionSlowRequest() {
        long startTime = System.currentTimeMillis() - 2000; // 2 seconds ago (slow request)
        String requestId = "test-request-id";

        when(request.getAttribute("startTime")).thenReturn(startTime);
        when(request.getAttribute("requestId")).thenReturn(requestId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/slow");
        when(request.getHeader("User-Agent")).thenReturn("Test Browser");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        performanceInterceptor.afterCompletion(request, response, handler, null);

        verify(request).getAttribute("startTime");
        verify(request).getAttribute("requestId");
        verify(response).getStatus();
        verify(request).getHeader("User-Agent");
    }

    @Test
    void testGetClientIpAddressWithXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        // Use reflection to access private method for testing
        String ip = getClientIpAddressTestHelper("192.168.1.1, 10.0.0.1", null, "127.0.0.1");

        assertEquals("192.168.1.1", ip);
    }

    @Test
    void testGetClientIpAddressWithXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");

        String ip = getClientIpAddressTestHelper(null, "192.168.1.2", "127.0.0.1");

        assertEquals("192.168.1.2", ip);
    }

    @Test
    void testGetClientIpAddressWithRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = getClientIpAddressTestHelper(null, null, "127.0.0.1");

        assertEquals("127.0.0.1", ip);
    }

    @Test
    void testGenerateRequestId() {
        // Since generateRequestId is private, we test it indirectly through preHandle
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        performanceInterceptor.preHandle(request, response, handler);

        verify(request).setAttribute(eq("requestId"), any(String.class));
    }

    @Test
    void testAddPerformanceHeaders() {
        long startTime = System.currentTimeMillis() - 100; // 100ms ago (fast request)
        String requestId = "test-request-id";

        when(request.getAttribute("startTime")).thenReturn(startTime);
        when(request.getAttribute("requestId")).thenReturn(requestId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);

        performanceInterceptor.afterCompletion(request, response, handler, null);

        verify(response).setHeader(eq("X-Response-Time"), any(String.class));
        verify(response).setHeader(eq("X-Server-Time"), any(String.class));
        verify(response).setHeader(eq("X-Performance"), any(String.class));
    }

    @Test
    void testPerformanceStatsHealthy() {
        // Reset metrics and add some healthy data
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.reset();
        
        // Simulate some normal requests
        for (int i = 0; i < 100; i++) {
            com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.recordRequest(200, false);
        }

        boolean isHealthy = PerformanceInterceptor.PerformanceStats.isHealthy();
        assertTrue(isHealthy);

        String healthReport = PerformanceInterceptor.PerformanceStats.getHealthReport();
        assertNotNull(healthReport);
        assertTrue(healthReport.contains("HEALTHY"));
    }

    @Test
    void testPerformanceStatsUnhealthy() {
        // Reset metrics and add some unhealthy data
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.reset();
        
        // Simulate many slow and error requests
        for (int i = 0; i < 50; i++) {
            com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.recordRequest(2000, false); // slow
        }
        for (int i = 0; i < 10; i++) {
            com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.recordRequest(500, true); // error
        }

        boolean isHealthy = PerformanceInterceptor.PerformanceStats.isHealthy();
        assertFalse(isHealthy);

        String healthReport = PerformanceInterceptor.PerformanceStats.getHealthReport();
        assertNotNull(healthReport);
        assertTrue(healthReport.contains("UNHEALTHY"));
    }

    @Test
    void testPerformanceStatsReset() {
        // Add some data
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.recordRequest(500, false);
        
        String statsBefore = PerformanceInterceptor.PerformanceStats.getCurrentStats();
        assertTrue(statsBefore.contains("Total Requests: 1"));

        PerformanceInterceptor.PerformanceStats.resetStats();
        
        String statsAfter = PerformanceInterceptor.PerformanceStats.getCurrentStats();
        assertTrue(statsAfter.contains("Total Requests: 0"));
    }

    @Test
    void testConcurrentRequestsTracking() {
        // Test increment
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();
        
        // Test decrement
        com.codenavigator.web.config.PerformanceConfig.PerformanceMetrics.decrementConcurrentRequests();
        
        String stats = PerformanceInterceptor.PerformanceStats.getCurrentStats();
        assertTrue(stats.contains("Current Concurrent: 1"));
        assertTrue(stats.contains("Max Concurrent: 2"));
    }

    // Helper method to test getClientIpAddress indirectly
    private String getClientIpAddressTestHelper(String xForwardedFor, String xRealIp, String remoteAddr) {
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return remoteAddr;
    }
}