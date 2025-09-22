package com.codenavigator.web.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Tag(name = "首页管理", description = "系统首页和导航相关接口")
public class HomeController {
    
    @Hidden
    @GetMapping("/")
    public String home(Model model, @RequestParam(value = "welcome", required = false) String welcome) {
        model.addAttribute("title", "CodeNavigator - 智能对话引导学习框架");
        model.addAttribute("version", "v1.0.0-SNAPSHOT");
        
        // 添加欢迎消息支持
        if ("true".equals(welcome)) {
            model.addAttribute("showWelcome", true);
        }
        
        return "index";
    }
    
    @Hidden
    @GetMapping("/learning")
    public String learning(Model model) {
        model.addAttribute("title", "学习路径");
        model.addAttribute("breadcrumbs", new String[]{"首页", "学习路径"});
        return "learning/index";
    }
    
    
    @Hidden
    @GetMapping("/code-analysis")
    public String codeAnalysis(Model model) {
        model.addAttribute("title", "代码分析");
        model.addAttribute("breadcrumbs", new String[]{"首页", "代码分析"});
        return "code-analysis/index";
    }
    
    @Hidden
    @GetMapping("/progress")
    public String progress(Model model) {
        model.addAttribute("title", "学习进度");
        model.addAttribute("breadcrumbs", new String[]{"首页", "学习进度"});
        return "progress/index";
    }
    
    @Operation(summary = "快速开始页面", description = "引导新用户的快速开始流程页面")
    @ApiResponse(responseCode = "200", description = "成功返回快速开始页面")
    @GetMapping("/getting-started")
    public String gettingStarted(Model model, RedirectAttributes redirectAttributes) {
        // 引导新用户的快速开始流程
        model.addAttribute("title", "快速开始");
        model.addAttribute("breadcrumbs", new String[]{"首页", "快速开始"});
        model.addAttribute("isGuidedFlow", true);
        return "onboarding/getting-started";
    }

    @Operation(summary = "API文档", description = "跳转到Swagger UI API文档页面")
    @ApiResponse(responseCode = "302", description = "重定向到Swagger UI")
    @GetMapping("/api-docs")
    public String apiDocs() {
        return "redirect:/swagger-ui.html";
    }
}