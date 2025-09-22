package com.codenavigator.web.controller;

import com.codenavigator.ai.enums.AiProvider;
import com.codenavigator.ai.service.AiModelService;
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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-model")
@Tag(name = "AI模型管理", description = "AI模型切换、状态查询和测试相关接口")
public class AiModelController {

    private final AiModelService aiModelService;

    @Operation(summary = "获取当前AI提供商", description = "获取当前正在使用的AI模型提供商信息")
    @ApiResponse(responseCode = "200", description = "成功获取当前提供商信息",
            content = @Content(mediaType = "application/json",
            schema = @Schema(example = "{\"provider\": \"openai\", \"displayName\": \"OpenAI\", \"description\": \"GPT系列模型\"}")))
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentProvider() {
        try {
            AiProvider currentProvider = aiModelService.getCurrentProvider();
            Map<String, Object> response = new HashMap<>();
            response.put("provider", currentProvider.getCode());
            response.put("displayName", currentProvider.getDisplayName());
            response.put("description", currentProvider.getDescription());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current provider", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "切换AI提供商", description = "切换到指定的AI模型提供商")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功切换AI提供商",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"已切换到DeepSeek\", \"provider\": \"deepseek\"}"))),
        @ApiResponse(responseCode = "400", description = "提供商不可用或参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/switch/{provider}")
    public ResponseEntity<Map<String, Object>> switchProvider(
            @Parameter(description = "AI提供商代码", required = true,
                    schema = @Schema(allowableValues = {"openai", "deepseek", "claude", "gemini"}))
            @PathVariable String provider) {

        Map<String, Object> response = new HashMap<>();

        try {
            AiProvider aiProvider = AiProvider.fromCode(provider);
            aiModelService.switchProvider(aiProvider);

            response.put("success", true);
            response.put("message", "已切换到" + aiProvider.getDisplayName());
            response.put("provider", aiProvider.getCode());

            log.info("Successfully switched to provider: {}", aiProvider.getDisplayName());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "不支持的AI提供商: " + provider);
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error switching provider", e);
            response.put("success", false);
            response.put("message", "切换失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "获取所有可用提供商", description = "获取所有可用的AI模型提供商列表")
    @ApiResponse(responseCode = "200", description = "成功获取提供商列表",
            content = @Content(mediaType = "application/json",
            schema = @Schema(example = "[{\"provider\": \"openai\", \"displayName\": \"OpenAI\", \"available\": true}]")))
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getAvailableProviders() {
        try {
            List<AiProvider> providers = aiModelService.getAvailableProviders();
            List<Map<String, Object>> response = providers.stream()
                    .map(provider -> {
                        Map<String, Object> providerInfo = new HashMap<>();
                        providerInfo.put("provider", provider.getCode());
                        providerInfo.put("displayName", provider.getDisplayName());
                        providerInfo.put("description", provider.getDescription());
                        providerInfo.put("available", true);
                        providerInfo.put("current", provider.equals(aiModelService.getCurrentProvider()));
                        return providerInfo;
                    })
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting available providers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取提供商状态", description = "获取指定AI提供商的详细状态信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取提供商状态",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"provider\": \"openai\", \"available\": true, \"modelName\": \"gpt-4\", \"hasApiKey\": true}"))),
        @ApiResponse(responseCode = "400", description = "不支持的提供商"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/providers/{provider}/status")
    public ResponseEntity<Map<String, Object>> getProviderStatus(
            @Parameter(description = "AI提供商代码", required = true)
            @PathVariable String provider) {

        try {
            AiProvider aiProvider = AiProvider.fromCode(provider);
            Map<String, Object> status = aiModelService.getProviderStatus(aiProvider);
            return ResponseEntity.ok(status);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "不支持的AI提供商: " + provider);
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("Error getting provider status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取所有提供商状态", description = "获取所有AI提供商的状态信息")
    @ApiResponse(responseCode = "200", description = "成功获取所有提供商状态")
    @GetMapping("/providers/status")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllProviderStatus() {
        try {
            Map<AiProvider, Map<String, Object>> allStatus = aiModelService.getAllProviderStatus();

            // 转换为更友好的格式
            Map<String, Map<String, Object>> response = new HashMap<>();
            allStatus.forEach((provider, status) ->
                response.put(provider.getCode(), status));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting all provider status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "测试提供商连接", description = "测试指定AI提供商的连接状态")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "测试完成",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"provider\": \"openai\", \"connected\": true, \"responseTime\": 1200}"))),
        @ApiResponse(responseCode = "400", description = "不支持的提供商"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/providers/{provider}/test")
    public ResponseEntity<Map<String, Object>> testProvider(
            @Parameter(description = "AI提供商代码", required = true)
            @PathVariable String provider) {

        Map<String, Object> response = new HashMap<>();

        try {
            AiProvider aiProvider = AiProvider.fromCode(provider);
            long startTime = System.currentTimeMillis();

            boolean connected = aiModelService.testProvider(aiProvider);
            long responseTime = System.currentTimeMillis() - startTime;

            response.put("provider", aiProvider.getCode());
            response.put("displayName", aiProvider.getDisplayName());
            response.put("connected", connected);
            response.put("responseTime", responseTime);
            response.put("message", connected ? "连接成功" : "连接失败");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", "不支持的AI提供商: " + provider);
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error testing provider", e);
            response.put("error", "测试失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "发送测试消息", description = "向指定AI提供商发送测试消息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功发送测试消息",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"provider\": \"openai\", \"message\": \"Hello!\", \"response\": \"Hi there!\"}"))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "发送失败")
    })
    @PostMapping("/test-message")
    public ResponseEntity<Map<String, Object>> sendTestMessage(
            @Parameter(description = "测试请求", required = true)
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String message = request.get("message");
            String providerCode = request.get("provider");

            if (message == null || message.trim().isEmpty()) {
                response.put("error", "消息内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            String aiResponse;
            if (providerCode != null && !providerCode.trim().isEmpty()) {
                AiProvider provider = AiProvider.fromCode(providerCode);
                aiResponse = aiModelService.sendMessage(message, provider);
                response.put("provider", provider.getCode());
            } else {
                aiResponse = aiModelService.sendMessage(message);
                response.put("provider", aiModelService.getCurrentProvider().getCode());
            }

            response.put("message", message);
            response.put("response", aiResponse);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending test message", e);
            response.put("error", "发送失败: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}