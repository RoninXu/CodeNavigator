package com.codenavigator.web.controller;

import com.codenavigator.ai.service.LearningPathGenerator;
import com.codenavigator.ai.service.ProgressTracker;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.common.enums.UserLevel;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/learning-paths")
@Tag(name = "学习路径管理", description = "学习路径生成、查看、进度跟踪相关接口")
public class LearningPathController {
    
    private final LearningPathGenerator pathGenerator;
    private final ProgressTracker progressTracker;
    
    @Hidden
    @GetMapping
    public String listPaths(Model model) {
        model.addAttribute("pageTitle", "学习路径");
        return "learning-path/list";
    }
    
    @Hidden
    @GetMapping("/{pathId}")
    public String viewPath(
            @Parameter(description = "学习路径唯一标识符", required = true, example = "spring-boot-path")
            @PathVariable String pathId, Model model) {
        log.info("Viewing learning path: {}", pathId);
        
        // 这里应该从数据库获取学习路径
        // LearningPath path = learningPathRepository.findById(pathId);
        
        // 为演示目的，生成一个示例路径
        LearningPath path = pathGenerator.generatePath("Spring", UserLevel.INTERMEDIATE, new HashMap<>());
        
        model.addAttribute("learningPath", path);
        model.addAttribute("pageTitle", path.getTitle());
        
        return "learning-path/detail";
    }
    
    @Operation(summary = "开始学习路径", description = "为用户启动指定的学习路径")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "学习路径启动成功",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"学习路径已开始！\"}"))),
        @ApiResponse(responseCode = "404", description = "学习路径不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{pathId}/start")
    @ResponseBody
    public Map<String, Object> startLearningPath(
            @Parameter(description = "要开始的学习路径ID", required = true, example = "spring-boot-path")
            @PathVariable String pathId) {
        log.info("Starting learning path: {}", pathId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里应该创建用户进度记录
            response.put("success", true);
            response.put("message", "学习路径已开始！");
            
        } catch (Exception e) {
            log.error("Error starting learning path", e);
            response.put("success", false);
            response.put("message", "启动学习路径失败，请稍后再试。");
        }
        
        return response;
    }
    
    @Hidden
    @GetMapping("/generate")
    public String generatePathForm(Model model) {
        model.addAttribute("pageTitle", "生成学习路径");
        return "learning-path/generate";
    }
    
    @Operation(summary = "生成学习路径", description = "根据用户需求生成个性化学习路径")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "学习路径生成成功",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LearningPath.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "生成失败")
    })
    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generatePath(
            @Parameter(description = "生成请求，包含技术栈、用户水平等信息", required = true,
                    schema = @Schema(example = "{\"technology\": \"Spring Boot\", \"level\": \"INTERMEDIATE\", \"goals\": [\"web开发\", \"微服务\"]}"))
            @RequestBody Map<String, Object> request) {
        log.info("Generating learning path with request: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String technology = (String) request.get("technology");
            String levelStr = (String) request.get("level");
            UserLevel userLevel = UserLevel.valueOf(levelStr.toUpperCase());
            
            LearningPath path = pathGenerator.generatePath(technology, userLevel, request);
            
            response.put("success", true);
            response.put("pathId", path.getId());
            response.put("path", path);
            
        } catch (Exception e) {
            log.error("Error generating learning path", e);
            response.put("success", false);
            response.put("message", "生成学习路径失败：" + e.getMessage());
        }
        
        return response;
    }
    
    @Operation(summary = "获取学习进度", description = "获取用户在指定学习路径中的学习进度信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取学习进度",
                content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"summary\": {\"completed\": 3, \"total\": 10}, \"modules\": []}"))),
        @ApiResponse(responseCode = "404", description = "学习路径或用户不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{pathId}/progress")
    @ResponseBody
    public Map<String, Object> getProgress(
            @Parameter(description = "学习路径唯一标识符", required = true, example = "spring-boot-path")
            @PathVariable String pathId,
            @Parameter(description = "用户唯一标识符", example = "user-123")
            @RequestParam(defaultValue = "default-user") String userId) {
        log.info("Getting progress for path: {} and user: {}", pathId, userId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            ProgressTracker.ProgressSummary summary = progressTracker.getProgressSummary(userId, pathId);
            List<ProgressTracker.ModuleProgress> moduleProgress = progressTracker.getModuleProgressList(userId, pathId);
            
            response.put("success", true);
            response.put("summary", summary);
            response.put("modules", moduleProgress);
            
        } catch (Exception e) {
            log.error("Error getting progress", e);
            response.put("success", false);
            response.put("message", "获取进度信息失败");
        }
        
        return response;
    }
}