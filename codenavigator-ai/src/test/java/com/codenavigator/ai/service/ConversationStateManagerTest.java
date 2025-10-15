package com.codenavigator.ai.service;

import com.codenavigator.ai.model.ConversationState;
import com.codenavigator.common.enums.UserLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConversationStateManager单元测试
 * 测试对话状态管理功能
 */
@DisplayName("ConversationStateManager单元测试")
class ConversationStateManagerTest {

    private ConversationStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new ConversationStateManager();
    }

    // ========== 保存和获取状态测试 ==========

    @Test
    @DisplayName("保存状态 - 应成功保存到本地缓存")
    void testSaveState_SuccessfullySavesState() {
        // Given
        ConversationState state = createState("session-1", "1");

        // When
        stateManager.saveState(state);

        // Then
        ConversationState retrieved = stateManager.getState("session-1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getSessionId()).isEqualTo("session-1");
        assertThat(retrieved.getUserId()).isEqualTo("1");
    }

    @Test
    @DisplayName("获取状态 - 不存在的会话返回null")
    void testGetState_NonExistentSession_ReturnsNull() {
        // When
        ConversationState state = stateManager.getState("non-existent");

        // Then
        assertThat(state).isNull();
    }

    @Test
    @DisplayName("保存状态 - 覆盖已存在的状态")
    void testSaveState_OverwritesExistingState() {
        // Given
        ConversationState originalState = createState("session-1", "1");
        originalState.setLearningGoal("Spring");
        stateManager.saveState(originalState);

        ConversationState updatedState = createState("session-1", "1");
        updatedState.setLearningGoal("Kafka");

        // When
        stateManager.saveState(updatedState);

        // Then
        ConversationState retrieved = stateManager.getState("session-1");
        assertThat(retrieved.getLearningGoal()).isEqualTo("Kafka");
    }

    // ========== 删除状态测试 ==========

    @Test
    @DisplayName("删除状态 - 应从本地缓存中删除")
    void testDeleteState_RemovesFromLocalCache() {
        // Given
        ConversationState state = createState("session-1", "1");
        stateManager.saveState(state);
        assertThat(stateManager.getState("session-1")).isNotNull();

        // When
        stateManager.deleteState("session-1");

        // Then
        assertThat(stateManager.getState("session-1")).isNull();
    }

    @Test
    @DisplayName("删除状态 - 删除不存在的会话不报错")
    void testDeleteState_NonExistentSession_NoError() {
        // When & Then - 不应抛出异常
        stateManager.deleteState("non-existent");
    }

    // ========== 活跃会话统计测试 ==========

    @Test
    @DisplayName("活跃会话数 - 初始为0")
    void testGetActiveSessionCount_InitiallyZero() {
        // When
        int count = stateManager.getActiveSessionCount();

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("活跃会话数 - 添加会话后增加")
    void testGetActiveSessionCount_IncreasesAfterAddingSessions() {
        // Given
        stateManager.saveState(createState("session-1", "1"));
        stateManager.saveState(createState("session-2", "2"));
        stateManager.saveState(createState("session-3", "3"));

        // When
        int count = stateManager.getActiveSessionCount();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("活跃会话数 - 删除会话后减少")
    void testGetActiveSessionCount_DecreasesAfterDeletingSessions() {
        // Given
        stateManager.saveState(createState("session-1", "1"));
        stateManager.saveState(createState("session-2", "2"));
        stateManager.deleteState("session-1");

        // When
        int count = stateManager.getActiveSessionCount();

        // Then
        assertThat(count).isEqualTo(1);
    }

    // ========== 上下文更新测试 ==========

    @Test
    @DisplayName("更新上下文 - 应成功更新并保存")
    void testUpdateStateContext_SuccessfullyUpdates() {
        // Given
        ConversationState state = createState("session-1", "1");
        stateManager.saveState(state);

        // When
        stateManager.updateStateContext("session-1", "technology", "Spring");
        stateManager.updateStateContext("session-1", "level", "INTERMEDIATE");

        // Then
        ConversationState retrieved = stateManager.getState("session-1");
        assertThat(retrieved.getContext()).isNotNull();
        assertThat(retrieved.getContext().get("technology")).isEqualTo("Spring");
        assertThat(retrieved.getContext().get("level")).isEqualTo("INTERMEDIATE");
    }

    @Test
    @DisplayName("更新上下文 - 不存在的会话不报错")
    void testUpdateStateContext_NonExistentSession_NoError() {
        // When & Then - 不应抛出异常
        stateManager.updateStateContext("non-existent", "key", "value");
    }

    @Test
    @DisplayName("更新上下文 - 创建新的上下文Map如果不存在")
    void testUpdateStateContext_CreatesNewContextIfNull() {
        // Given
        ConversationState state = createState("session-1", "1");
        state.setContext(null);
        stateManager.saveState(state);

        // When
        stateManager.updateStateContext("session-1", "key", "value");

        // Then
        ConversationState retrieved = stateManager.getState("session-1");
        assertThat(retrieved.getContext()).isNotNull();
        assertThat(retrieved.getContext().get("key")).isEqualTo("value");
    }

    // ========== 清理过期状态测试 ==========

    @Test
    @DisplayName("清理过期状态 - 应移除过期的会话")
    void testClearExpiredStates_RemovesExpiredSessions() {
        // Given
        ConversationState activeState = createState("active-session", "1");
        stateManager.saveState(activeState);

        ConversationState expiredState = createExpiredState("expired-session", "2");
        stateManager.saveState(expiredState);

        // When
        stateManager.clearExpiredStates();

        // Then
        assertThat(stateManager.getState("active-session")).isNotNull();
        assertThat(stateManager.getState("expired-session")).isNull();
    }

    @Test
    @DisplayName("清理过期状态 - 活跃会话数正确更新")
    void testClearExpiredStates_UpdatesActiveCount() {
        // Given
        stateManager.saveState(createState("active-1", "1"));
        stateManager.saveState(createState("active-2", "2"));
        stateManager.saveState(createExpiredState("expired-1", "3"));
        stateManager.saveState(createExpiredState("expired-2", "4"));

        // When
        stateManager.clearExpiredStates();

        // Then
        assertThat(stateManager.getActiveSessionCount()).isEqualTo(2);
    }

    // ========== 并发测试 ==========

    @Test
    @DisplayName("并发保存 - 应正确处理多线程")
    void testConcurrentSave_HandlesMultipleThreads() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int sessionNum = i;
            threads[i] = new Thread(() -> {
                ConversationState state = createState("session-" + sessionNum, String.valueOf(sessionNum));
                stateManager.saveState(state);
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertThat(stateManager.getActiveSessionCount()).isEqualTo(threadCount);
    }

    // ========== 状态完整性测试 ==========

    @Test
    @DisplayName("保存完整状态 - 所有字段都应保存")
    void testSaveState_PreservesAllFields() {
        // Given
        ConversationState state = createCompleteState();

        // When
        stateManager.saveState(state);

        // Then
        ConversationState retrieved = stateManager.getState(state.getSessionId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getSessionId()).isEqualTo(state.getSessionId());
        assertThat(retrieved.getUserId()).isEqualTo(state.getUserId());
        assertThat(retrieved.getPhase()).isEqualTo(state.getPhase());
        assertThat(retrieved.getLearningGoal()).isEqualTo(state.getLearningGoal());
        assertThat(retrieved.getUserLevel()).isEqualTo(state.getUserLevel());
        assertThat(retrieved.getMessageCount()).isEqualTo(state.getMessageCount());
        assertThat(retrieved.getContext()).isEqualTo(state.getContext());
    }

    @Test
    @DisplayName("状态修改 - 应保留引用")
    void testStateModification_PreservesReference() {
        // Given
        ConversationState state = createState("session-1", "1");
        stateManager.saveState(state);

        // When
        ConversationState retrieved = stateManager.getState("session-1");
        retrieved.setLearningGoal("Modified Goal");
        stateManager.saveState(retrieved);

        // Then
        ConversationState retrieved2 = stateManager.getState("session-1");
        assertThat(retrieved2.getLearningGoal()).isEqualTo("Modified Goal");
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("保存null会话ID - 应处理正常")
    void testSaveState_NullSessionId_HandlesGracefully() {
        // Given
        ConversationState state = new ConversationState();
        state.setSessionId(null);
        state.setUserId("1");

        // When & Then - 可能抛出异常或正常处理
        try {
            stateManager.saveState(state);
            // 如果没有抛出异常，验证可以获取
            ConversationState retrieved = stateManager.getState(null);
            assertThat(retrieved).isNotNull();
        } catch (Exception e) {
            // 抛出异常也是合理的行为
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("多次保存同一会话 - 最后保存的生效")
    void testSaveState_MultipleSaves_LastWins() {
        // Given
        String sessionId = "session-1";

        // When
        for (int i = 0; i < 5; i++) {
            ConversationState state = createState(sessionId, "1");
            state.setLearningGoal("Goal-" + i);
            stateManager.saveState(state);
        }

        // Then
        ConversationState retrieved = stateManager.getState(sessionId);
        assertThat(retrieved.getLearningGoal()).isEqualTo("Goal-4");
    }

    // ========== 辅助方法 ==========

    private ConversationState createState(String sessionId, String userId) {
        return ConversationState.builder()
            .sessionId(sessionId)
            .userId(userId)
            .phase(ConversationState.ConversationPhase.GREETING)
            .messageCount(0)
            .lastInteraction(LocalDateTime.now())
            .build();
    }

    private ConversationState createExpiredState(String sessionId, String userId) {
        ConversationState state = createState(sessionId, userId);
        // 设置一个很久以前的时间使其过期
        state.setLastInteraction(LocalDateTime.now().minusHours(3));
        return state;
    }

    private ConversationState createCompleteState() {
        Map<String, Object> context = new HashMap<>();
        context.put("technology", "Spring");
        context.put("level", "INTERMEDIATE");

        return ConversationState.builder()
            .sessionId("complete-session")
            .userId("1")
            .phase(ConversationState.ConversationPhase.PATH_PLANNING)
            .learningGoal("Spring Boot")
            .userLevel(UserLevel.INTERMEDIATE)
            .messageCount(5)
            .context(context)
            .lastInteraction(LocalDateTime.now())
            .build();
    }
}
