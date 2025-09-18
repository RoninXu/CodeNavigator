package com.codenavigator.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Redis模板配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON序列化配置
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                          ObjectMapper.DefaultTyping.NON_FINAL);
        // 注册JSR310时间模块，支持LocalDateTime等Java 8时间类型
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
            new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // String序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器配置
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认1小时过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)))
                .disableCachingNullValues(); // 不缓存null值

        // 不同缓存区域的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户信息缓存：30分钟过期
        cacheConfigurations.put(CacheNames.USER_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 学习路径缓存：2小时过期
        cacheConfigurations.put(CacheNames.LEARNING_PATH_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofHours(2)));
        
        // 用户进度缓存：15分钟过期
        cacheConfigurations.put(CacheNames.USER_PROGRESS_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 代码分析结果缓存：6小时过期
        cacheConfigurations.put(CacheNames.CODE_ANALYSIS_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofHours(6)));
        
        // 对话会话缓存：1小时过期
        cacheConfigurations.put(CacheNames.CONVERSATION_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 学习统计缓存：30分钟过期
        cacheConfigurations.put(CacheNames.STATISTICS_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 热门内容缓存：1小时过期
        cacheConfigurations.put(CacheNames.HOT_CONTENT_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 排行榜缓存：5分钟过期
        cacheConfigurations.put(CacheNames.LEADERBOARD_CACHE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 缓存名称常量
     */
    public static class CacheNames {
        public static final String USER_CACHE = "user";
        public static final String LEARNING_PATH_CACHE = "learningPath";
        public static final String USER_PROGRESS_CACHE = "userProgress";
        public static final String CODE_ANALYSIS_CACHE = "codeAnalysis";
        public static final String CONVERSATION_CACHE = "conversation";
        public static final String STATISTICS_CACHE = "statistics";
        public static final String HOT_CONTENT_CACHE = "hotContent";
        public static final String LEADERBOARD_CACHE = "leaderboard";
    }

    /**
     * 缓存键生成器
     */
    public static class CacheKeyGenerator {
        
        private static final String SEPARATOR = ":";
        
        /**
         * 生成用户缓存键
         */
        public static String userKey(Long userId) {
            return CacheNames.USER_CACHE + SEPARATOR + userId;
        }
        
        /**
         * 生成学习路径缓存键
         */
        public static String learningPathKey(String pathId) {
            return CacheNames.LEARNING_PATH_CACHE + SEPARATOR + pathId;
        }
        
        /**
         * 生成用户进度缓存键
         */
        public static String userProgressKey(Long userId, String pathId) {
            return CacheNames.USER_PROGRESS_CACHE + SEPARATOR + userId + SEPARATOR + pathId;
        }
        
        /**
         * 生成代码分析缓存键
         */
        public static String codeAnalysisKey(Long userId, String codeHash) {
            return CacheNames.CODE_ANALYSIS_CACHE + SEPARATOR + userId + SEPARATOR + codeHash;
        }
        
        /**
         * 生成对话会话缓存键
         */
        public static String conversationKey(String sessionId) {
            return CacheNames.CONVERSATION_CACHE + SEPARATOR + sessionId;
        }
        
        /**
         * 生成统计数据缓存键
         */
        public static String statisticsKey(String type, String period) {
            return CacheNames.STATISTICS_CACHE + SEPARATOR + type + SEPARATOR + period;
        }
        
        /**
         * 生成热门内容缓存键
         */
        public static String hotContentKey(String contentType, String category) {
            return CacheNames.HOT_CONTENT_CACHE + SEPARATOR + contentType + SEPARATOR + category;
        }
        
        /**
         * 生成排行榜缓存键
         */
        public static String leaderboardKey(String type, int limit) {
            return CacheNames.LEADERBOARD_CACHE + SEPARATOR + type + SEPARATOR + limit;
        }
    }

    /**
     * 缓存配置常量
     */
    public static class CacheConfig {
        
        // 缓存过期时间（秒）
        public static final int USER_EXPIRE_TIME = 30 * 60; // 30分钟
        public static final int LEARNING_PATH_EXPIRE_TIME = 2 * 60 * 60; // 2小时
        public static final int USER_PROGRESS_EXPIRE_TIME = 15 * 60; // 15分钟
        public static final int CODE_ANALYSIS_EXPIRE_TIME = 6 * 60 * 60; // 6小时
        public static final int CONVERSATION_EXPIRE_TIME = 60 * 60; // 1小时
        public static final int STATISTICS_EXPIRE_TIME = 30 * 60; // 30分钟
        public static final int HOT_CONTENT_EXPIRE_TIME = 60 * 60; // 1小时
        public static final int LEADERBOARD_EXPIRE_TIME = 5 * 60; // 5分钟
        
        // 缓存预热配置
        public static final boolean ENABLE_CACHE_WARMUP = true;
        public static final int WARMUP_BATCH_SIZE = 100;
        
        // 缓存监控配置
        public static final boolean ENABLE_CACHE_METRICS = true;
        public static final int METRICS_COLLECTION_INTERVAL = 60; // 秒
    }
}