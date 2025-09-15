package com.codenavigator.core.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackages = "com.codenavigator.core.repository")
@EnableTransactionManagement
public class DatabaseOptimizationConfig {

    /**
     * Hibernate性能优化配置
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> {
            // ================================
            // 查询优化配置
            // ================================
            
            // 启用二级缓存
            properties.put("hibernate.cache.use_second_level_cache", "true");
            properties.put("hibernate.cache.use_query_cache", "true");
            properties.put("hibernate.cache.region.factory_class", 
                          "org.hibernate.cache.jcache.JCacheRegionFactory");
            
            // 配置缓存提供者（使用EHCache）
            properties.put("hibernate.javax.cache.provider", 
                          "org.ehcache.jsr107.EhcacheCachingProvider");
            
            // ================================
            // 批处理优化
            // ================================
            
            // 启用JDBC批处理
            properties.put("hibernate.jdbc.batch_size", "25");
            properties.put("hibernate.jdbc.batch_versioned_data", "true");
            properties.put("hibernate.order_inserts", "true");
            properties.put("hibernate.order_updates", "true");
            
            // 启用批量获取
            properties.put("hibernate.default_batch_fetch_size", "16");
            
            // ================================
            // 连接池优化
            // ================================
            
            // 语句缓存
            properties.put("hibernate.jdbc.use_streams_for_binary", "true");
            properties.put("hibernate.jdbc.use_get_generated_keys", "true");
            
            // ================================
            // 延迟加载优化
            // ================================
            
            // 启用延迟加载字节码增强
            properties.put("hibernate.bytecode.use_reflection_optimizer", "true");
            
            // ================================
            // 统计和监控
            // ================================
            
            // 启用统计信息收集（开发环境）
            properties.put("hibernate.generate_statistics", "true");
            properties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "100");
            
            // ================================
            // SQL优化
            // ================================
            
            // 启用SQL注释
            properties.put("hibernate.use_sql_comments", "true");
            
            // 禁用自动schema验证（生产环境）
            properties.put("hibernate.hbm2ddl.auto", "validate");
            
            // ================================
            // 其他性能优化
            // ================================
            
            // 减少元数据获取
            properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
            
            // 启用多租户支持的优化
            properties.put("hibernate.multiTenancy", "NONE");
            
            // 设置默认的fetch策略
            properties.put("hibernate.fetch.associations.policy", "select");
        };
    }

    /**
     * 自定义物理命名策略
     * 将Java驼峰命名转换为数据库下划线命名
     */
    @Bean
    public PhysicalNamingStrategyStandardImpl physicalNamingStrategy() {
        return new PhysicalNamingStrategyStandardImpl() {
            @Override
            public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
                return Identifier.toIdentifier(camelCaseToSnakeCase(name.getText()));
            }
            
            @Override
            public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
                return Identifier.toIdentifier(camelCaseToSnakeCase(name.getText()));
            }
            
            private String camelCaseToSnakeCase(String camelCase) {
                return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            }
        };
    }

    /**
     * 查询提示配置
     * 为常用查询提供性能提示
     */
    public static class QueryHints {
        
        // 查询提示常量
        public static final String CACHEABLE = "org.hibernate.cacheable";
        public static final String CACHE_REGION = "org.hibernate.cacheRegion";
        public static final String FETCH_SIZE = "org.hibernate.fetchSize";
        public static final String TIMEOUT = "org.hibernate.timeout";
        public static final String READ_ONLY = "org.hibernate.readOnly";
        
        /**
         * 获取缓存查询提示
         */
        public static Map<String, Object> getCacheableHints(String region) {
            Map<String, Object> hints = new HashMap<>();
            hints.put(CACHEABLE, true);
            if (region != null) {
                hints.put(CACHE_REGION, region);
            }
            return hints;
        }
        
        /**
         * 获取只读查询提示
         */
        public static Map<String, Object> getReadOnlyHints() {
            Map<String, Object> hints = new HashMap<>();
            hints.put(READ_ONLY, true);
            return hints;
        }
        
        /**
         * 获取批量查询提示
         */
        public static Map<String, Object> getBatchHints(int fetchSize) {
            Map<String, Object> hints = new HashMap<>();
            hints.put(FETCH_SIZE, fetchSize);
            return hints;
        }
        
        /**
         * 获取超时查询提示
         */
        public static Map<String, Object> getTimeoutHints(int timeoutSeconds) {
            Map<String, Object> hints = new HashMap<>();
            hints.put(TIMEOUT, timeoutSeconds);
            return hints;
        }
    }

    /**
     * 查询性能监控器
     */
    public static class QueryPerformanceMonitor {
        
        private static final int SLOW_QUERY_THRESHOLD_MS = 100;
        
        /**
         * 记录慢查询
         */
        public static void logSlowQuery(String query, long executionTime) {
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                System.err.printf("Slow Query Detected: %dms - %s%n", executionTime, query);
                // 这里可以集成到监控系统，如Micrometer、Prometheus等
            }
        }
        
        /**
         * 分析查询计划
         */
        public static void analyzeQueryPlan(String query) {
            // 这里可以实现查询计划分析逻辑
            // 例如检查是否使用了索引、是否有全表扫描等
        }
    }

    /**
     * 数据库连接池监控配置
     */
    public static class ConnectionPoolMonitoring {
        
        /**
         * 获取连接池监控指标
         */
        public static Map<String, Object> getPoolMetrics(DataSource dataSource) {
            Map<String, Object> metrics = new HashMap<>();
            
            // 这里可以根据使用的连接池（HikariCP、Tomcat等）获取具体指标
            // 例如：活跃连接数、空闲连接数、等待连接数等
            
            return metrics;
        }
    }

    /**
     * 缓存策略配置
     */
    public static class CacheStrategy {
        
        // 缓存区域定义
        public static final String USER_CACHE = "userCache";
        public static final String LEARNING_PATH_CACHE = "learningPathCache";
        public static final String PROGRESS_CACHE = "progressCache";
        public static final String QUERY_CACHE = "queryCache";
        
        /**
         * 获取实体缓存配置
         */
        public static Map<String, String> getEntityCacheConfig() {
            Map<String, String> config = new HashMap<>();
            config.put("com.codenavigator.core.entity.User", USER_CACHE);
            config.put("com.codenavigator.core.entity.LearningPath", LEARNING_PATH_CACHE);
            config.put("com.codenavigator.core.entity.UserProgress", PROGRESS_CACHE);
            return config;
        }
    }

    /**
     * 索引使用情况分析器
     */
    public static class IndexAnalyzer {
        
        /**
         * 分析查询是否使用了合适的索引
         */
        public static boolean isQueryOptimized(String query) {
            // 这里可以实现查询分析逻辑
            // 例如检查WHERE子句中的字段是否有索引
            // 检查JOIN条件是否使用了外键索引等
            
            // 简单的启发式检查
            String lowerQuery = query.toLowerCase();
            
            // 检查是否有LIMIT子句（分页查询）
            boolean hasLimit = lowerQuery.contains("limit");
            
            // 检查是否有ORDER BY子句
            boolean hasOrderBy = lowerQuery.contains("order by");
            
            // 检查是否有索引友好的WHERE条件
            boolean hasIndexFriendlyWhere = lowerQuery.contains("where") && 
                                          (lowerQuery.contains("id =") || 
                                           lowerQuery.contains("username =") ||
                                           lowerQuery.contains("email ="));
            
            return hasIndexFriendlyWhere || (hasLimit && hasOrderBy);
        }
        
        /**
         * 获取索引建议
         */
        public static String getIndexSuggestion(String query) {
            // 这里可以实现索引建议逻辑
            // 分析查询中的WHERE、ORDER BY、JOIN条件
            // 提供索引创建建议
            
            return "Consider adding indexes on frequently queried columns";
        }
    }
}