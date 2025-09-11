package com.codenavigator.ai.service;

import com.codenavigator.ai.model.ConversationState;
import com.codenavigator.common.enums.UserLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaturalLanguageProcessor {
    
    // 技术关键词映射
    private static final Map<String, List<String>> TECHNOLOGY_KEYWORDS = Map.of(
        "Spring", Arrays.asList("spring", "springboot", "spring boot", "spring framework", "依赖注入", "ioc", "aop"),
        "Kafka", Arrays.asList("kafka", "消息队列", "mq", "message queue", "stream", "流处理"),
        "Netty", Arrays.asList("netty", "nio", "网络编程", "socket", "tcp", "udp", "异步"),
        "MySQL", Arrays.asList("mysql", "数据库", "sql", "关系数据库", "jdbc"),
        "Redis", Arrays.asList("redis", "缓存", "cache", "nosql", "内存数据库"),
        "Docker", Arrays.asList("docker", "容器", "container", "微服务", "k8s", "kubernetes"),
        "Java", Arrays.asList("java", "jvm", "多线程", "并发", "集合", "泛型")
    );
    
    // 技能水平关键词
    private static final Map<UserLevel, List<String>> LEVEL_KEYWORDS = Map.of(
        UserLevel.BEGINNER, Arrays.asList("初学者", "新手", "刚开始", "零基础", "不会", "不懂", "菜鸟"),
        UserLevel.INTERMEDIATE, Arrays.asList("有点经验", "一般", "了解一些", "会一点", "中等", "有基础"),
        UserLevel.ADVANCED, Arrays.asList("熟练", "经验丰富", "很熟悉", "专家", "精通", "资深")
    );
    
    // 意图关键词
    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
        "set_learning_goal", Arrays.asList("学习", "想学", "掌握", "了解", "深入", "提升"),
        "ask_question", Arrays.asList("什么是", "如何", "怎么", "为什么", "问题"),
        "get_help", Arrays.asList("帮助", "帮忙", "协助", "指导"),
        "show_progress", Arrays.asList("进度", "完成", "学了", "掌握了"),
        "request_review", Arrays.asList("检查", "review", "评估", "反馈")
    );
    
    public String extractIntent(String message, ConversationState state) {
        log.debug("Extracting intent from message: {}", message);
        
        String normalizedMessage = message.toLowerCase();
        
        // 根据会话阶段优先判断意图
        switch (state.getPhase()) {
            case GREETING:
                if (containsAnyKeyword(normalizedMessage, INTENT_KEYWORDS.get("set_learning_goal"))) {
                    return "set_learning_goal";
                }
                break;
            case GOAL_IDENTIFICATION:
                return "set_learning_goal";
            case SKILL_ASSESSMENT:
                return "assess_skill";
            case PATH_PLANNING:
                return "plan_path";
            case TASK_EXECUTION:
                if (containsAnyKeyword(normalizedMessage, INTENT_KEYWORDS.get("ask_question"))) {
                    return "ask_question";
                }
                if (containsAnyKeyword(normalizedMessage, INTENT_KEYWORDS.get("request_review"))) {
                    return "request_review";
                }
                return "task_help";
        }
        
        // 通用意图识别
        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            if (containsAnyKeyword(normalizedMessage, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return "general_question";
    }
    
    public Map<String, Object> extractEntities(String message) {
        log.debug("Extracting entities from message: {}", message);
        
        Map<String, Object> entities = new HashMap<>();
        String normalizedMessage = message.toLowerCase();
        
        // 提取技术实体
        for (Map.Entry<String, List<String>> entry : TECHNOLOGY_KEYWORDS.entrySet()) {
            if (containsAnyKeyword(normalizedMessage, entry.getValue())) {
                entities.put("technology", entry.getKey());
                break;
            }
        }
        
        // 提取技能水平实体
        for (Map.Entry<UserLevel, List<String>> entry : LEVEL_KEYWORDS.entrySet()) {
            if (containsAnyKeyword(normalizedMessage, entry.getValue())) {
                entities.put("skill_level", entry.getKey());
                break;
            }
        }
        
        // 提取时间实体
        String timePattern = "(\\d+)\\s*(天|周|月|小时|分钟)";
        Pattern pattern = Pattern.compile(timePattern);
        var matcher = pattern.matcher(message);
        if (matcher.find()) {
            entities.put("time_duration", matcher.group());
        }
        
        return entities;
    }
    
    public String extractLearningGoal(String message) {
        log.debug("Extracting learning goal from message: {}", message);
        
        String normalizedMessage = message.toLowerCase();
        
        // 查找技术关键词
        for (Map.Entry<String, List<String>> entry : TECHNOLOGY_KEYWORDS.entrySet()) {
            if (containsAnyKeyword(normalizedMessage, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        // 使用简单的规则提取学习目标
        String[] patterns = {
            "学习\\s*(\\w+)",
            "想学\\s*(\\w+)",
            "掌握\\s*(\\w+)",
            "了解\\s*(\\w+)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            var matcher = pattern.matcher(message);
            if (matcher.find()) {
                String goal = matcher.group(1);
                // 验证是否是有效的技术名称
                for (String tech : TECHNOLOGY_KEYWORDS.keySet()) {
                    if (tech.toLowerCase().contains(goal.toLowerCase()) || 
                        goal.toLowerCase().contains(tech.toLowerCase())) {
                        return tech;
                    }
                }
                return goal;
            }
        }
        
        return null;
    }
    
    public UserLevel assessUserLevel(String message) {
        log.debug("Assessing user level from message: {}", message);
        
        String normalizedMessage = message.toLowerCase();
        
        // 直接匹配技能水平关键词
        for (Map.Entry<UserLevel, List<String>> entry : LEVEL_KEYWORDS.entrySet()) {
            if (containsAnyKeyword(normalizedMessage, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        // 基于经验年限判断
        Pattern experiencePattern = Pattern.compile("(\\d+)\\s*年");
        var matcher = experiencePattern.matcher(message);
        if (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            if (years < 2) return UserLevel.BEGINNER;
            if (years < 5) return UserLevel.INTERMEDIATE;
            return UserLevel.ADVANCED;
        }
        
        // 默认返回中等水平
        return UserLevel.INTERMEDIATE;
    }
    
    public double calculateConfidence(String intent, Map<String, Object> entities) {
        double confidence = 0.5; // 基础置信度
        
        // 根据提取的实体数量调整置信度
        confidence += entities.size() * 0.1;
        
        // 根据意图类型调整置信度
        switch (intent) {
            case "set_learning_goal":
                if (entities.containsKey("technology")) {
                    confidence += 0.3;
                }
                break;
            case "assess_skill":
                if (entities.containsKey("skill_level")) {
                    confidence += 0.3;
                }
                break;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    private boolean containsAnyKeyword(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}