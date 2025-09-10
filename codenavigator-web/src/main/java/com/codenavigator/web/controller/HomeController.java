package com.codenavigator.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "CodeNavigator - 智能对话引导学习框架");
        model.addAttribute("version", "v1.0.0-SNAPSHOT");
        return "index";
    }
    
    @GetMapping("/learning")
    public String learning(Model model) {
        model.addAttribute("title", "学习路径");
        return "learning/index";
    }
    
    @GetMapping("/conversation")
    public String conversation(Model model) {
        model.addAttribute("title", "AI对话");
        return "conversation/chat";
    }
}