package com.codenavigator.web.controller;

import com.codenavigator.ai.engine.ConversationEngine;
import com.codenavigator.ai.dto.ConversationRequest;
import com.codenavigator.ai.dto.ConversationResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/conversation")
@Tag(name = "对话管理", description = "AI学习助手对话相关接口")
public class ConversationController {
    
    private final ConversationEngine conversationEngine;
    
    @Hidden
    @GetMapping
    public String conversationPage(Model model) {
        model.addAttribute("pageTitle", "AI学习助手");
        return "conversation/chat";
    }
    
    @Operation(summary = "发送对话消息", description = "向AI学习助手发送消息并获取回复")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功处理消息",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ConversationResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/message")
    @ResponseBody
    public ResponseEntity<ConversationResponse> sendMessage(
            @Parameter(description = "对话请求内容，包含用户消息、用户ID等信息", required = true)
            @RequestBody ConversationRequest request) {
        log.info("Received conversation message from user: {}", request.getUserId());
        
        try {
            // 设置默认用户ID和会话类型（实际应用中应该从session获取）
            if (request.getUserId() == null) {
                request.setUserId("default-user");
            }
            if (request.getType() == null) {
                request.setType(ConversationRequest.ConversationType.GENERAL_QUESTION);
            }
            
            ConversationResponse response = conversationEngine.processMessage(request);
            
            log.info("Conversation response generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing conversation message", e);
            
            ConversationResponse errorResponse = ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.ERROR_MESSAGE)
                .message("抱歉，我遇到了一些问题，请稍后再试。")
                .confidence(0.0)
                .build();
                
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    @Operation(summary = "获取会话信息", description = "根据会话ID获取对话会话的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取会话信息"),
        @ApiResponse(responseCode = "404", description = "会话不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/sessions/{sessionId}")
    @ResponseBody
    public ResponseEntity<Object> getSessionInfo(
            @Parameter(description = "会话唯一标识符", required = true, example = "session-123")
            @PathVariable String sessionId) {
        // 获取会话信息的逻辑
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "结束会话", description = "结束指定的对话会话")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "会话已成功结束"),
        @ApiResponse(responseCode = "404", description = "会话不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/sessions/{sessionId}/end")
    @ResponseBody
    public ResponseEntity<Object> endSession(
            @Parameter(description = "要结束的会话唯一标识符", required = true, example = "session-123")
            @PathVariable String sessionId) {
        // 结束会话的逻辑
        log.info("Ending conversation session: {}", sessionId);
        return ResponseEntity.ok().build();
    }
}