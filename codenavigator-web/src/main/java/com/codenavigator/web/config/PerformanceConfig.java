package com.codenavigator.web.config;

import com.codenavigator.web.interceptor.PerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.core.task.AsyncTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class PerformanceConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    /**
     * 添加性能监控拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico");
    }

    /**
     * 配置异步支持
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncTaskExecutor());
        configurer.setDefaultTimeout(30000); // 30秒超时
    }

    /**
     * 异步任务执行器
     */
    @Bean("asyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(8);
        
        // 最大线程数
        executor.setMaxPoolSize(20);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名前缀
        executor.setThreadNamePrefix("CodeNavigator-Async-");
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }

    /**
     * 批量处理任务执行器
     */
    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 批量处理使用更多线程
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("CodeNavigator-Batch-");
        executor.setKeepAliveSeconds(120);
        
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * AI处理任务执行器
     */
    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // AI处理通常IO密集，使用较少线程
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("CodeNavigator-AI-");
        executor.setKeepAliveSeconds(300); // AI任务可能较长，保持线程更久
        
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.initialize();
        return executor;
    }

    /**
     * 性能监控配置常量
     */
    public static class PerformanceConstants {
        
        // 响应时间阈值（毫秒）
        public static final long SLOW_REQUEST_THRESHOLD = 1000; // 1秒
        public static final long VERY_SLOW_REQUEST_THRESHOLD = 3000; // 3秒
        
        // 并发限制
        public static final int MAX_CONCURRENT_REQUESTS = 100;
        public static final int MAX_REQUESTS_PER_MINUTE = 1000;
        
        // 缓存配置
        public static final int CACHE_PRELOAD_SIZE = 50;
        public static final int CACHE_WARMUP_DELAY = 30; // 秒
        
        // 异步处理阈值
        public static final long ASYNC_PROCESSING_THRESHOLD = 500; // 500ms以上的任务异步处理
        
        // 批量处理大小
        public static final int BATCH_SIZE = 20;
        public static final int MAX_BATCH_SIZE = 100;
        
        // 超时配置
        public static final int API_TIMEOUT = 30000; // 30秒
        public static final int AI_PROCESSING_TIMEOUT = 120000; // 2分钟
        public static final int DATABASE_QUERY_TIMEOUT = 10000; // 10秒
    }

    /**
     * API性能指标
     */
    public static class PerformanceMetrics {
        
        // 请求计数器
        private static volatile long totalRequests = 0;
        private static volatile long slowRequests = 0;
        private static volatile long errorRequests = 0;
        
        // 响应时间统计
        private static volatile long totalResponseTime = 0;
        private static volatile long maxResponseTime = 0;
        private static volatile long minResponseTime = Long.MAX_VALUE;
        
        // 并发统计
        private static volatile int currentConcurrentRequests = 0;
        private static volatile int maxConcurrentRequests = 0;
        
        public static synchronized void recordRequest(long responseTime, boolean isError) {
            totalRequests++;
            totalResponseTime += responseTime;
            
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            
            if (responseTime > PerformanceConstants.SLOW_REQUEST_THRESHOLD) {
                slowRequests++;
            }
            
            if (isError) {
                errorRequests++;
            }
        }
        
        public static synchronized void incrementConcurrentRequests() {
            currentConcurrentRequests++;
            if (currentConcurrentRequests > maxConcurrentRequests) {
                maxConcurrentRequests = currentConcurrentRequests;
            }
        }
        
        public static synchronized void decrementConcurrentRequests() {
            if (currentConcurrentRequests > 0) {
                currentConcurrentRequests--;
            }
        }
        
        public static synchronized double getAverageResponseTime() {
            return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0;
        }
        
        public static synchronized double getSlowRequestRate() {
            return totalRequests > 0 ? (double) slowRequests / totalRequests * 100 : 0;
        }
        
        public static synchronized double getErrorRate() {
            return totalRequests > 0 ? (double) errorRequests / totalRequests * 100 : 0;
        }
        
        public static synchronized String getMetricsSummary() {
            return String.format(
                "Total Requests: %d, Average Response Time: %.2fms, " +
                "Slow Request Rate: %.2f%%, Error Rate: %.2f%%, " +
                "Current Concurrent: %d, Max Concurrent: %d",
                totalRequests, getAverageResponseTime(), getSlowRequestRate(),
                getErrorRate(), currentConcurrentRequests, maxConcurrentRequests
            );
        }
        
        public static synchronized void reset() {
            totalRequests = 0;
            slowRequests = 0;
            errorRequests = 0;
            totalResponseTime = 0;
            maxResponseTime = 0;
            minResponseTime = Long.MAX_VALUE;
            currentConcurrentRequests = 0;
            maxConcurrentRequests = 0;
        }
    }
}