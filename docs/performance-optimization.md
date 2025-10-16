# CodeNavigator 性能优化建议文档

**文档版本**: 1.0
**更新日期**: 2025-10-16
**适用版本**: CodeNavigator V1.0+

---

## 📋 目录

1. [概述](#概述)
2. [JVM性能优化](#jvm性能优化)
3. [数据库性能优化](#数据库性能优化)
4. [缓存优化策略](#缓存优化策略)
5. [网络和传输优化](#网络和传输优化)
6. [AI模型调用优化](#ai模型调用优化)
7. [应用层优化](#应用层优化)
8. [监控和诊断](#监控和诊断)
9. [性能测试](#性能测试)
10. [常见性能问题](#常见性能问题)

---

## 🎯 概述

### 优化目标

| 指标 | 当前基线 | 优化目标 | 说明 |
|------|----------|----------|------|
| API响应时间 | 2-3s | < 500ms | 非AI接口响应时间 |
| AI对话响应 | 5-10s | < 5s | AI模型调用响应时间 |
| 并发用户数 | 100 | 500+ | 单实例支持并发数 |
| 数据库查询 | 100-500ms | < 100ms | 常规查询响应时间 |
| 内存使用 | 2-4GB | < 2GB | JVM堆内存使用 |
| CPU使用率 | 50-70% | < 50% | 平均CPU使用率 |

### 优化原则

1. **先监控，后优化**: 基于实际性能数据进行优化
2. **关注瓶颈**: 优先解决最严重的性能瓶颈
3. **测试驱动**: 每次优化后进行性能测试验证
4. **渐进式优化**: 逐步优化，避免过度优化
5. **权衡取舍**: 平衡性能、可维护性和开发成本

---

## ☕ JVM性能优化

### 1. 堆内存配置

#### 基本配置

```bash
# 开发环境 (8GB机器)
JAVA_OPTS="-Xms512m -Xmx2g"

# 生产环境 (16GB机器)
JAVA_OPTS="-Xms2g -Xmx4g"

# 生产环境 (32GB机器)
JAVA_OPTS="-Xms4g -Xmx8g"
```

**配置建议**:
- **-Xms**: 设置为-Xmx的50-75%，减少堆扩展开销
- **-Xmx**: 不超过物理内存的50%，为操作系统和其他进程留空间
- **堆大小**: 根据应用实际使用量设置，通过监控调整

#### 垃圾回收器选择

**G1GC (推荐)**:
```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40"
```

**优势**:
- 低延迟，适合Web应用
- 自动调节停顿时间
- 适用于大堆内存 (> 6GB)

**ZGC (大内存场景)**:
```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+UseZGC \
  -XX:ZCollectionInterval=120 \
  -XX:ZAllocationSpikeTolerance=5"
```

**优势**:
- 超低延迟 (< 10ms)
- 适用于超大堆 (> 16GB)
- 停顿时间不随堆大小增加

### 2. GC日志和监控

#### 启用GC日志

```bash
JAVA_OPTS="$JAVA_OPTS \
  -Xlog:gc*:file=/var/log/codenavigator/gc.log:time,uptime,level,tags \
  -Xlog:gc*:file=/var/log/codenavigator/gc-%t.log:time,uptime,level,tags:filecount=5,filesize=100M"
```

#### GC监控指标

**关键指标**:
- **Minor GC频率**: 建议 < 1次/分钟
- **Full GC频率**: 建议 < 1次/小时
- **GC停顿时间**: 建议 < 200ms
- **堆使用率**: 建议 < 80%

### 3. 其他JVM优化

#### 字符串优化

```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+UseStringDeduplication \
  -XX:StringDeduplicationAgeThreshold=3"
```

**作用**: 去除重复字符串，节省内存

#### 堆外内存

```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:MaxDirectMemorySize=1g \
  -XX:MetaspaceSize=256m \
  -XX:MaxMetaspaceSize=512m"
```

#### OOM处理

```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/codenavigator/heapdump \
  -XX:+ExitOnOutOfMemoryError"
```

### 4. JVM性能调优示例

**完整的生产环境JVM配置**:

```bash
#!/bin/bash
# 16GB内存服务器配置

JAVA_OPTS=""

# 堆内存配置
JAVA_OPTS="$JAVA_OPTS -Xms4g -Xmx8g"

# G1GC配置
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:G1HeapRegionSize=16m"
JAVA_OPTS="$JAVA_OPTS -XX:InitiatingHeapOccupancyPercent=45"
JAVA_OPTS="$JAVA_OPTS -XX:G1NewSizePercent=30"
JAVA_OPTS="$JAVA_OPTS -XX:G1MaxNewSizePercent=40"
JAVA_OPTS="$JAVA_OPTS -XX:G1MixedGCLiveThresholdPercent=85"
JAVA_OPTS="$JAVA_OPTS -XX:G1MixedGCCountTarget=8"

# 字符串优化
JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"

# 堆外内存
JAVA_OPTS="$JAVA_OPTS -XX:MaxDirectMemorySize=1g"
JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=256m"
JAVA_OPTS="$JAVA_OPTS -XX:MaxMetaspaceSize=512m"

# GC日志
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/var/log/codenavigator/gc.log:time,uptime,level,tags:filecount=5,filesize=100M"

# OOM处理
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=/var/log/codenavigator/"

# JMX监控
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=9999"
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"

export JAVA_OPTS
```

---

## 🗄️ 数据库性能优化

### 1. MySQL配置优化

#### InnoDB缓冲池

```ini
[mysqld]
# 设置为物理内存的50-70%
innodb_buffer_pool_size = 8G

# 多个缓冲池实例，提高并发
innodb_buffer_pool_instances = 8

# 预热缓冲池
innodb_buffer_pool_dump_at_shutdown = 1
innodb_buffer_pool_load_at_startup = 1
```

**作用**: 缓存数据和索引，减少磁盘I/O

#### 连接配置

```ini
# 最大连接数
max_connections = 1000

# 最大连接错误
max_connect_errors = 100000

# 连接超时
wait_timeout = 600
interactive_timeout = 600
```

#### 日志配置

```ini
# 重做日志大小
innodb_log_file_size = 512M
innodb_log_files_in_group = 3

# 日志缓冲
innodb_log_buffer_size = 64M

# 刷新策略 (性能优先)
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
```

**innodb_flush_log_at_trx_commit**:
- `0`: 最高性能，可能丢失1秒数据
- `1`: 最高安全性，每次提交写盘
- `2`: 平衡选择，每次提交写OS缓存

#### 查询缓存

```ini
# 查询缓存 (MySQL 5.7)
query_cache_type = 1
query_cache_size = 256M
query_cache_limit = 2M

# MySQL 8.0已移除查询缓存，使用应用层缓存
```

### 2. 索引优化

#### 索引设计原则

**创建索引的场景**:
1. WHERE子句中频繁使用的列
2. ORDER BY和GROUP BY的列
3. 关联查询的JOIN列
4. 唯一约束列

**避免索引的场景**:
1. 经常更新的列
2. 区分度低的列 (如性别)
3. 很少在查询中使用的列

#### 复合索引示例

```sql
-- 用户查询优化
CREATE INDEX idx_user_status_level ON users(status, level, created_at);

-- 学习进度查询优化
CREATE INDEX idx_progress_composite ON user_progress(
    user_id,
    learning_path_id,
    status,
    updated_at
);

-- 对话记录查询优化
CREATE INDEX idx_conversation_user_time ON conversations(
    user_id,
    created_at
);
```

**复合索引顺序**:
1. 等值查询的列在前
2. 范围查询的列在后
3. 区分度高的列在前

#### 索引监控

```sql
-- 查看表索引
SHOW INDEX FROM users;

-- 检查未使用的索引
SELECT
    object_schema,
    object_name,
    index_name
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE index_name IS NOT NULL
  AND count_star = 0
  AND object_schema = 'code_navigator'
ORDER BY object_schema, object_name;

-- 检查重复索引
SELECT
    a.TABLE_SCHEMA,
    a.TABLE_NAME,
    a.INDEX_NAME,
    GROUP_CONCAT(a.COLUMN_NAME ORDER BY a.SEQ_IN_INDEX) AS columns,
    COUNT(*) AS duplicate_indexes
FROM information_schema.STATISTICS a
JOIN information_schema.STATISTICS b
  ON a.TABLE_SCHEMA = b.TABLE_SCHEMA
  AND a.TABLE_NAME = b.TABLE_NAME
  AND a.INDEX_NAME != b.INDEX_NAME
  AND a.SEQ_IN_INDEX = b.SEQ_IN_INDEX
  AND a.COLUMN_NAME = b.COLUMN_NAME
WHERE a.TABLE_SCHEMA = 'code_navigator'
GROUP BY a.TABLE_SCHEMA, a.TABLE_NAME, a.INDEX_NAME
HAVING COUNT(*) > 1;
```

### 3. 查询优化

#### 慢查询分析

```sql
-- 启用慢查询日志
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 1;
SET GLOBAL log_queries_not_using_indexes = 1;

-- 分析慢查询
SELECT
    SUBSTRING(sql_text, 1, 100) AS query,
    count_star AS exec_count,
    avg_timer_wait / 1000000000000 AS avg_time_sec,
    sum_rows_examined AS total_rows_scanned
FROM performance_schema.events_statements_summary_by_digest
WHERE schema_name = 'code_navigator'
ORDER BY avg_timer_wait DESC
LIMIT 10;
```

#### 查询优化技巧

**使用EXPLAIN分析**:
```sql
EXPLAIN SELECT * FROM users WHERE email = 'user@example.com';
EXPLAIN FORMAT=JSON SELECT * FROM user_progress WHERE user_id = 1;
```

**优化前**:
```sql
-- 不使用索引的查询
SELECT * FROM users
WHERE LOWER(email) = 'user@example.com';
```

**优化后**:
```sql
-- 使用索引
SELECT * FROM users
WHERE email = 'user@example.com';

-- 如果必须不区分大小写，使用生成列
ALTER TABLE users ADD COLUMN email_lower VARCHAR(255)
GENERATED ALWAYS AS (LOWER(email)) STORED;
CREATE INDEX idx_email_lower ON users(email_lower);
```

**避免SELECT ***:
```sql
-- 不推荐
SELECT * FROM users WHERE id = 1;

-- 推荐
SELECT id, username, email, level FROM users WHERE id = 1;
```

**分页优化**:
```sql
-- 不推荐 (大偏移量慢)
SELECT * FROM learning_paths LIMIT 10000, 20;

-- 推荐 (使用子查询)
SELECT * FROM learning_paths
WHERE id > (
    SELECT id FROM learning_paths
    ORDER BY id
    LIMIT 10000, 1
)
ORDER BY id
LIMIT 20;
```

### 4. HikariCP连接池优化

#### 最佳配置

```yaml
spring:
  datasource:
    hikari:
      # 最大连接数 = ((核心数 * 2) + 磁盘数)
      maximum-pool-size: 50

      # 最小空闲连接
      minimum-idle: 10

      # 空闲超时 (10分钟)
      idle-timeout: 600000

      # 连接最大存活时间 (30分钟)
      max-lifetime: 1800000

      # 连接超时 (30秒)
      connection-timeout: 30000

      # 泄漏检测阈值 (1分钟)
      leak-detection-threshold: 60000

      # 连接测试查询
      connection-test-query: SELECT 1
```

**连接池大小计算**:
```
optimal_pool_size = ((core_count * 2) + effective_spindle_count)

例如: 4核CPU + 2个磁盘 = (4 * 2) + 2 = 10
推荐连接数: 10-20
```

### 5. 数据库维护

#### 定期优化表

```bash
#!/bin/bash
# optimize_tables.sh

TABLES="users learning_paths learning_modules user_progress conversations learning_notes"

for TABLE in $TABLES; do
    echo "Optimizing table: $TABLE"
    mysql -u root -p$MYSQL_ROOT_PASSWORD code_navigator -e "OPTIMIZE TABLE $TABLE;"
done
```

#### 统计信息更新

```sql
-- 更新表统计信息
ANALYZE TABLE users;
ANALYZE TABLE learning_paths;
ANALYZE TABLE user_progress;
ANALYZE TABLE conversations;

-- 自动更新统计信息
SET GLOBAL innodb_stats_auto_recalc = 1;
```

---

## 🚀 缓存优化策略

### 1. Redis配置优化

#### 内存配置

```redis
# redis.conf

# 最大内存限制
maxmemory 4gb

# 内存淘汰策略
maxmemory-policy allkeys-lru

# LRU采样数
maxmemory-samples 5
```

**淘汰策略选择**:
- `allkeys-lru`: 最近最少使用 (推荐)
- `volatile-lru`: 只淘汰设置了过期时间的key
- `allkeys-random`: 随机淘汰
- `volatile-ttl`: 淘汰最早过期的key

#### 持久化配置

```redis
# RDB持久化 (性能优先)
save 900 1
save 300 10
save 60 10000

# AOF持久化 (可靠性优先)
appendonly yes
appendfsync everysec

# 混合持久化 (推荐)
aof-use-rdb-preamble yes
```

#### 网络优化

```redis
# TCP keepalive
tcp-keepalive 300

# 超时设置
timeout 0

# TCP backlog
tcp-backlog 511
```

### 2. 多级缓存策略

#### 缓存架构

```
┌─────────────┐
│   请求      │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  本地缓存    │ Caffeine (L1)
│  (100ms)    │
└──────┬──────┘
       │ Miss
       ▼
┌─────────────┐
│  Redis缓存   │ (L2)
│  (1-5ms)    │
└──────┬──────┘
       │ Miss
       ▼
┌─────────────┐
│   数据库     │
│  (100ms+)   │
└─────────────┘
```

#### Spring Cache配置

```yaml
spring:
  cache:
    type: redis
    redis:
      # 缓存过期时间 (1小时)
      time-to-live: 3600000

      # 不缓存null值
      cache-null-values: false

      # 键前缀
      key-prefix: "codenavigator:"
      use-key-prefix: true
```

#### 本地缓存 + Redis缓存

```java
@Configuration
public class CacheConfig {

    /**
     * 本地缓存配置 (Caffeine)
     */
    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }

    /**
     * Redis缓存配置
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

### 3. 缓存使用最佳实践

#### 缓存注解使用

```java
@Service
public class UserService {

    /**
     * 查询缓存
     */
    @Cacheable(value = "users", key = "#userId",
               unless = "#result == null")
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElse(null);
    }

    /**
     * 更新缓存
     */
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 删除缓存
     */
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * 批量删除缓存
     */
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsers() {
        // 清除所有用户缓存
    }
}
```

#### 缓存key设计

**命名规范**:
```
codenavigator:{module}:{entity}:{id}

示例:
codenavigator:user:profile:123
codenavigator:path:detail:456
codenavigator:conversation:history:789
```

**复杂key示例**:
```java
// 使用SpEL表达式
@Cacheable(value = "learningPaths",
           key = "#userId + ':' + #level + ':' + #framework")
public List<LearningPath> getUserRecommendedPaths(
    Long userId, String level, String framework) {
    // ...
}
```

### 4. 缓存预热和更新

#### 缓存预热

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private UserService userService;

    @Autowired
    private LearningPathService pathService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Starting cache warming...");

        // 预加载热点用户
        List<Long> hotUserIds = userService.getHotUserIds();
        hotUserIds.forEach(userService::getUserById);

        // 预加载热门学习路径
        List<Long> popularPathIds = pathService.getPopularPathIds();
        popularPathIds.forEach(pathService::getPathById);

        log.info("Cache warming completed");
    }
}
```

#### 定时刷新缓存

```java
@Component
public class CacheRefreshScheduler {

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void refreshCache() {
        log.info("Refreshing cache...");

        // 清除过期缓存
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // 重新加载热点数据
        warmUpHotData();

        log.info("Cache refresh completed");
    }
}
```

### 5. 缓存监控

#### Redis监控

```bash
# 查看Redis信息
redis-cli INFO memory
redis-cli INFO stats

# 监控命中率
redis-cli INFO stats | grep keyspace
```

**关键指标**:
- 命中率: keyspace_hits / (keyspace_hits + keyspace_misses) > 90%
- 内存使用率: used_memory / maxmemory < 80%
- 连接数: connected_clients < max_clients * 80%

#### 缓存统计

```java
@RestController
@RequestMapping("/admin/cache")
public class CacheMonitorController {

    @Autowired
    private CacheManager cacheManager;

    @GetMapping("/stats")
    public Map<String, CacheStats> getCacheStats() {
        Map<String, CacheStats> stats = new HashMap<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                    ((CaffeineCache) cache).getNativeCache();

                CacheStats cacheStats = nativeCache.stats();
                stats.put(cacheName, cacheStats);
            }
        });

        return stats;
    }
}
```

---

## 🌐 网络和传输优化

### 1. Nginx优化

#### 基本配置

```nginx
# nginx.conf

# 工作进程数 = CPU核心数
worker_processes auto;

# 每个进程最大连接数
events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    # 文件传输优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;

    # 超时设置
    keepalive_timeout 65;
    keepalive_requests 100;

    # 客户端缓冲
    client_body_buffer_size 128k;
    client_max_body_size 10m;
    client_header_buffer_size 1k;
    large_client_header_buffers 4 4k;
}
```

#### Gzip压缩

```nginx
# 启用Gzip压缩
gzip on;
gzip_vary on;
gzip_min_length 1000;
gzip_comp_level 6;
gzip_types
    text/plain
    text/css
    text/xml
    text/javascript
    application/json
    application/javascript
    application/xml+rss
    application/rss+xml
    font/truetype
    font/opentype
    application/vnd.ms-fontobject
    image/svg+xml;
gzip_disable "msie6";
```

#### 静态资源缓存

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
    access_log off;
}
```

#### 反向代理优化

```nginx
upstream codenavigator_backend {
    server app1:8080 weight=3 max_fails=3 fail_timeout=30s;
    server app2:8080 weight=2 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

location / {
    proxy_pass http://codenavigator_backend;

    # 代理缓冲
    proxy_buffering on;
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;
    proxy_busy_buffers_size 8k;

    # 超时设置
    proxy_connect_timeout 30s;
    proxy_send_timeout 30s;
    proxy_read_timeout 30s;

    # HTTP头优化
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

### 2. Spring Boot Tomcat优化

#### 线程池配置

```yaml
server:
  tomcat:
    threads:
      # 最小空闲线程
      min-spare: 20

      # 最大工作线程
      max: 200

    # 最大连接数
    max-connections: 10000

    # 等待队列长度
    accept-count: 100

    # 连接超时
    connection-timeout: 20000

    # Keep-Alive配置
    keep-alive-timeout: 60000
    max-keep-alive-requests: 100
```

**线程数计算**:
```
IO密集型应用: threads = CPU核心数 * 2
CPU密集型应用: threads = CPU核心数 + 1
混合型应用: threads = CPU核心数 * 2 ~ CPU核心数 * 4
```

#### HTTP/2支持

```yaml
server:
  http2:
    enabled: true
  ssl:
    enabled: true
    key-store: classpath:ssl/keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

#### 响应压缩

```yaml
server:
  compression:
    enabled: true
    mime-types:
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
      - application/javascript
      - application/json
      - application/xml
    min-response-size: 1024
```

### 3. 数据传输优化

#### JSON序列化优化

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 仅序列化非null字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 忽略未知属性
        mapper.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 日期格式
        mapper.configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        return mapper;
    }
}
```

#### 分页和限流

```java
@RestController
public class ApiController {

    /**
     * 分页查询
     */
    @GetMapping("/api/users")
    public Page<User> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        // 限制每页最大数量
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size);
        return userService.findAll(pageable);
    }

    /**
     * 限流注解
     */
    @RateLimiter(name = "api", fallbackMethod = "apiFallback")
    @GetMapping("/api/conversations")
    public List<Conversation> getConversations() {
        return conversationService.getRecentConversations();
    }
}
```

---

## 🤖 AI模型调用优化

### 1. 请求优化

#### Token管理

```java
@Service
public class AiModelService {

    private static final int MAX_TOKENS = 2000;
    private static final int MAX_CONTEXT_LENGTH = 4000;

    public String generateResponse(String prompt, String context) {
        // 截断过长的上下文
        if (context.length() > MAX_CONTEXT_LENGTH) {
            context = context.substring(
                context.length() - MAX_CONTEXT_LENGTH);
        }

        // 控制生成长度
        ChatRequest request = ChatRequest.builder()
            .model("gpt-4")
            .messages(buildMessages(prompt, context))
            .maxTokens(MAX_TOKENS)
            .temperature(0.7)
            .build();

        return chatClient.chat(request).content();
    }
}
```

#### 批量处理

```java
/**
 * 批量代码分析
 */
public List<CodeAnalysisResult> batchAnalyze(List<CodeSnippet> codes) {
    // 批量提交，减少API调用次数
    String batchPrompt = codes.stream()
        .map(code -> String.format(
            "Code %d:\n%s\n---\n",
            code.getId(), code.getContent()))
        .collect(Collectors.joining("\n"));

    String response = aiClient.analyze(batchPrompt);
    return parseMultipleResults(response);
}
```

### 2. 缓存AI响应

```java
@Service
public class ConversationService {

    /**
     * 缓存常见问题回答
     */
    @Cacheable(value = "aiResponses",
               key = "#question",
               unless = "#result == null")
    public String getAiResponse(String question) {
        // 对于常见问题，直接返回缓存结果
        return aiModelService.generateResponse(question);
    }

    /**
     * 使用相似度搜索缓存
     */
    public String getResponseWithSimilarityCache(String question) {
        // 查找相似问题的缓存
        Optional<CachedResponse> similar =
            responseCacheService.findSimilar(question, 0.9);

        if (similar.isPresent()) {
            return similar.get().getResponse();
        }

        // 调用AI生成新回答
        String response = aiModelService.generateResponse(question);
        responseCacheService.save(question, response);

        return response;
    }
}
```

### 3. 异步处理

```java
@Service
public class AsyncAiService {

    @Async("aiExecutor")
    public CompletableFuture<String> generateResponseAsync(String prompt) {
        String response = aiModelService.generateResponse(prompt);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 异步执行器配置
     */
    @Bean(name = "aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-async-");
        executor.initialize();
        return executor;
    }
}
```

### 4. 超时和降级

```java
@Service
public class ResilientAiService {

    @CircuitBreaker(name = "aiService", fallbackMethod = "fallbackResponse")
    @TimeLimiter(name = "aiService")
    public CompletableFuture<String> getAiResponse(String prompt) {
        return CompletableFuture.supplyAsync(() ->
            aiModelService.generateResponse(prompt));
    }

    /**
     * 降级方法
     */
    private CompletableFuture<String> fallbackResponse(
        String prompt, Exception ex) {
        log.warn("AI service failed, returning fallback", ex);
        return CompletableFuture.completedFuture(
            "抱歉，AI服务暂时不可用，请稍后再试。");
    }
}

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      aiService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10

  timelimiter:
    instances:
      aiService:
        timeout-duration: 30s
```

---

## 💻 应用层优化

### 1. 数据库查询优化

#### N+1问题解决

```java
// 不推荐: N+1查询
public List<UserDTO> getUsers() {
    List<User> users = userRepository.findAll();
    return users.stream()
        .map(user -> {
            List<LearningPath> paths =
                pathRepository.findByUserId(user.getId());  // N次查询
            return new UserDTO(user, paths);
        })
        .collect(Collectors.toList());
}

// 推荐: 使用JOIN FETCH
@Query("SELECT u FROM User u LEFT JOIN FETCH u.learningPaths")
List<User> findAllWithPaths();

public List<UserDTO> getUsersOptimized() {
    List<User> users = userRepository.findAllWithPaths();  // 1次查询
    return users.stream()
        .map(user -> new UserDTO(user, user.getLearningPaths()))
        .collect(Collectors.toList());
}
```

#### 批量操作

```java
// 批量插入
public void batchInsert(List<User> users) {
    int batchSize = 50;
    for (int i = 0; i < users.size(); i += batchSize) {
        int end = Math.min(i + batchSize, users.size());
        List<User> batch = users.subList(i, end);
        userRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
    }
}

// 批量更新
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
int batchUpdateStatus(@Param("status") String status, @Param("ids") List<Long> ids);
```

### 2. 异步处理

```java
@Service
public class NotificationService {

    /**
     * 异步发送通知
     */
    @Async
    public void sendNotification(User user, String message) {
        // 耗时操作异步执行
        emailService.sendEmail(user.getEmail(), message);
        smsService.sendSMS(user.getPhone(), message);
    }

    /**
     * 异步任务配置
     */
    @Configuration
    @EnableAsync
    public static class AsyncConfig {

        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(10);
            executor.setMaxPoolSize(20);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("async-");
            executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy());
            executor.initialize();
            return executor;
        }
    }
}
```

### 3. 懒加载优化

```java
@Entity
public class LearningPath {

    @Id
    private Long id;

    private String title;

    // 懒加载关联
    @OneToMany(mappedBy = "learningPath", fetch = FetchType.LAZY)
    private List<LearningModule> modules;

    // DTO投影避免懒加载
    public interface LearningPathSummary {
        Long getId();
        String getTitle();
        Integer getModuleCount();
    }
}

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    // 只查询需要的字段
    @Query("SELECT l.id as id, l.title as title, " +
           "COUNT(m) as moduleCount " +
           "FROM LearningPath l LEFT JOIN l.modules m " +
           "GROUP BY l.id, l.title")
    List<LearningPathSummary> findAllSummaries();
}
```

### 4. 对象池化

```java
/**
 * 使用对象池减少对象创建开销
 */
@Configuration
public class PoolConfig {

    @Bean
    public GenericObjectPool<ExpensiveObject> expensiveObjectPool() {
        GenericObjectPoolConfig<ExpensiveObject> config =
            new GenericObjectPoolConfig<>();
        config.setMaxTotal(50);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        config.setTestOnBorrow(true);

        return new GenericObjectPool<>(
            new ExpensiveObjectFactory(), config);
    }
}
```

---

## 📊 监控和诊断

### 1. 性能指标监控

#### Actuator监控端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    web:
      server:
        request:
          autotime:
            enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.9, 0.95, 0.99
```

#### 关键性能指标

```java
@Component
public class PerformanceMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录AI响应时间
     */
    public void recordAiResponseTime(long milliseconds) {
        Timer.builder("ai.response.time")
            .description("AI model response time")
            .tag("model", "gpt-4")
            .register(meterRegistry)
            .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录缓存命中率
     */
    public void recordCacheHit(String cacheName, boolean hit) {
        Counter.builder("cache." + (hit ? "hits" : "misses"))
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }
}
```

### 2. APM工具集成

#### SkyWalking集成

```yaml
# application.yml
skywalking:
  agent:
    service-name: codenavigator
    collector:
      backend-service: skywalking-oap:11800
```

```bash
# 启动参数
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=codenavigator \
     -Dskywalking.collector.backend_service=skywalking-oap:11800 \
     -jar codenavigator.jar
```

### 3. 日志性能

#### 异步日志

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 异步Appender -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <appender-ref ref="FILE"/>
    </appender>

    <!-- 文件Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/codenavigator/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/codenavigator/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC"/>
    </root>
</configuration>
```

---

## 🧪 性能测试

### 1. JMeter压力测试

#### 测试计划示例

```xml
<!-- test-plan.jmx -->
<ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">10</stringProp>
    <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

#### 测试脚本

```bash
#!/bin/bash
# performance_test.sh

# 启动JMeter测试
jmeter -n -t test-plan.jmx \
    -l results.jtl \
    -e -o report/ \
    -Jthreads=100 \
    -Jrampup=10 \
    -Jduration=300

# 生成报告
jmeter -g results.jtl -o report/
```

### 2. Gatling负载测试

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CodeNavigatorSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("CodeNavigator Load Test")
    .exec(http("Get User")
      .get("/api/users/1"))
    .pause(1)
    .exec(http("Create Conversation")
      .post("/api/conversations")
      .body(StringBody("""{"message": "Hello"}"""))
      .header("Content-Type", "application/json"))

  setUp(
    scn.inject(
      rampUsers(100) during (10 seconds),
      constantUsersPerSec(20) during (5 minutes)
    )
  ).protocols(httpProtocol)
}
```

### 3. 性能基准测试

```bash
#!/bin/bash
# benchmark.sh

echo "=== API Performance Benchmark ==="

# 测试接口响应时间
for i in {1..100}; do
    curl -s -w "%{time_total}\n" -o /dev/null \
        http://localhost:8080/api/users/$i
done | awk '{sum+=$1; n++} END {print "Avg:", sum/n*1000, "ms"}'

# 测试并发性能
ab -n 1000 -c 50 \
   -H "Authorization: Bearer token" \
   http://localhost:8080/api/conversations
```

---

## ⚠️ 常见性能问题

### 1. 内存泄漏

**症状**: 内存持续增长，GC频繁

**排查**:
```bash
# 生成堆转储
jmap -dump:format=b,file=heapdump.hprof <pid>

# 使用MAT分析
# Eclipse Memory Analyzer Tool
```

**常见原因**:
- ThreadLocal未清理
- 静态集合持续增长
- 缓存未设置过期
- 数据库连接未关闭

### 2. 慢查询

**症状**: 数据库查询耗时长

**排查**:
```sql
-- 查看慢查询
SHOW FULL PROCESSLIST;

-- 分析执行计划
EXPLAIN SELECT * FROM users WHERE email = 'test@example.com';
```

**解决方案**:
- 添加索引
- 优化查询语句
- 使用缓存
- 数据库分片

### 3. 线程池耗尽

**症状**: 请求阻塞，响应超时

**排查**:
```bash
# 查看线程状态
jstack <pid> | grep "java.lang.Thread.State"

# 查看线程数
jstack <pid> | grep "^\"" | wc -l
```

**解决方案**:
- 增加线程池大小
- 优化任务执行时间
- 使用异步处理
- 添加请求限流

---

## 📚 参考资源

### 官方文档
- [Spring Boot Performance Tuning](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.performance)
- [MySQL Performance Schema](https://dev.mysql.com/doc/refman/8.0/en/performance-schema.html)
- [Redis Best Practices](https://redis.io/docs/manual/optimization/)

### 工具
- [JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html) - Java性能分析
- [VisualVM](https://visualvm.github.io/) - JVM监控
- [Arthas](https://arthas.aliyun.com/) - Java诊断工具

### 书籍推荐
- 《Java性能优化权威指南》
- 《高性能MySQL》
- 《Redis设计与实现》

---

**最后更新**: 2025-10-16
**维护者**: CodeNavigator Team

如有疑问或建议，请通过GitHub Issues反馈。
