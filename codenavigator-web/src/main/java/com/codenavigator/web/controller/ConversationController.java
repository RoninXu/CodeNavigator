package com.codenavigator.web.controller;

import com.codenavigator.ai.engine.ConversationEngine;
import com.codenavigator.ai.dto.ConversationRequest;
import com.codenavigator.ai.dto.ConversationResponse;
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
public class ConversationController {
    
    private final ConversationEngine conversationEngine;
    
    @GetMapping
    public String conversationPage(Model model) {
        model.addAttribute("pageTitle", "AI学习助手");
        return "conversation/chat";
    }
    
    @PostMapping("/message")
    @ResponseBody
    public ResponseEntity<ConversationResponse> sendMessage(@RequestBody ConversationRequest request) {
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
    
    @GetMapping("/sessions/{sessionId}")
    @ResponseBody
    public ResponseEntity<Object> getSessionInfo(@PathVariable String sessionId) {
        // 获取会话信息的逻辑
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/end")
    @ResponseBody
    public ResponseEntity<Object> endSession(@PathVariable String sessionId) {
        // 结束会话的逻辑
        log.info("Ending conversation session: {}", sessionId);
        return ResponseEntity.ok().build();
    }
}