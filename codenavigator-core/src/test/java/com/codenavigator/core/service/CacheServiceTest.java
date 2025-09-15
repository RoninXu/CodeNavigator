package com.codenavigator.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void testSetAndGet() {
        String key = "test:key";
        String value = "test value";

        // Test set
        cacheService.set(key, value);
        verify(valueOperations).set(key, value);

        // Test get
        when(valueOperations.get(key)).thenReturn(value);
        String result = cacheService.get(key, String.class);
        assertEquals(value, result);
    }

    @Test
    void testSetWithExpiration() {
        String key = "test:key";
        String value = "test value";
        long timeout = 3600;
        TimeUnit unit = TimeUnit.SECONDS;

        cacheService.set(key, value, timeout, unit);
        verify(valueOperations).set(key, value, timeout, unit);
    }

    @Test
    void testGetWithWrongType() {
        String key = "test:key";
        String value = "test value";

        when(valueOperations.get(key)).thenReturn(value);
        Integer result = cacheService.get(key, Integer.class);
        assertNull(result);
    }

    @Test
    void testGetWithException() {
        String key = "test:key";
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis error"));

        String result = cacheService.get(key, String.class);
        assertNull(result);
    }

    @Test
    void testDelete() {
        String key = "test:key";
        when(redisTemplate.delete(key)).thenReturn(true);

        boolean result = cacheService.delete(key);
        assertTrue(result);
        verify(redisTemplate).delete(key);
    }

    @Test
    void testBatchDelete() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        when(redisTemplate.delete(keys)).thenReturn(3L);

        long result = cacheService.delete(keys);
        assertEquals(3L, result);
        verify(redisTemplate).delete(keys);
    }

    @Test
    void testExists() {
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        boolean result = cacheService.exists(key);
        assertTrue(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    void testExpire() {
        String key = "test:key";
        long timeout = 3600;
        TimeUnit unit = TimeUnit.SECONDS;
        when(redisTemplate.expire(key, timeout, unit)).thenReturn(true);

        boolean result = cacheService.expire(key, timeout, unit);
        assertTrue(result);
        verify(redisTemplate).expire(key, timeout, unit);
    }

    @Test
    void testGetExpire() {
        String key = "test:key";
        when(redisTemplate.getExpire(key)).thenReturn(3600L);

        long result = cacheService.getExpire(key);
        assertEquals(3600L, result);
        verify(redisTemplate).getExpire(key);
    }

    @Test
    void testKeys() {
        String pattern = "test:*";
        Set<String> expectedKeys = Set.of("test:key1", "test:key2");
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        Set<String> result = cacheService.keys(pattern);
        assertEquals(expectedKeys, result);
        verify(redisTemplate).keys(pattern);
    }

    @Test
    void testHashOperations() {
        String key = "test:hash";
        String hashKey = "field1";
        String value = "value1";

        // Test hSet
        cacheService.hSet(key, hashKey, value);
        verify(hashOperations).put(key, hashKey, value);

        // Test hGet
        when(hashOperations.get(key, hashKey)).thenReturn(value);
        Object result = cacheService.hGet(key, hashKey);
        assertEquals(value, result);

        // Test hGetAll
        Map<Object, Object> expectedMap = Map.of(hashKey, value);
        when(hashOperations.entries(key)).thenReturn(expectedMap);
        Map<Object, Object> allResult = cacheService.hGetAll(key);
        assertEquals(expectedMap, allResult);

        // Test hDelete
        when(hashOperations.delete(key, hashKey)).thenReturn(1L);
        long deleteResult = cacheService.hDelete(key, hashKey);
        assertEquals(1L, deleteResult);
    }

    @Test
    void testListOperations() {
        String key = "test:list";
        String value1 = "value1";
        String value2 = "value2";

        // Test lPush
        when(listOperations.leftPushAll(key, value1, value2)).thenReturn(2L);
        long pushResult = cacheService.lPush(key, value1, value2);
        assertEquals(2L, pushResult);

        // Test lRange
        List<Object> expectedList = Arrays.asList(value1, value2);
        when(listOperations.range(key, 0, -1)).thenReturn(expectedList);
        List<Object> rangeResult = cacheService.lRange(key, 0, -1);
        assertEquals(expectedList, rangeResult);
    }

    @Test
    void testSetOperations() {
        String key = "test:set";
        String value1 = "value1";
        String value2 = "value2";

        // Test sAdd
        when(setOperations.add(key, value1, value2)).thenReturn(2L);
        long addResult = cacheService.sAdd(key, value1, value2);
        assertEquals(2L, addResult);

        // Test sMembers
        Set<Object> expectedSet = Set.of(value1, value2);
        when(setOperations.members(key)).thenReturn(expectedSet);
        Set<Object> membersResult = cacheService.sMembers(key);
        assertEquals(expectedSet, membersResult);
    }

    @Test
    void testZSetOperations() {
        String key = "test:zset";
        String value = "value1";
        double score = 1.0;

        // Test zAdd
        when(zSetOperations.add(key, value, score)).thenReturn(true);
        boolean addResult = cacheService.zAdd(key, value, score);
        assertTrue(addResult);

        // Test zRangeByScore
        Set<Object> expectedSet = Set.of(value);
        when(zSetOperations.rangeByScore(key, 0.0, 2.0)).thenReturn(expectedSet);
        Set<Object> rangeResult = cacheService.zRangeByScore(key, 0.0, 2.0);
        assertEquals(expectedSet, rangeResult);
    }

    @Test
    void testErrorHandling() {
        String key = "test:key";
        String value = "test value";

        // Test set with exception
        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(key, value);
        assertDoesNotThrow(() -> cacheService.set(key, value));

        // Test delete with exception
        when(redisTemplate.delete(key)).thenThrow(new RuntimeException("Redis error"));
        boolean deleteResult = cacheService.delete(key);
        assertFalse(deleteResult);

        // Test exists with exception
        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis error"));
        boolean existsResult = cacheService.exists(key);
        assertFalse(existsResult);
    }
}