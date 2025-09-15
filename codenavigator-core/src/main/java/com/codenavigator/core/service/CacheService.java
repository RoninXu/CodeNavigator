package com.codenavigator.core.service;

import com.codenavigator.core.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cache set: key={}", key);
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Cache set with expiration: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Failed to set cache with expiration: key={}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isAssignableFrom(value.getClass())) {
                log.debug("Cache hit: key={}", key);
                return (T) value;
            }
            log.debug("Cache miss: key={}", key);
            return null;
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存（泛型方法）
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit: key={}", key);
            } else {
                log.debug("Cache miss: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Cache deleted: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to delete cache: key={}", key, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     */
    public long delete(Collection<String> keys) {
        try {
            Long result = redisTemplate.delete(keys);
            log.debug("Cache batch deleted: keys={}, count={}", keys.size(), result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Failed to batch delete cache: keys={}", keys, e);
            return 0;
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check cache existence: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            log.debug("Cache expiration set: key={}, timeout={} {}", key, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to set cache expiration: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存剩余过期时间
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("Failed to get cache expiration: key={}", key, e);
            return -1;
        }
    }

    /**
     * 模糊查询缓存键
     */
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            log.debug("Cache keys found: pattern={}, count={}", pattern, keys != null ? keys.size() : 0);
            return keys;
        } catch (Exception e) {
            log.error("Failed to get cache keys: pattern={}", pattern, e);
            return Set.of();
        }
    }

    /**
     * 设置Hash缓存
     */
    public void hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            log.debug("Hash cache set: key={}, hashKey={}", key, hashKey);
        } catch (Exception e) {
            log.error("Failed to set hash cache: key={}, hashKey={}", key, hashKey, e);
        }
    }

    /**
     * 获取Hash缓存
     */
    public Object hGet(String key, String hashKey) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hashKey);
            if (value != null) {
                log.debug("Hash cache hit: key={}, hashKey={}", key, hashKey);
            } else {
                log.debug("Hash cache miss: key={}, hashKey={}", key, hashKey);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to get hash cache: key={}, hashKey={}", key, hashKey, e);
            return null;
        }
    }

    /**
     * 获取所有Hash缓存
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
            log.debug("Hash cache get all: key={}, size={}", key, map.size());
            return map;
        } catch (Exception e) {
            log.error("Failed to get all hash cache: key={}", key, e);
            return Map.of();
        }
    }

    /**
     * 删除Hash缓存
     */
    public long hDelete(String key, Object... hashKeys) {
        try {
            Long result = redisTemplate.opsForHash().delete(key, hashKeys);
            log.debug("Hash cache deleted: key={}, hashKeys={}, count={}", key, hashKeys, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Failed to delete hash cache: key={}, hashKeys={}", key, hashKeys, e);
            return 0;
        }
    }

    /**
     * 设置List缓存
     */
    public long lPush(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForList().leftPushAll(key, values);
            log.debug("List cache pushed: key={}, count={}", key, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Failed to push list cache: key={}", key, e);
            return 0;
        }
    }

    /**
     * 获取List缓存
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, start, end);
            log.debug("List cache range: key={}, start={}, end={}, size={}", 
                     key, start, end, list != null ? list.size() : 0);
            return list;
        } catch (Exception e) {
            log.error("Failed to get list cache range: key={}", key, e);
            return List.of();
        }
    }

    /**
     * 设置Set缓存
     */
    public long sAdd(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(key, values);
            log.debug("Set cache added: key={}, count={}", key, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Failed to add set cache: key={}", key, e);
            return 0;
        }
    }

    /**
     * 获取Set缓存
     */
    public Set<Object> sMembers(String key) {
        try {
            Set<Object> set = redisTemplate.opsForSet().members(key);
            log.debug("Set cache members: key={}, size={}", key, set != null ? set.size() : 0);
            return set;
        } catch (Exception e) {
            log.error("Failed to get set cache members: key={}", key, e);
            return Set.of();
        }
    }

    /**
     * 设置ZSet缓存
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            log.debug("ZSet cache added: key={}, value={}, score={}", key, value, score);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to add zset cache: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取ZSet缓存（按分数范围）
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            Set<Object> set = redisTemplate.opsForZSet().rangeByScore(key, min, max);
            log.debug("ZSet cache range by score: key={}, min={}, max={}, size={}", 
                     key, min, max, set != null ? set.size() : 0);
            return set;
        } catch (Exception e) {
            log.error("Failed to get zset cache range by score: key={}", key, e);
            return Set.of();
        }
    }

    /**
     * 清除所有缓存
     */
    public void flushAll() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.warn("All cache flushed");
        } catch (Exception e) {
            log.error("Failed to flush all cache", e);
        }
    }

    /**
     * 获取缓存信息
     */
    public Map<String, Object> getInfo() {
        try {
            // 这里可以实现获取Redis信息的逻辑
            // 例如内存使用情况、连接数等
            log.debug("Getting cache info");
            return Map.of("status", "ok");
        } catch (Exception e) {
            log.error("Failed to get cache info", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}