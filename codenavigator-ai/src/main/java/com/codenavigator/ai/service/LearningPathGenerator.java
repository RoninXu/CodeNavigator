package com.codenavigator.ai.service;

import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.core.entity.LearningModule;
import com.codenavigator.common.enums.UserLevel;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.ModuleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathGenerator {
    
    // 预设的学习路径模板
    private static final Map<String, PathTemplate> PATH_TEMPLATES = new HashMap<>();
    
    static {
        initializePathTemplates();
    }
    
    public LearningPath generatePath(String technology, UserLevel userLevel, Map<String, Object> context) {
        log.info("Generating learning path for technology: {}, user level: {}", technology, userLevel);
        
        PathTemplate template = PATH_TEMPLATES.get(technology.toLowerCase());
        if (template == null) {
            template = createGenericTemplate(technology);
        }
        
        LearningPath path = LearningPath.builder()
            .id(UUID.randomUUID().toString())
            .title(String.format("%s 学习路径 - %s级别", technology, getUserLevelName(userLevel)))
            .description(template.description)
            .targetLevel(userLevel)
            .estimatedDuration(calculateDuration(template.modules, userLevel))
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build();
        
        // 根据用户水平筛选和调整模块
        List<LearningModule> modules = adaptModulesForUserLevel(template.modules, userLevel);
        path.setModules(modules);
        
        // 设置模块的顺序和依赖关系
        setModuleOrder(modules);
        
        log.info("Generated learning path with {} modules", modules.size());
        return path;
    }
    
    private List<LearningModule> adaptModulesForUserLevel(List<ModuleTemplate> templates, UserLevel userLevel) {
        List<LearningModule> modules = new ArrayList<>();
        
        for (ModuleTemplate template : templates) {
            // 根据用户水平决定是否包含此模块
            if (shouldIncludeModule(template, userLevel)) {
                LearningModule module = LearningModule.builder()
                    .id(UUID.randomUUID().toString())
                    .title(template.title)
                    .description(template.description)
                    .moduleType(template.type)
                    .difficulty(adjustDifficultyForUserLevel(template.difficulty, userLevel))
                    .estimatedHours(template.estimatedHours)
                    .orderIndex(template.orderIndex)
                    .isRequired(template.isRequired)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
                modules.add(module);
            }
        }
        
        return modules;
    }
    
    private boolean shouldIncludeModule(ModuleTemplate template, UserLevel userLevel) {
        switch (userLevel) {
            case BEGINNER:
                return template.difficulty != DifficultyLevel.EXPERT;
            case INTERMEDIATE:
                return template.difficulty != DifficultyLevel.BEGINNER || template.isRequired;
            case ADVANCED:
                return template.difficulty != DifficultyLevel.BEGINNER;
            default:
                return true;
        }
    }
    
    private DifficultyLevel adjustDifficultyForUserLevel(DifficultyLevel originalDifficulty, UserLevel userLevel) {
        // 根据用户水平调整难度
        switch (userLevel) {
            case BEGINNER:
                return originalDifficulty == DifficultyLevel.EXPERT ? DifficultyLevel.ADVANCED : originalDifficulty;
            case ADVANCED:
                return originalDifficulty == DifficultyLevel.BEGINNER ? DifficultyLevel.INTERMEDIATE : originalDifficulty;
            default:
                return originalDifficulty;
        }
    }
    
    private void setModuleOrder(List<LearningModule> modules) {
        // 按照orderIndex排序
        modules.sort(Comparator.comparingInt(LearningModule::getOrderIndex));
        
        // 设置前置模块依赖
        for (int i = 1; i < modules.size(); i++) {
            LearningModule currentModule = modules.get(i);
            LearningModule previousModule = modules.get(i - 1);
            
            if (currentModule.getPrerequisites() == null) {
                currentModule.setPrerequisites(new ArrayList<>());
            }
            currentModule.getPrerequisites().add(previousModule.getId());
        }
    }
    
    private Integer calculateDuration(List<ModuleTemplate> modules, UserLevel userLevel) {
        int totalHours = modules.stream()
            .filter(template -> shouldIncludeModule(template, userLevel))
            .mapToInt(template -> template.estimatedHours)
            .sum();
            
        // 根据用户水平调整总时长
        switch (userLevel) {
            case BEGINNER:
                totalHours = (int) (totalHours * 1.5); // 初学者需要更多时间
                break;
            case ADVANCED:
                totalHours = (int) (totalHours * 0.7); // 高级用户可以更快完成
                break;
        }
        
        return (totalHours + 7) / 8; // 转换为天数（按每天8小时计算）
    }
    
    private String getUserLevelName(UserLevel level) {
        switch (level) {
            case BEGINNER: return "初级";
            case INTERMEDIATE: return "中级";
            case ADVANCED: return "高级";
            default: return "中级";
        }
    }
    
    private PathTemplate createGenericTemplate(String technology) {
        log.info("Creating generic template for technology: {}", technology);
        
        List<ModuleTemplate> modules = Arrays.asList(
            new ModuleTemplate("基础概念", String.format("了解%s的基本概念和原理", technology), 
                             ModuleType.THEORY, DifficultyLevel.BEGINNER, 8, 1, true),
            new ModuleTemplate("环境搭建", String.format("搭建%s开发环境", technology), 
                             ModuleType.PRACTICE, DifficultyLevel.BEGINNER, 4, 2, true),
            new ModuleTemplate("快速入门", String.format("%s快速入门教程", technology), 
                             ModuleType.TUTORIAL, DifficultyLevel.INTERMEDIATE, 12, 3, true),
            new ModuleTemplate("进阶学习", String.format("%s进阶特性学习", technology), 
                             ModuleType.THEORY, DifficultyLevel.ADVANCED, 16, 4, false),
            new ModuleTemplate("实战项目", String.format("使用%s完成实战项目", technology), 
                             ModuleType.PROJECT, DifficultyLevel.ADVANCED, 24, 5, true)
        );
        
        return new PathTemplate(
            String.format("全面掌握%s技术栈", technology),
            modules
        );
    }
    
    private static void initializePathTemplates() {
        // Spring框架学习路径
        PATH_TEMPLATES.put("spring", new PathTemplate(
            "全面掌握Spring生态系统，从基础到高级应用",
            Arrays.asList(
                new ModuleTemplate("Spring核心概念", "理解IoC容器、依赖注入、AOP等核心概念", 
                                 ModuleType.THEORY, DifficultyLevel.BEGINNER, 12, 1, true),
                new ModuleTemplate("Spring Boot入门", "学习Spring Boot快速开发", 
                                 ModuleType.TUTORIAL, DifficultyLevel.BEGINNER, 16, 2, true),
                new ModuleTemplate("Spring MVC", "掌握Web层开发", 
                                 ModuleType.PRACTICE, DifficultyLevel.INTERMEDIATE, 20, 3, true),
                new ModuleTemplate("Spring Data", "学习数据访问层开发", 
                                 ModuleType.PRACTICE, DifficultyLevel.INTERMEDIATE, 18, 4, true),
                new ModuleTemplate("Spring Security", "掌握安全认证授权", 
                                 ModuleType.PRACTICE, DifficultyLevel.ADVANCED, 24, 5, false),
                new ModuleTemplate("Spring Cloud微服务", "学习微服务架构", 
                                 ModuleType.THEORY, DifficultyLevel.EXPERT, 32, 6, false),
                new ModuleTemplate("电商系统项目", "完整的Spring Boot电商项目实战", 
                                 ModuleType.PROJECT, DifficultyLevel.ADVANCED, 40, 7, true)
            )
        ));
        
        // Kafka学习路径
        PATH_TEMPLATES.put("kafka", new PathTemplate(
            "掌握Apache Kafka消息队列系统，从基础到生产环境应用",
            Arrays.asList(
                new ModuleTemplate("Kafka基础概念", "理解消息队列、Topic、Partition、Consumer Group", 
                                 ModuleType.THEORY, DifficultyLevel.BEGINNER, 8, 1, true),
                new ModuleTemplate("Kafka环境搭建", "单机和集群环境搭建", 
                                 ModuleType.PRACTICE, DifficultyLevel.BEGINNER, 6, 2, true),
                new ModuleTemplate("Producer开发", "学习消息生产者开发", 
                                 ModuleType.TUTORIAL, DifficultyLevel.INTERMEDIATE, 12, 3, true),
                new ModuleTemplate("Consumer开发", "学习消息消费者开发", 
                                 ModuleType.TUTORIAL, DifficultyLevel.INTERMEDIATE, 12, 4, true),
                new ModuleTemplate("Kafka Streams", "流处理框架学习", 
                                 ModuleType.THEORY, DifficultyLevel.ADVANCED, 20, 5, false),
                new ModuleTemplate("性能调优", "Kafka性能优化和监控", 
                                 ModuleType.PRACTICE, DifficultyLevel.EXPERT, 16, 6, false),
                new ModuleTemplate("实时数据处理项目", "构建实时数据处理系统", 
                                 ModuleType.PROJECT, DifficultyLevel.ADVANCED, 32, 7, true)
            )
        ));
        
        // Netty学习路径
        PATH_TEMPLATES.put("netty", new PathTemplate(
            "掌握Netty网络编程框架，构建高性能网络应用",
            Arrays.asList(
                new ModuleTemplate("NIO基础", "理解Java NIO、Channel、Selector", 
                                 ModuleType.THEORY, DifficultyLevel.INTERMEDIATE, 10, 1, true),
                new ModuleTemplate("Netty核心概念", "EventLoop、Pipeline、Handler机制", 
                                 ModuleType.THEORY, DifficultyLevel.INTERMEDIATE, 12, 2, true),
                new ModuleTemplate("Netty入门实战", "第一个Netty服务端和客户端", 
                                 ModuleType.TUTORIAL, DifficultyLevel.INTERMEDIATE, 14, 3, true),
                new ModuleTemplate("编解码器", "学习各种编解码器的使用", 
                                 ModuleType.PRACTICE, DifficultyLevel.INTERMEDIATE, 16, 4, true),
                new ModuleTemplate("高级特性", "零拷贝、内存池、心跳机制", 
                                 ModuleType.THEORY, DifficultyLevel.ADVANCED, 18, 5, false),
                new ModuleTemplate("性能优化", "Netty性能调优最佳实践", 
                                 ModuleType.PRACTICE, DifficultyLevel.EXPERT, 20, 6, false),
                new ModuleTemplate("IM系统项目", "构建高性能即时通信系统", 
                                 ModuleType.PROJECT, DifficultyLevel.ADVANCED, 36, 7, true)
            )
        ));
    }
    
    // 内部类：路径模板
    private static class PathTemplate {
        public final String description;
        public final List<ModuleTemplate> modules;
        
        public PathTemplate(String description, List<ModuleTemplate> modules) {
            this.description = description;
            this.modules = modules;
        }
    }
    
    // 内部类：模块模板
    private static class ModuleTemplate {
        public final String title;
        public final String description;
        public final ModuleType type;
        public final DifficultyLevel difficulty;
        public final Integer estimatedHours;
        public final Integer orderIndex;
        public final Boolean isRequired;
        
        public ModuleTemplate(String title, String description, ModuleType type, 
                            DifficultyLevel difficulty, Integer estimatedHours, 
                            Integer orderIndex, Boolean isRequired) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.difficulty = difficulty;
            this.estimatedHours = estimatedHours;
            this.orderIndex = orderIndex;
            this.isRequired = isRequired;
        }
    }
}