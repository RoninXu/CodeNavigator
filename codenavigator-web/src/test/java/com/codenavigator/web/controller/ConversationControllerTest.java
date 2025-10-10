package com.codenavigator.web.controller;

import com.codenavigator.ai.dto.ConversationRequest;
import com.codenavigator.ai.dto.ConversationResponse;
import com.codenavigator.ai.engine.ConversationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ConversationController单元测试
 * 使用MockMvc进行Controller层测试
 */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationEngine conversationEngine;

    private ConversationRequest validRequest;
    private ConversationResponse successResponse;

    @BeforeEach
    void setUp() {
        // 准备测试请求
        validRequest = new ConversationRequest();
        validRequest.setUserId("test-user-123");
        validRequest.setMessage("What is Spring Boot?");
        validRequest.setType(ConversationRequest.ConversationType.GENERAL_QUESTION);

        // 准备测试响应
        successResponse = ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.ANSWER)
                .message("Spring Boot is a framework for building Java applications.")
                .confidence(0.95)
                .build();
    }

    @Test
    void testConversationPage_ReturnsCorrectView() throws Exception {
        // When & Then
        mockMvc.perform(get("/conversation"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation/chat"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attribute("pageTitle", "AI学习助手"));
    }

    @Test
    void testSendMessage_Success() throws Exception {
        // Given
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ANSWER"))
                .andExpect(jsonPath("$.message").value(containsString("Spring Boot")))
                .andExpect(jsonPath("$.confidence").value(0.95));

        // Verify
        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_WithDefaultUserId() throws Exception {
        // Given
        validRequest.setUserId(null); // 不设置userId
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ANSWER"));

        // Verify that processMessage was called with default userId
        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_WithDefaultType() throws Exception {
        // Given
        validRequest.setType(null); // 不设置type
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ANSWER"));

        // Verify
        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_EngineThrowsException_ReturnsErrorResponse() throws Exception {
        // Given
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ERROR_MESSAGE"))
                .andExpect(jsonPath("$.message").value(containsString("遇到了一些问题")))
                .andExpect(jsonPath("$.confidence").value(0.0));

        // Verify
        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_CodeReviewRequest() throws Exception {
        // Given
        validRequest.setType(ConversationRequest.ConversationType.CODE_REVIEW);
        validRequest.setMessage("public class Test { }");

        ConversationResponse codeReviewResponse = ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.CODE_REVIEW)
                .message("Your code looks good!")
                .confidence(0.90)
                .build();

        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(codeReviewResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CODE_REVIEW"))
                .andExpect(jsonPath("$.confidence").value(0.90));

        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_ConceptExplanation() throws Exception {
        // Given
        validRequest.setType(ConversationRequest.ConversationType.CONCEPT_EXPLANATION);
        validRequest.setMessage("Explain dependency injection");

        ConversationResponse explanationResponse = ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.EXPLANATION)
                .message("Dependency Injection is a design pattern...")
                .confidence(0.92)
                .build();

        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(explanationResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXPLANATION"))
                .andExpect(jsonPath("$.message").value(containsString("Dependency Injection")))
                .andExpect(jsonPath("$.confidence").value(0.92));

        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testGetSessionInfo_Success() throws Exception {
        // Given
        String sessionId = "session-123";

        // When & Then
        mockMvc.perform(get("/conversation/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk());
    }

    @Test
    void testEndSession_Success() throws Exception {
        // Given
        String sessionId = "session-123";

        // When & Then
        mockMvc.perform(post("/conversation/sessions/{sessionId}/end", sessionId))
                .andExpect(status().isOk());
    }

    @Test
    void testSendMessage_InvalidJson_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = "{invalid json}";

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Verify engine was never called
        verify(conversationEngine, never()).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_EmptyMessage() throws Exception {
        // Given
        validRequest.setMessage("");
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_LongMessage() throws Exception {
        // Given
        String longMessage = "a".repeat(5000); // 5000字符的长消息
        validRequest.setMessage(longMessage);

        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ANSWER"));

        verify(conversationEngine, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void testSendMessage_MultipleRequestsInSequence() throws Exception {
        // Given
        when(conversationEngine.processMessage(any(ConversationRequest.class)))
                .thenReturn(successResponse);

        // When & Then - 发送多个请求
        for (int i = 0; i < 3; i++) {
            validRequest.setMessage("Question " + (i + 1));

            mockMvc.perform(post("/conversation/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("ANSWER"));
        }

        // Verify engine was called 3 times
        verify(conversationEngine, times(3)).processMessage(any(ConversationRequest.class));
    }
}
