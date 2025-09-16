package com.codenavigator.web.interceptor;

import com.codenavigator.web.config.PerformanceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "startTime";
    private static final String REQUEST_ID_ATTR = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        
        request.setAttribute(START_TIME_ATTR, startTime);
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        
        // 增加并发请求计数
        PerformanceConfig.PerformanceMetrics.incrementConcurrentRequests();
        
        // 记录请求开始
        log.debug("Request started: {} {} [{}]", 
                 request.getMethod(), request.getRequestURI(), requestId);
        
        // 检查并发限制
        if (isOverConcurrencyLimit()) {
            log.warn("Too many concurrent requests, rejecting request: {}", requestId);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader("Retry-After", "30");
            return false;
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        // 在控制器处理完成后执行，可以在这里添加额外的性能监控
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
        log.debug("Request post-processed: [{}]", requestId);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 减少并发请求计数
        PerformanceConfig.PerformanceMetrics.decrementConcurrentRequests();
        
        // 记录性能指标
        boolean isError = ex != null || response.getStatus() >= 400;
        PerformanceConfig.PerformanceMetrics.recordRequest(duration, isError);
        
        // 记录请求完成信息
        String logLevel = determineLogLevel(duration, isError);
        String message = String.format(
            "Request completed: {} {} [{}] - {}ms - Status: {} - Error: {}",
            request.getMethod(), request.getRequestURI(), requestId, 
            duration, response.getStatus(), isError
        );
        
        switch (logLevel) {
            case "ERROR":
                log.error(message);
                break;
            case "WARN":
                log.warn(message);
                break;
            case "INFO":
                log.info(message);
                break;
            default:
                log.debug(message);
        }
        
        // 慢请求告警
        if (duration > PerformanceConfig.PerformanceConstants.SLOW_REQUEST_THRESHOLD) {
            handleSlowRequest(request, duration, requestId);
        }
        
        // 异常请求处理
        if (ex != null) {
            handleErrorRequest(request, ex, requestId);
        }
        
        // 添加性能相关的响应头
        addPerformanceHeaders(response, duration);
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return String.valueOf(System.currentTimeMillis()) + "-" + 
               String.valueOf(Thread.currentThread().getId());
    }

    /**
     * 检查是否超过并发限制
     */
    private boolean isOverConcurrencyLimit() {
        // 简单的并发限制检查
        return false; // 这里可以实现更复杂的限流逻辑
    }

    /**
     * 确定日志级别
     */
    private String determineLogLevel(long duration, boolean isError) {
        if (isError) {
            return "ERROR";
        } else if (duration > PerformanceConfig.PerformanceConstants.VERY_SLOW_REQUEST_THRESHOLD) {
            return "WARN";
        } else if (duration > PerformanceConfig.PerformanceConstants.SLOW_REQUEST_THRESHOLD) {
            return "INFO";
        } else {
            return "DEBUG";
        }
    }

    /**
     * 处理慢请求
     */
    private void handleSlowRequest(HttpServletRequest request, long duration, String requestId) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = getClientIpAddress(request);
        
        log.warn("Slow request detected: {} {} [{}] - {}ms from {} ({})", 
                method, uri, requestId, duration, remoteAddr, userAgent);
        
        // 这里可以发送告警通知或记录到监控系统
        recordSlowRequestMetrics(uri, method, duration);
    }

    /**
     * 处理错误请求
     */
    private void handleErrorRequest(HttpServletRequest request, Exception ex, String requestId) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = getClientIpAddress(request);
        
        log.error("Request error: {} {} [{}] from {} - {}", 
                 method, uri, requestId, remoteAddr, ex.getMessage(), ex);
        
        // 这里可以发送错误告警或记录到错误监控系统
        recordErrorMetrics(uri, method, ex.getClass().getSimpleName());
    }

    /**
     * 添加性能相关的响应头
     */
    private void addPerformanceHeaders(HttpServletResponse response, long duration) {
        response.setHeader("X-Response-Time", String.valueOf(duration));
        response.setHeader("X-Server-Time", String.valueOf(System.currentTimeMillis()));
        
        // 添加缓存控制头
        if (duration < PerformanceConfig.PerformanceConstants.SLOW_REQUEST_THRESHOLD / 2) {
            response.setHeader("X-Performance", "fast");
        } else if (duration < PerformanceConfig.PerformanceConstants.SLOW_REQUEST_THRESHOLD) {
            response.setHeader("X-Performance", "normal");
        } else {
            response.setHeader("X-Performance", "slow");
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 记录慢请求指标
     */
    private void recordSlowRequestMetrics(String uri, String method, long duration) {
        // 这里可以集成到监控系统，如Micrometer、Prometheus等
        log.debug("Recording slow request metrics: {} {} - {}ms", method, uri, duration);
    }

    /**
     * 记录错误指标
     */
    private void recordErrorMetrics(String uri, String method, String errorType) {
        // 这里可以集成到错误监控系统
        log.debug("Recording error metrics: {} {} - {}", method, uri, errorType);
    }

    /**
     * 性能统计数据
     */
    public static class PerformanceStats {
        
        /**
         * 获取当前性能统计
         */
        public static String getCurrentStats() {
            return PerformanceConfig.PerformanceMetrics.getMetricsSummary();
        }
        
        /**
         * 重置统计数据
         */
        public static void resetStats() {
            PerformanceConfig.PerformanceMetrics.reset();
            log.info("Performance statistics reset");
        }
        
        /**
         * 检查系统性能健康状态
         */
        public static boolean isHealthy() {
            double errorRate = PerformanceConfig.PerformanceMetrics.getErrorRate();
            double slowRequestRate = PerformanceConfig.PerformanceMetrics.getSlowRequestRate();
            double avgResponseTime = PerformanceConfig.PerformanceMetrics.getAverageResponseTime();
            
            // 定义健康阈值
            boolean healthyErrorRate = errorRate < 5.0; // 错误率小于5%
            boolean healthySlowRate = slowRequestRate < 10.0; // 慢请求率小于10%
            boolean healthyAvgTime = avgResponseTime < 500.0; // 平均响应时间小于500ms
            
            return healthyErrorRate && healthySlowRate && healthyAvgTime;
        }
        
        /**
         * 获取性能健康报告
         */
        public static String getHealthReport() {
            boolean isHealthy = isHealthy();
            String stats = getCurrentStats();
            String status = isHealthy ? "HEALTHY" : "UNHEALTHY";
            
            return String.format("System Performance Status: %s\n%s", status, stats);
        }
    }
}