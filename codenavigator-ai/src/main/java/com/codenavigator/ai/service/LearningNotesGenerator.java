package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult;
import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningNotesGenerator {
    
    // 知识点模板库
    private static final Map<String, KnowledgeTemplate> KNOWLEDGE_TEMPLATES = new HashMap<>();
    
    // 学习笔记结构模板
    private static final Map<String, String> NOTE_TEMPLATES = new HashMap<>();
    
    static {
        initializeKnowledgeTemplates();
        initializeNoteTemplates();
    }
    
    public LearningNotes generateLearningNotes(CodeAnalysisResult analysisResult, String userLevel, String focusArea) {
        log.info("生成学习笔记，用户级别: {}, 焦点领域: {}", userLevel, focusArea);
        
        // 分析代码问题和建议，提取学习点
        List<LearningPoint> learningPoints = extractLearningPoints(analysisResult);
        
        // 生成知识要点
        List<KnowledgePoint> knowledgePoints = generateKnowledgePoints(analysisResult, learningPoints);
        
        // 生成代码示例和对比
        List<CodeExample> codeExamples = generateCodeExamples(analysisResult, learningPoints);
        
        // 生成练习题
        List<PracticeQuestion> practiceQuestions = generatePracticeQuestions(analysisResult, userLevel);
        
        // 生成扩展阅读资料
        List<ReadingMaterial> readingMaterials = generateReadingMaterials(learningPoints, userLevel);
        
        // 生成学习总结
        String summary = generateLearningNotesSummary(analysisResult, learningPoints);
        
        // 构建完整的学习笔记
        return LearningNotes.builder()
            .id(generateNotesId())
            .title(generateNotesTitle(analysisResult, focusArea))
            .createdTime(LocalDateTime.now())
            .userLevel(userLevel)
            .focusArea(focusArea)
            .summary(summary)
            .learningPoints(learningPoints)
            .knowledgePoints(knowledgePoints)
            .codeExamples(codeExamples)
            .practiceQuestions(practiceQuestions)
            .readingMaterials(readingMaterials)
            .estimatedStudyTime(calculateStudyTime(learningPoints, userLevel))
            .tags(generateTags(analysisResult, learningPoints))
            .build();
    }
    
    public String generateMarkdownNotes(LearningNotes notes) {
        StringBuilder markdown = new StringBuilder();
        
        // 标题和基本信息
        markdown.append("# ").append(notes.getTitle()).append("\n\n");
        markdown.append("> **创建时间**: ").append(notes.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        markdown.append("> **用户级别**: ").append(notes.getUserLevel()).append("\n");
        markdown.append("> **学习重点**: ").append(notes.getFocusArea()).append("\n");
        markdown.append("> **预计学习时间**: ").append(notes.getEstimatedStudyTime()).append(" 分钟\n\n");
        
        // 学习摘要
        markdown.append("## 📝 学习摘要\n\n");
        markdown.append(notes.getSummary()).append("\n\n");
        
        // 学习要点
        if (!notes.getLearningPoints().isEmpty()) {
            markdown.append("## 🎯 核心学习点\n\n");
            for (int i = 0; i < notes.getLearningPoints().size(); i++) {
                LearningPoint point = notes.getLearningPoints().get(i);
                markdown.append("### ").append(i + 1).append(". ").append(point.getTitle()).append("\n\n");
                markdown.append(point.getDescription()).append("\n\n");
                
                if (point.getImportance() != null) {
                    markdown.append("**重要程度**: ");
                    for (int j = 0; j < point.getImportance(); j++) {
                        markdown.append("⭐");
                    }
                    markdown.append("\n\n");
                }
            }
        }
        
        // 知识要点
        if (!notes.getKnowledgePoints().isEmpty()) {
            markdown.append("## 📚 知识要点详解\n\n");
            for (KnowledgePoint kp : notes.getKnowledgePoints()) {
                markdown.append("### ").append(kp.getTitle()).append("\n\n");
                markdown.append(kp.getExplanation()).append("\n\n");
                
                if (!kp.getKeyPoints().isEmpty()) {
                    markdown.append("**要点总结**:\n");
                    for (String keyPoint : kp.getKeyPoints()) {
                        markdown.append("- ").append(keyPoint).append("\n");
                    }
                    markdown.append("\n");
                }
            }
        }
        
        // 代码示例
        if (!notes.getCodeExamples().isEmpty()) {
            markdown.append("## 💻 代码示例\n\n");
            for (CodeExample example : notes.getCodeExamples()) {
                markdown.append("### ").append(example.getTitle()).append("\n\n");
                markdown.append(example.getDescription()).append("\n\n");
                
                if (example.getBadExample() != null) {
                    markdown.append("❌ **不推荐的写法**:\n");
                    markdown.append("```java\n").append(example.getBadExample()).append("\n```\n\n");
                }
                
                if (example.getGoodExample() != null) {
                    markdown.append("✅ **推荐的写法**:\n");
                    markdown.append("```java\n").append(example.getGoodExample()).append("\n```\n\n");
                }
                
                if (example.getExplanation() != null) {
                    markdown.append("**说明**: ").append(example.getExplanation()).append("\n\n");
                }
            }
        }
        
        // 练习题
        if (!notes.getPracticeQuestions().isEmpty()) {
            markdown.append("## 🤔 思考练习\n\n");
            for (int i = 0; i < notes.getPracticeQuestions().size(); i++) {
                PracticeQuestion question = notes.getPracticeQuestions().get(i);
                markdown.append("### 练习 ").append(i + 1).append(": ").append(question.getTitle()).append("\n\n");
                markdown.append(question.getQuestion()).append("\n\n");
                
                if (question.getHint() != null) {
                    markdown.append("**提示**: ").append(question.getHint()).append("\n\n");
                }
                
                if (question.getSolution() != null) {
                    markdown.append("<details>\n<summary>点击查看答案</summary>\n\n");
                    markdown.append(question.getSolution()).append("\n\n");
                    markdown.append("</details>\n\n");
                }
            }
        }
        
        // 扩展阅读
        if (!notes.getReadingMaterials().isEmpty()) {
            markdown.append("## 📖 扩展阅读\n\n");
            for (ReadingMaterial material : notes.getReadingMaterials()) {
                markdown.append("- **").append(material.getTitle()).append("**");
                if (material.getAuthor() != null) {
                    markdown.append(" - ").append(material.getAuthor());
                }
                markdown.append("\n");
                
                if (material.getDescription() != null) {
                    markdown.append("  ").append(material.getDescription()).append("\n");
                }
                
                if (material.getUrl() != null) {
                    markdown.append("  📎 [链接](").append(material.getUrl()).append(")\n");
                }
                markdown.append("\n");
            }
        }
        
        // 学习标签
        if (!notes.getTags().isEmpty()) {
            markdown.append("## 🏷️ 标签\n\n");
            for (String tag : notes.getTags()) {
                markdown.append("`").append(tag).append("` ");
            }
            markdown.append("\n\n");
        }
        
        // 结语
        markdown.append("---\n");
        markdown.append("*本学习笔记由 CodeNavigator AI 自动生成，基于您的代码分析结果。建议结合实际编程练习来巩固所学知识。*\n");
        
        return markdown.toString();
    }
    
    private List<LearningPoint> extractLearningPoints(CodeAnalysisResult analysisResult) {
        List<LearningPoint> points = new ArrayList<>();
        Set<String> addedPoints = new HashSet<>(); // 避免重复
        
        // 从问题中提取学习点
        for (CodeIssue issue : analysisResult.getIssues()) {
            String pointKey = issue.getType() + "_" + issue.getCategory();
            if (!addedPoints.contains(pointKey)) {
                LearningPoint point = createLearningPointFromIssue(issue);
                if (point != null) {
                    points.add(point);
                    addedPoints.add(pointKey);
                }
            }
        }
        
        // 从建议中提取学习点
        for (CodeSuggestion suggestion : analysisResult.getSuggestions()) {
            String pointKey = suggestion.getCategory() + "_suggestion";
            if (!addedPoints.contains(pointKey)) {
                LearningPoint point = createLearningPointFromSuggestion(suggestion);
                if (point != null) {
                    points.add(point);
                    addedPoints.add(pointKey);
                }
            }
        }
        
        // 根据质量指标补充学习点
        QualityMetrics metrics = analysisResult.getMetrics();
        if (metrics.getCodeStyle() != null && metrics.getCodeStyle() < 70) {
            points.add(LearningPoint.builder()
                .title("Java代码规范")
                .description("学习和遵循Java编码规范，提高代码的专业性和可读性")
                .category("STYLE")
                .importance(4)
                .difficulty("BEGINNER")
                .build());
        }
        
        if (metrics.getSecurity() != null && metrics.getSecurity() < 70) {
            points.add(LearningPoint.builder()
                .title("安全编程实践")
                .description("了解常见的安全漏洞并学习如何编写安全的代码")
                .category("SECURITY")
                .importance(5)
                .difficulty("INTERMEDIATE")
                .build());
        }
        
        return points.stream()
            .sorted((p1, p2) -> Integer.compare(p2.getImportance(), p1.getImportance()))
            .limit(8) // 限制学习点数量
            .collect(Collectors.toList());
    }
    
    private List<KnowledgePoint> generateKnowledgePoints(CodeAnalysisResult result, List<LearningPoint> learningPoints) {
        List<KnowledgePoint> knowledgePoints = new ArrayList<>();
        
        for (LearningPoint point : learningPoints) {
            KnowledgeTemplate template = KNOWLEDGE_TEMPLATES.get(point.getCategory());
            if (template != null) {
                KnowledgePoint kp = KnowledgePoint.builder()
                    .title(point.getTitle())
                    .explanation(generateDetailedExplanation(point, template))
                    .keyPoints(template.getKeyPoints())
                    .category(point.getCategory())
                    .build();
                knowledgePoints.add(kp);
            }
        }
        
        return knowledgePoints;
    }
    
    private List<CodeExample> generateCodeExamples(CodeAnalysisResult result, List<LearningPoint> learningPoints) {
        List<CodeExample> examples = new ArrayList<>();
        
        for (LearningPoint point : learningPoints) {
            if (shouldIncludeCodeExample(point)) {
                CodeExample example = generateCodeExampleForPoint(point, result);
                if (example != null) {
                    examples.add(example);
                }
            }
        }
        
        return examples;
    }
    
    private List<PracticeQuestion> generatePracticeQuestions(CodeAnalysisResult result, String userLevel) {
        List<PracticeQuestion> questions = new ArrayList<>();
        
        // 根据发现的问题生成练习题
        Map<String, Long> categoryCount = result.getIssues().stream()
            .collect(Collectors.groupingBy(CodeIssue::getCategory, Collectors.counting()));
        
        if (categoryCount.containsKey("STYLE")) {
            questions.add(PracticeQuestion.builder()
                .title("代码规范修正")
                .question("请找出以下代码中的命名规范问题，并给出修正建议")
                .type("CODE_REVIEW")
                .difficulty(userLevel)
                .hint("注意变量名、方法名和类名的命名约定")
                .solution("变量名应使用驼峰命名法，类名首字母大写，常量全部大写并用下划线分隔")
                .build());
        }
        
        if (categoryCount.containsKey("PERFORMANCE")) {
            questions.add(PracticeQuestion.builder()
                .title("性能优化思考")
                .question("分析给定代码的性能问题，提出3种可能的优化方案")
                .type("ANALYSIS")
                .difficulty(userLevel)
                .hint("考虑算法复杂度、数据结构选择、内存使用等方面")
                .solution("1. 使用StringBuilder替代字符串拼接 2. 为集合指定初始容量 3. 避免在循环中创建大量对象")
                .build());
        }
        
        if (categoryCount.containsKey("SECURITY")) {
            questions.add(PracticeQuestion.builder()
                .title("安全漏洞识别")
                .question("识别代码中可能存在的安全风险，并说明如何修复")
                .type("SECURITY_AUDIT")
                .difficulty("INTERMEDIATE")
                .hint("注意SQL注入、XSS攻击、密码存储等常见安全问题")
                .solution("使用参数化查询防止SQL注入，对用户输入进行验证和转义，使用加密算法存储敏感信息")
                .build());
        }
        
        return questions;
    }
    
    private List<ReadingMaterial> generateReadingMaterials(List<LearningPoint> learningPoints, String userLevel) {
        List<ReadingMaterial> materials = new ArrayList<>();
        Set<String> categories = learningPoints.stream()
            .map(LearningPoint::getCategory)
            .collect(Collectors.toSet());
        
        if (categories.contains("STYLE")) {
            materials.add(ReadingMaterial.builder()
                .title("Google Java Style Guide")
                .description("Google官方Java代码规范指南")
                .url("https://google.github.io/styleguide/javaguide.html")
                .type("OFFICIAL_GUIDE")
                .difficulty("BEGINNER")
                .build());
        }
        
        if (categories.contains("SECURITY")) {
            materials.add(ReadingMaterial.builder()
                .title("OWASP Top 10")
                .description("最常见的Web应用安全风险")
                .url("https://owasp.org/www-project-top-ten/")
                .type("SECURITY_GUIDE")
                .difficulty("INTERMEDIATE")
                .build());
        }
        
        if (categories.contains("PERFORMANCE")) {
            materials.add(ReadingMaterial.builder()
                .title("Java Performance Tuning Guide")
                .author("Oracle")
                .description("Oracle官方Java性能调优指南")
                .type("PERFORMANCE_GUIDE")
                .difficulty("ADVANCED")
                .build());
        }
        
        // 根据用户级别添加通用资料
        if ("BEGINNER".equals(userLevel)) {
            materials.add(ReadingMaterial.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .description("Java编程最佳实践经典书籍")
                .type("BOOK")
                .difficulty("BEGINNER")
                .build());
        } else if ("ADVANCED".equals(userLevel)) {
            materials.add(ReadingMaterial.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .description("软件架构设计和最佳实践")
                .type("BOOK")
                .difficulty("ADVANCED")
                .build());
        }
        
        return materials;
    }
    
    // 辅助方法
    private String generateNotesId() {
        return "LN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String generateNotesTitle(CodeAnalysisResult result, String focusArea) {
        if (focusArea != null && !focusArea.isEmpty()) {
            return String.format("%s学习笔记 - 基于代码分析", focusArea);
        }
        
        String level = result.getQualityLevel().getDescription();
        return String.format("代码质量提升学习笔记 (%s级别)", level);
    }
    
    private String generateLearningNotesSummary(CodeAnalysisResult result, List<LearningPoint> learningPoints) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("本次学习笔记基于您的代码分析结果生成。");
        summary.append(String.format("代码整体质量评分为 %d/100，属于%s水平。", 
            result.getOverallScore(), result.getQualityLevel().getDescription()));
        
        if (!result.getIssues().isEmpty()) {
            long criticalCount = result.getIssues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.CRITICAL)
                .count();
            if (criticalCount > 0) {
                summary.append(String.format("发现 %d 个严重问题需要优先关注。", criticalCount));
            }
        }
        
        if (!learningPoints.isEmpty()) {
            summary.append(String.format("本笔记总结了 %d 个核心学习点，", learningPoints.size()));
            summary.append("建议按照重要程度逐一学习和实践。");
        }
        
        summary.append("通过系统学习这些知识点，您的编程技能将得到显著提升。");
        
        return summary.toString();
    }
    
    private int calculateStudyTime(List<LearningPoint> learningPoints, String userLevel) {
        int baseTime = learningPoints.size() * 15; // 每个学习点15分钟
        
        // 根据用户级别调整
        switch (userLevel) {
            case "BEGINNER":
                return (int) (baseTime * 1.5);
            case "ADVANCED":
                return (int) (baseTime * 0.8);
            default:
                return baseTime;
        }
    }
    
    private List<String> generateTags(CodeAnalysisResult result, List<LearningPoint> learningPoints) {
        Set<String> tags = new HashSet<>();
        
        // 从学习点生成标签
        learningPoints.forEach(point -> {
            tags.add(point.getCategory().toLowerCase());
            if (point.getDifficulty() != null) {
                tags.add(point.getDifficulty().toLowerCase());
            }
        });
        
        // 从质量级别生成标签
        tags.add(result.getQualityLevel().name().toLowerCase());
        
        // 添加通用标签
        tags.add("java");
        tags.add("code-review");
        tags.add("learning-notes");
        
        return new ArrayList<>(tags);
    }
    
    private LearningPoint createLearningPointFromIssue(CodeIssue issue) {
        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("NAMING_VIOLATION", "Java命名规范");
        titleMap.put("MAGIC_NUMBER", "常量定义与使用");
        titleMap.put("LONG_METHOD", "方法拆分与重构");
        titleMap.put("EMPTY_CATCH", "异常处理最佳实践");
        titleMap.put("SQL_INJECTION", "SQL注入防护");
        
        String title = titleMap.getOrDefault(issue.getType(), "代码改进");
        
        return LearningPoint.builder()
            .title(title)
            .description(generateLearningDescription(issue))
            .category(issue.getCategory())
            .importance(mapSeverityToImportance(issue.getSeverity()))
            .difficulty(mapSeverityToDifficulty(issue.getSeverity()))
            .build();
    }
    
    private LearningPoint createLearningPointFromSuggestion(CodeSuggestion suggestion) {
        return LearningPoint.builder()
            .title(suggestion.getTitle())
            .description(suggestion.getDescription())
            .category(suggestion.getCategory())
            .importance(mapPriorityToImportance(suggestion.getPriority()))
            .difficulty(suggestion.getDifficultyLevel())
            .build();
    }
    
    private String generateLearningDescription(CodeIssue issue) {
        Map<String, String> descriptionMap = new HashMap<>();
        descriptionMap.put("NAMING_VIOLATION", "学习Java命名约定，包括驼峰命名法、常量命名等规则");
        descriptionMap.put("MAGIC_NUMBER", "理解魔法数字的问题，学习如何正确定义和使用常量");
        descriptionMap.put("LONG_METHOD", "掌握方法拆分技巧，提高代码的可读性和可维护性");
        descriptionMap.put("EMPTY_CATCH", "学习正确的异常处理方式，避免隐藏错误");
        descriptionMap.put("SQL_INJECTION", "了解SQL注入攻击原理和防护措施");
        
        return descriptionMap.getOrDefault(issue.getType(), issue.getDescription());
    }
    
    private String generateDetailedExplanation(LearningPoint point, KnowledgeTemplate template) {
        return template.getBaseExplanation() + "\n\n" + 
               "在您的代码中，这个知识点特别重要，因为它直接影响到代码的" + 
               template.getImpactArea() + "。建议重点关注并在实际编程中应用。";
    }
    
    private boolean shouldIncludeCodeExample(LearningPoint point) {
        return Arrays.asList("STYLE", "PERFORMANCE", "SECURITY", "MAINTAINABILITY")
            .contains(point.getCategory());
    }
    
    private CodeExample generateCodeExampleForPoint(LearningPoint point, CodeAnalysisResult result) {
        Map<String, CodeExampleTemplate> templates = new HashMap<>();
        
        templates.put("STYLE", CodeExampleTemplate.builder()
            .title("代码规范示例")
            .description("对比规范和不规范的代码写法")
            .badExample("String user_name = \"john\";\nint MAX_COUNT = 100;")
            .goodExample("String userName = \"john\";\nprivate static final int MAX_COUNT = 100;")
            .explanation("使用驼峰命名法，常量用大写字母和下划线")
            .build());
            
        templates.put("PERFORMANCE", CodeExampleTemplate.builder()
            .title("性能优化示例")
            .description("对比低效和高效的代码实现")
            .badExample("String result = \"\";\nfor (String item : items) {\n    result += item;\n}")
            .goodExample("StringBuilder sb = new StringBuilder();\nfor (String item : items) {\n    sb.append(item);\n}\nString result = sb.toString();")
            .explanation("使用StringBuilder避免频繁的字符串对象创建")
            .build());
        
        CodeExampleTemplate template = templates.get(point.getCategory());
        if (template != null) {
            return CodeExample.builder()
                .title(template.getTitle())
                .description(template.getDescription())
                .badExample(template.getBadExample())
                .goodExample(template.getGoodExample())
                .explanation(template.getExplanation())
                .category(point.getCategory())
                .build();
        }
        
        return null;
    }
    
    private int mapSeverityToImportance(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL: return 5;
            case HIGH: return 4;
            case MEDIUM: return 3;
            case LOW: return 2;
            default: return 1;
        }
    }
    
    private String mapSeverityToDifficulty(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL:
            case HIGH:
                return "INTERMEDIATE";
            case MEDIUM:
                return "BEGINNER";
            case LOW:
            case INFO:
            default:
                return "BEGINNER";
        }
    }
    
    private int mapPriorityToImportance(Priority priority) {
        switch (priority) {
            case HIGH: return 4;
            case MEDIUM: return 3;
            case LOW: return 2;
            default: return 1;
        }
    }
    
    private static void initializeKnowledgeTemplates() {
        KNOWLEDGE_TEMPLATES.put("STYLE", KnowledgeTemplate.builder()
            .baseExplanation("Java代码规范是保证代码质量和团队协作的基础")
            .impactArea("代码可读性和维护性")
            .keyPoints(Arrays.asList(
                "使用驼峰命名法命名变量和方法",
                "类名首字母大写",
                "常量全部大写，用下划线分隔",
                "合理使用缩进和空格",
                "避免过长的行和方法"
            ))
            .build());
            
        KNOWLEDGE_TEMPLATES.put("PERFORMANCE", KnowledgeTemplate.builder()
            .baseExplanation("性能优化是提升程序运行效率的关键技术")
            .impactArea("程序执行效率和资源使用")
            .keyPoints(Arrays.asList(
                "选择合适的数据结构和算法",
                "避免不必要的对象创建",
                "合理使用缓存机制",
                "优化数据库查询",
                "注意内存泄漏问题"
            ))
            .build());
            
        KNOWLEDGE_TEMPLATES.put("SECURITY", KnowledgeTemplate.builder()
            .baseExplanation("安全编程是保护应用和用户数据的重要措施")
            .impactArea("系统安全性和数据保护")
            .keyPoints(Arrays.asList(
                "防止SQL注入攻击",
                "验证和过滤用户输入",
                "使用HTTPS传输敏感数据",
                "正确处理密码和敏感信息",
                "实施访问控制和权限管理"
            ))
            .build());
    }
    
    private static void initializeNoteTemplates() {
        NOTE_TEMPLATES.put("COMPREHENSIVE", "全面的学习笔记，包含理论和实践");
        NOTE_TEMPLATES.put("FOCUSED", "针对特定问题的集中学习");
        NOTE_TEMPLATES.put("QUICK", "快速学习要点总结");
    }
    
    // 内部类定义
    public static class LearningNotes {
        private String id;
        private String title;
        private LocalDateTime createdTime;
        private String userLevel;
        private String focusArea;
        private String summary;
        private List<LearningPoint> learningPoints;
        private List<KnowledgePoint> knowledgePoints;
        private List<CodeExample> codeExamples;
        private List<PracticeQuestion> practiceQuestions;
        private List<ReadingMaterial> readingMaterials;
        private int estimatedStudyTime;
        private List<String> tags;
        
        public static LearningNotesBuilder builder() {
            return new LearningNotesBuilder();
        }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public String getUserLevel() { return userLevel; }
        public String getFocusArea() { return focusArea; }
        public String getSummary() { return summary; }
        public List<LearningPoint> getLearningPoints() { return learningPoints; }
        public List<KnowledgePoint> getKnowledgePoints() { return knowledgePoints; }
        public List<CodeExample> getCodeExamples() { return codeExamples; }
        public List<PracticeQuestion> getPracticeQuestions() { return practiceQuestions; }
        public List<ReadingMaterial> getReadingMaterials() { return readingMaterials; }
        public int getEstimatedStudyTime() { return estimatedStudyTime; }
        public List<String> getTags() { return tags; }
        
        public static class LearningNotesBuilder {
            private LearningNotes notes = new LearningNotes();
            
            public LearningNotesBuilder id(String id) { notes.id = id; return this; }
            public LearningNotesBuilder title(String title) { notes.title = title; return this; }
            public LearningNotesBuilder createdTime(LocalDateTime createdTime) { notes.createdTime = createdTime; return this; }
            public LearningNotesBuilder userLevel(String userLevel) { notes.userLevel = userLevel; return this; }
            public LearningNotesBuilder focusArea(String focusArea) { notes.focusArea = focusArea; return this; }
            public LearningNotesBuilder summary(String summary) { notes.summary = summary; return this; }
            public LearningNotesBuilder learningPoints(List<LearningPoint> learningPoints) { notes.learningPoints = learningPoints; return this; }
            public LearningNotesBuilder knowledgePoints(List<KnowledgePoint> knowledgePoints) { notes.knowledgePoints = knowledgePoints; return this; }
            public LearningNotesBuilder codeExamples(List<CodeExample> codeExamples) { notes.codeExamples = codeExamples; return this; }
            public LearningNotesBuilder practiceQuestions(List<PracticeQuestion> practiceQuestions) { notes.practiceQuestions = practiceQuestions; return this; }
            public LearningNotesBuilder readingMaterials(List<ReadingMaterial> readingMaterials) { notes.readingMaterials = readingMaterials; return this; }
            public LearningNotesBuilder estimatedStudyTime(int estimatedStudyTime) { notes.estimatedStudyTime = estimatedStudyTime; return this; }
            public LearningNotesBuilder tags(List<String> tags) { notes.tags = tags; return this; }
            
            public LearningNotes build() { return notes; }
        }
    }
    
    public static class LearningPoint {
        private String title;
        private String description;
        private String category;
        private Integer importance; // 1-5
        private String difficulty;
        
        public static LearningPointBuilder builder() { return new LearningPointBuilder(); }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public Integer getImportance() { return importance; }
        public String getDifficulty() { return difficulty; }
        
        public static class LearningPointBuilder {
            private LearningPoint point = new LearningPoint();
            
            public LearningPointBuilder title(String title) { point.title = title; return this; }
            public LearningPointBuilder description(String description) { point.description = description; return this; }
            public LearningPointBuilder category(String category) { point.category = category; return this; }
            public LearningPointBuilder importance(Integer importance) { point.importance = importance; return this; }
            public LearningPointBuilder difficulty(String difficulty) { point.difficulty = difficulty; return this; }
            
            public LearningPoint build() { return point; }
        }
    }
    
    public static class KnowledgePoint {
        private String title;
        private String explanation;
        private List<String> keyPoints;
        private String category;
        
        public static KnowledgePointBuilder builder() { return new KnowledgePointBuilder(); }
        
        public String getTitle() { return title; }
        public String getExplanation() { return explanation; }
        public List<String> getKeyPoints() { return keyPoints; }
        public String getCategory() { return category; }
        
        public static class KnowledgePointBuilder {
            private KnowledgePoint point = new KnowledgePoint();
            
            public KnowledgePointBuilder title(String title) { point.title = title; return this; }
            public KnowledgePointBuilder explanation(String explanation) { point.explanation = explanation; return this; }
            public KnowledgePointBuilder keyPoints(List<String> keyPoints) { point.keyPoints = keyPoints; return this; }
            public KnowledgePointBuilder category(String category) { point.category = category; return this; }
            
            public KnowledgePoint build() { return point; }
        }
    }
    
    public static class CodeExample {
        private String title;
        private String description;
        private String badExample;
        private String goodExample;
        private String explanation;
        private String category;
        
        public static CodeExampleBuilder builder() { return new CodeExampleBuilder(); }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getBadExample() { return badExample; }
        public String getGoodExample() { return goodExample; }
        public String getExplanation() { return explanation; }
        public String getCategory() { return category; }
        
        public static class CodeExampleBuilder {
            private CodeExample example = new CodeExample();
            
            public CodeExampleBuilder title(String title) { example.title = title; return this; }
            public CodeExampleBuilder description(String description) { example.description = description; return this; }
            public CodeExampleBuilder badExample(String badExample) { example.badExample = badExample; return this; }
            public CodeExampleBuilder goodExample(String goodExample) { example.goodExample = goodExample; return this; }
            public CodeExampleBuilder explanation(String explanation) { example.explanation = explanation; return this; }
            public CodeExampleBuilder category(String category) { example.category = category; return this; }
            
            public CodeExample build() { return example; }
        }
    }
    
    public static class PracticeQuestion {
        private String title;
        private String question;
        private String type;
        private String difficulty;
        private String hint;
        private String solution;
        
        public static PracticeQuestionBuilder builder() { return new PracticeQuestionBuilder(); }
        
        public String getTitle() { return title; }
        public String getQuestion() { return question; }
        public String getType() { return type; }
        public String getDifficulty() { return difficulty; }
        public String getHint() { return hint; }
        public String getSolution() { return solution; }
        
        public static class PracticeQuestionBuilder {
            private PracticeQuestion question = new PracticeQuestion();
            
            public PracticeQuestionBuilder title(String title) { question.title = title; return this; }
            public PracticeQuestionBuilder question(String q) { question.question = q; return this; }
            public PracticeQuestionBuilder type(String type) { question.type = type; return this; }
            public PracticeQuestionBuilder difficulty(String difficulty) { question.difficulty = difficulty; return this; }
            public PracticeQuestionBuilder hint(String hint) { question.hint = hint; return this; }
            public PracticeQuestionBuilder solution(String solution) { question.solution = solution; return this; }
            
            public PracticeQuestion build() { return question; }
        }
    }
    
    public static class ReadingMaterial {
        private String title;
        private String author;
        private String description;
        private String url;
        private String type;
        private String difficulty;
        
        public static ReadingMaterialBuilder builder() { return new ReadingMaterialBuilder(); }
        
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public String getType() { return type; }
        public String getDifficulty() { return difficulty; }
        
        public static class ReadingMaterialBuilder {
            private ReadingMaterial material = new ReadingMaterial();
            
            public ReadingMaterialBuilder title(String title) { material.title = title; return this; }
            public ReadingMaterialBuilder author(String author) { material.author = author; return this; }
            public ReadingMaterialBuilder description(String description) { material.description = description; return this; }
            public ReadingMaterialBuilder url(String url) { material.url = url; return this; }
            public ReadingMaterialBuilder type(String type) { material.type = type; return this; }
            public ReadingMaterialBuilder difficulty(String difficulty) { material.difficulty = difficulty; return this; }
            
            public ReadingMaterial build() { return material; }
        }
    }
    
    private static class KnowledgeTemplate {
        private String baseExplanation;
        private String impactArea;
        private List<String> keyPoints;
        
        public static KnowledgeTemplateBuilder builder() { return new KnowledgeTemplateBuilder(); }
        
        public String getBaseExplanation() { return baseExplanation; }
        public String getImpactArea() { return impactArea; }
        public List<String> getKeyPoints() { return keyPoints; }
        
        public static class KnowledgeTemplateBuilder {
            private KnowledgeTemplate template = new KnowledgeTemplate();
            
            public KnowledgeTemplateBuilder baseExplanation(String baseExplanation) { 
                template.baseExplanation = baseExplanation; return this; }
            public KnowledgeTemplateBuilder impactArea(String impactArea) { 
                template.impactArea = impactArea; return this; }
            public KnowledgeTemplateBuilder keyPoints(List<String> keyPoints) { 
                template.keyPoints = keyPoints; return this; }
            
            public KnowledgeTemplate build() { return template; }
        }
    }
    
    private static class CodeExampleTemplate {
        private String title;
        private String description;
        private String badExample;
        private String goodExample;
        private String explanation;
        
        public static CodeExampleTemplateBuilder builder() { return new CodeExampleTemplateBuilder(); }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getBadExample() { return badExample; }
        public String getGoodExample() { return goodExample; }
        public String getExplanation() { return explanation; }
        
        public static class CodeExampleTemplateBuilder {
            private CodeExampleTemplate template = new CodeExampleTemplate();
            
            public CodeExampleTemplateBuilder title(String title) { template.title = title; return this; }
            public CodeExampleTemplateBuilder description(String description) { template.description = description; return this; }
            public CodeExampleTemplateBuilder badExample(String badExample) { template.badExample = badExample; return this; }
            public CodeExampleTemplateBuilder goodExample(String goodExample) { template.goodExample = goodExample; return this; }
            public CodeExampleTemplateBuilder explanation(String explanation) { template.explanation = explanation; return this; }
            
            public CodeExampleTemplate build() { return template; }
        }
    }
}