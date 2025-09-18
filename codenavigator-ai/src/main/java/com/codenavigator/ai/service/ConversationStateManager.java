package com.codenavigator.ai.service;

import com.codenavigator.ai.model.ConversationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConversationStateManager {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 作为Redis的备选方案，本地缓存
    private final ConcurrentHashMap<String, ConversationState> localCache = new ConcurrentHashMap<>();
    
    private static final String STATE_KEY_PREFIX = "conversation:state:";
    private static final Duration STATE_EXPIRATION = Duration.ofHours(2);
    
    public ConversationState getState(String sessionId) {
        log.debug("Getting conversation state for session: {}", sessionId);
        
        try {
            // 首先尝试从Redis获取（如果可用）
            ConversationState state = null;
            if (redisTemplate != null) {
                String key = STATE_KEY_PREFIX + sessionId;
                state = (ConversationState) redisTemplate.opsForValue().get(key);
            }
            
            if (state != null) {
                log.debug("Found state in Redis for session: {}", sessionId);
                return state;
            }
            
            // 如果Redis中没有，从本地缓存获取
            state = localCache.get(sessionId);
            if (state != null && !state.isExpired()) {
                log.debug("Found state in local cache for session: {}", sessionId);
                return state;
            }
            
            // 清理过期的本地缓存
            if (state != null && state.isExpired()) {
                localCache.remove(sessionId);
            }
            
            log.debug("No valid state found for session: {}", sessionId);
            return null;
            
        } catch (Exception e) {
            log.warn("Error getting state from Redis, using local cache for session: {}", sessionId, e);
            ConversationState state = localCache.get(sessionId);
            return (state != null && !state.isExpired()) ? state : null;
        }
    }
    
    public void saveState(ConversationState state) {
        log.debug("Saving conversation state for session: {}", state.getSessionId());

        try {
            // 临时禁用Redis，直接使用本地缓存
            log.debug("Using local cache only for session: {}", state.getSessionId());

            // 保存到本地缓存
            localCache.put(state.getSessionId(), state);

        } catch (Exception e) {
            log.warn("Error saving state to local cache for session: {}",
                     state.getSessionId(), e);
            throw new RuntimeException("Failed to save conversation state", e);
        }
    }
    
    public void deleteState(String sessionId) {
        log.debug("Deleting conversation state for session: {}", sessionId);
        
        try {
            // 从Redis删除（如果可用）
            if (redisTemplate != null) {
                String key = STATE_KEY_PREFIX + sessionId;
                redisTemplate.delete(key);
                log.debug("State deleted from Redis for session: {}", sessionId);
            }

        } catch (Exception e) {
            log.warn("Error deleting state from Redis for session: {}", sessionId, e);
        }
        
        // 从本地缓存删除
        localCache.remove(sessionId);
    }
    
    public void clearExpiredStates() {
        log.debug("Clearing expired conversation states");
        
        // 清理本地缓存中的过期状态
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        log.debug("Expired states cleared from local cache");
    }
    
    public int getActiveSessionCount() {
        // 统计活跃会话数量
        return (int) localCache.values().stream()
            .filter(state -> !state.isExpired())
            .count();
    }
    
    public void updateStateContext(String sessionId, String key, Object value) {
        ConversationState state = getState(sessionId);
        if (state != null) {
            if (state.getContext() == null) {
                state.setContext(new java.util.HashMap<>());
            }
            state.getContext().put(key, value);
            saveState(state);
        }
    }
}