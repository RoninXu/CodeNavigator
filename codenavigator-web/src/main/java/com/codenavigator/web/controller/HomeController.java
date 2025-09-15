package com.codenavigator.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    
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
    
    @GetMapping("/learning")
    public String learning(Model model) {
        model.addAttribute("title", "学习路径");
        model.addAttribute("breadcrumbs", new String[]{"首页", "学习路径"});
        return "learning/index";
    }
    
    @GetMapping("/conversation")
    public String conversation(Model model, @RequestParam(value = "from", required = false) String from) {
        model.addAttribute("title", "AI对话");
        model.addAttribute("breadcrumbs", new String[]{"首页", "AI对话"});
        
        // 添加来源页面信息，用于更好的用户体验
        if (from != null) {
            model.addAttribute("referrer", from);
        }
        
        return "conversation/chat";
    }
    
    @GetMapping("/code-analysis")
    public String codeAnalysis(Model model) {
        model.addAttribute("title", "代码分析");
        model.addAttribute("breadcrumbs", new String[]{"首页", "代码分析"});
        return "code-analysis/index";
    }
    
    @GetMapping("/progress")
    public String progress(Model model) {
        model.addAttribute("title", "学习进度");
        model.addAttribute("breadcrumbs", new String[]{"首页", "学习进度"});
        return "progress/index";
    }
    
    @GetMapping("/getting-started")
    public String gettingStarted(Model model, RedirectAttributes redirectAttributes) {
        // 引导新用户的快速开始流程
        model.addAttribute("title", "快速开始");
        model.addAttribute("breadcrumbs", new String[]{"首页", "快速开始"});
        model.addAttribute("isGuidedFlow", true);
        return "onboarding/getting-started";
    }
}