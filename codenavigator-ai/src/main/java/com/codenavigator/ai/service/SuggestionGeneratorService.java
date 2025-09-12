package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionGeneratorService {
    
    // 代码模式和对应的改进建议模板
    private static final Map<String, SuggestionRule> SUGGESTION_RULES = new HashMap<>();
    
    // 代码改进的最佳实践模板
    private static final Map<String, String> BEST_PRACTICE_TEMPLATES = new HashMap<>();
    
    static {
        initializeSuggestionRules();
        initializeBestPracticeTemplates();
    }
    
    public List<CodeSuggestion> generateImprovementSuggestions(String code, String language, List<CodeIssue> existingIssues) {
        log.info("生成代码改进建议，语言: {}, 代码行数: {}", language, code.split("\n").length);
        
        List<CodeSuggestion> suggestions = new ArrayList<>();
        String[] lines = code.split("\n");
        
        // 基于代码模式生成建议
        suggestions.addAll(generatePatternBasedSuggestions(code, lines));
        
        // 基于已知问题生成建议
        suggestions.addAll(generateIssueBasedSuggestions(existingIssues, code));
        
        // 基于代码结构生成建议
        suggestions.addAll(generateStructuralSuggestions(code, lines));
        
        // 基于性能优化生成建议
        suggestions.addAll(generatePerformanceSuggestions(code, lines));
        
        // 基于安全性生成建议
        suggestions.addAll(generateSecuritySuggestions(code, lines));
        
        // 基于可维护性生成建议
        suggestions.addAll(generateMaintainabilitySuggestions(code, lines));
        
        // 去重并排序
        suggestions = deduplicateAndRankSuggestions(suggestions);
        
        log.info("生成了 {} 条改进建议", suggestions.size());
        return suggestions;
    }
    
    private List<CodeSuggestion> generatePatternBasedSuggestions(String code, String[] lines) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        for (Map.Entry<String, SuggestionRule> entry : SUGGESTION_RULES.entrySet()) {
            SuggestionRule rule = entry.getValue();
            Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            
            while (matcher.find()) {
                int lineNumber = getLineNumber(code, matcher.start());
                
                CodeSuggestion suggestion = CodeSuggestion.builder()
                    .title(rule.getTitle())
                    .description(rule.getDescription())
                    .category(rule.getCategory())
                    .priority(rule.getPriority())
                    .lineNumber(lineNumber)
                    .codeExample(rule.getExampleCode())
                    .estimatedImpact(rule.getEstimatedImpact())
                    .difficultyLevel(rule.getDifficultyLevel())
                    .tags(Arrays.asList(rule.getTags().split(",")))
                    .build();
                
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    
    private List<CodeSuggestion> generateIssueBasedSuggestions(List<CodeIssue> issues, String code) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        for (CodeIssue issue : issues) {
            CodeSuggestion suggestion = createSuggestionFromIssue(issue, code);
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    
    private List<CodeSuggestion> generateStructuralSuggestions(String code, String[] lines) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        // 检查长方法
        if (lines.length > 50) {
            suggestions.add(CodeSuggestion.builder()
                .title("考虑拆分长方法")
                .description("当前方法行数较多，建议将其拆分为多个较小的方法以提高可读性")
                .category("STRUCTURE")
                .priority(Priority.MEDIUM)
                .estimatedImpact(25)
                .difficultyLevel("MEDIUM")
                .codeExample("// 示例：将复杂方法拆分\npublic void complexMethod() {\n    validateInput();\n    processData();\n    generateResult();\n}")
                .tags(Arrays.asList("refactoring", "clean-code", "structure"))
                .build());
        }
        
        // 检查嵌套层级过深
        int maxNesting = calculateMaxNesting(code);
        if (maxNesting > 4) {
            suggestions.add(CodeSuggestion.builder()
                .title("减少代码嵌套层级")
                .description("代码嵌套层级过深，建议使用提前返回或提取方法来降低复杂度")
                .category("STRUCTURE")
                .priority(Priority.HIGH)
                .estimatedImpact(30)
                .difficultyLevel("MEDIUM")
                .codeExample("// 使用提前返回减少嵌套\nif (condition) {\n    return;\n}\n// 继续处理...")
                .tags(Arrays.asList("complexity", "readability", "refactoring"))
                .build());
        }
        
        // 检查重复代码
        List<String> duplicateLines = findDuplicateLines(lines);
        if (!duplicateLines.isEmpty()) {
            suggestions.add(CodeSuggestion.builder()
                .title("消除重复代码")
                .description("发现重复的代码片段，建议提取为公共方法或常量")
                .category("STRUCTURE")
                .priority(Priority.MEDIUM)
                .estimatedImpact(20)
                .difficultyLevel("EASY")
                .codeExample("// 提取重复代码为方法\nprivate void commonOperation() {\n    // 公共逻辑\n}")
                .tags(Arrays.asList("DRY", "refactoring", "maintainability"))
                .build());
        }
        
        return suggestions;
    }
    
    private List<CodeSuggestion> generatePerformanceSuggestions(String code, String[] lines) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        // 检查字符串拼接
        if (code.contains("String") && (code.contains("+") || code.contains("concat"))) {
            boolean inLoop = code.contains("for") || code.contains("while");
            if (inLoop) {
                suggestions.add(CodeSuggestion.builder()
                    .title("在循环中使用StringBuilder")
                    .description("在循环中进行字符串拼接效率较低，建议使用StringBuilder")
                    .category("PERFORMANCE")
                    .priority(Priority.HIGH)
                    .estimatedImpact(40)
                    .difficultyLevel("EASY")
                    .codeExample("StringBuilder sb = new StringBuilder();\nfor (String item : items) {\n    sb.append(item);\n}\nString result = sb.toString();")
                    .tags(Arrays.asList("performance", "strings", "optimization"))
                    .build());
            }
        }
        
        // 检查集合初始化容量
        if (code.contains("new ArrayList()") || code.contains("new HashMap()")) {
            suggestions.add(CodeSuggestion.builder()
                .title("指定集合初始容量")
                .description("为避免频繁扩容，建议在创建集合时指定合适的初始容量")
                .category("PERFORMANCE")
                .priority(Priority.MEDIUM)
                .estimatedImpact(15)
                .difficultyLevel("EASY")
                .codeExample("List<String> list = new ArrayList<>(expectedSize);\nMap<String, Object> map = new HashMap<>(expectedSize);")
                .tags(Arrays.asList("performance", "collections", "memory"))
                .build());
        }
        
        // 检查不必要的装箱拆箱
        if (code.contains("Integer.valueOf") || code.contains("Double.valueOf")) {
            suggestions.add(CodeSuggestion.builder()
                .title("避免不必要的装箱操作")
                .description("频繁的装箱拆箱会影响性能，考虑使用基本类型")
                .category("PERFORMANCE")
                .priority(Priority.LOW)
                .estimatedImpact(10)
                .difficultyLevel("EASY")
                .codeExample("// 使用基本类型而不是包装类型\nint value = 10; // 而不是 Integer value = Integer.valueOf(10);")
                .tags(Arrays.asList("performance", "boxing", "primitives"))
                .build());
        }
        
        return suggestions;
    }
    
    private List<CodeSuggestion> generateSecuritySuggestions(String code, String[] lines) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        // 检查SQL注入风险
        if (code.contains("Statement") && (code.contains("executeQuery") || code.contains("executeUpdate"))) {
            boolean hasConcatenation = code.contains("\"+") || code.contains("\" +");
            if (hasConcatenation) {
                suggestions.add(CodeSuggestion.builder()
                    .title("使用预编译语句防止SQL注入")
                    .description("字符串拼接构建SQL语句存在注入风险，建议使用PreparedStatement")
                    .category("SECURITY")
                    .priority(Priority.CRITICAL)
                    .estimatedImpact(90)
                    .difficultyLevel("MEDIUM")
                    .codeExample("PreparedStatement pstmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");\npstmt.setInt(1, userId);")
                    .tags(Arrays.asList("security", "sql-injection", "database"))
                    .build());
            }
        }
        
        // 检查密码硬编码
        if (code.contains("password") && (code.contains("=\"") || code.contains("= \""))) {
            suggestions.add(CodeSuggestion.builder()
                .title("避免密码硬编码")
                .description("代码中发现可能的密码硬编码，建议使用配置文件或环境变量")
                .category("SECURITY")
                .priority(Priority.HIGH)
                .estimatedImpact(80)
                .difficultyLevel("EASY")
                .codeExample("// 从配置文件或环境变量读取\nString password = System.getenv(\"DB_PASSWORD\");\n// 或使用 @Value(\"${db.password}\") 注解")
                .tags(Arrays.asList("security", "credentials", "configuration"))
                .build());
            }
        
        // 检查文件路径遍历风险
        if (code.contains("File") && code.contains("new File")) {
            suggestions.add(CodeSuggestion.builder()
                .title("验证文件路径防止路径遍历攻击")
                .description("直接使用用户输入构建文件路径存在安全风险，需要进行路径验证")
                .category("SECURITY")
                .priority(Priority.HIGH)
                .estimatedImpact(70)
                .difficultyLevel("MEDIUM")
                .codeExample("// 验证文件路径\nString safePath = Paths.get(basePath).resolve(fileName).normalize().toString();\nif (!safePath.startsWith(basePath)) {\n    throw new SecurityException(\"Invalid file path\");\n}")
                .tags(Arrays.asList("security", "file-system", "path-traversal"))
                .build());
        }
        
        return suggestions;
    }
    
    private List<CodeSuggestion> generateMaintainabilitySuggestions(String code, String[] lines) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        // 检查魔法数字
        Pattern magicNumberPattern = Pattern.compile("\\b(\\d{2,})\\b");
        Matcher matcher = magicNumberPattern.matcher(code);
        Set<String> magicNumbers = new HashSet<>();
        
        while (matcher.find()) {
            String number = matcher.group(1);
            if (!number.equals("100") && !number.equals("1000")) { // 排除常见的非魔法数字
                magicNumbers.add(number);
            }
        }
        
        if (!magicNumbers.isEmpty()) {
            suggestions.add(CodeSuggestion.builder()
                .title("使用常量替代魔法数字")
                .description("代码中包含魔法数字，建议定义为有意义的常量")
                .category("MAINTAINABILITY")
                .priority(Priority.MEDIUM)
                .estimatedImpact(25)
                .difficultyLevel("EASY")
                .codeExample("private static final int MAX_RETRY_ATTEMPTS = 3;\nprivate static final int TIMEOUT_SECONDS = 30;")
                .tags(Arrays.asList("maintainability", "constants", "magic-numbers"))
                .build());
        }
        
        // 检查注释覆盖率
        long commentLines = Arrays.stream(lines)
            .filter(line -> line.trim().startsWith("//") || line.trim().startsWith("*"))
            .count();
        
        double commentRatio = (double) commentLines / lines.length;
        if (commentRatio < 0.1) {
            suggestions.add(CodeSuggestion.builder()
                .title("增加代码注释")
                .description("代码注释较少，建议添加必要的注释说明复杂逻辑")
                .category("MAINTAINABILITY")
                .priority(Priority.LOW)
                .estimatedImpact(20)
                .difficultyLevel("EASY")
                .codeExample("/**\n * 计算用户积分奖励\n * @param userId 用户ID\n * @param action 用户行为\n * @return 积分值\n */")
                .tags(Arrays.asList("documentation", "comments", "maintainability"))
                .build());
        }
        
        // 检查异常处理
        if (code.contains("try") && !code.contains("finally") && !code.contains("try-with-resources")) {
            suggestions.add(CodeSuggestion.builder()
                .title("改进异常处理")
                .description("建议使用try-with-resources或确保资源在finally块中关闭")
                .category("MAINTAINABILITY")
                .priority(Priority.MEDIUM)
                .estimatedImpact(30)
                .difficultyLevel("EASY")
                .codeExample("try (FileInputStream fis = new FileInputStream(file)) {\n    // 处理文件\n} catch (IOException e) {\n    log.error(\"File processing failed\", e);\n}")
                .tags(Arrays.asList("exception-handling", "resources", "maintainability"))
                .build());
        }
        
        return suggestions;
    }
    
    private CodeSuggestion createSuggestionFromIssue(CodeIssue issue, String code) {
        Priority priority = mapSeverityToPriority(issue.getSeverity());
        String category = mapIssueCategoryToSuggestionCategory(issue.getCategory());
        
        String suggestionTitle = generateSuggestionTitle(issue);
        String suggestionDescription = generateSuggestionDescription(issue);
        String codeExample = generateCodeExample(issue);
        
        return CodeSuggestion.builder()
            .title(suggestionTitle)
            .description(suggestionDescription)
            .category(category)
            .priority(priority)
            .lineNumber(issue.getLineNumber())
            .codeExample(codeExample)
            .estimatedImpact(calculateEstimatedImpact(issue))
            .difficultyLevel(mapSeverityToDifficulty(issue.getSeverity()))
            .tags(generateTags(issue))
            .build();
    }
    
    private List<CodeSuggestion> deduplicateAndRankSuggestions(List<CodeSuggestion> suggestions) {
        // 根据标题和类别去重
        Map<String, CodeSuggestion> uniqueSuggestions = suggestions.stream()
            .collect(Collectors.toMap(
                s -> s.getTitle() + "_" + s.getCategory(),
                s -> s,
                (existing, replacement) -> existing.getPriority().getWeight() > replacement.getPriority().getWeight() 
                    ? existing : replacement
            ));
        
        // 按优先级和影响程度排序
        return uniqueSuggestions.values().stream()
            .sorted((s1, s2) -> {
                int priorityCompare = Integer.compare(
                    s2.getPriority().getWeight(),
                    s1.getPriority().getWeight()
                );
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                
                return Integer.compare(
                    s2.getEstimatedImpact() != null ? s2.getEstimatedImpact() : 0,
                    s1.getEstimatedImpact() != null ? s1.getEstimatedImpact() : 0
                );
            })
            .limit(20) // 限制建议数量
            .collect(Collectors.toList());
    }
    
    // 辅助方法
    private int getLineNumber(String code, int position) {
        String beforePosition = code.substring(0, position);
        return (int) beforePosition.chars().filter(ch -> ch == '\n').count() + 1;
    }
    
    private int calculateMaxNesting(String code) {
        int maxNesting = 0;
        int currentNesting = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentNesting++;
                maxNesting = Math.max(maxNesting, currentNesting);
            } else if (c == '}') {
                currentNesting--;
            }
        }
        
        return maxNesting;
    }
    
    private List<String> findDuplicateLines(String[] lines) {
        Map<String, Integer> lineCount = new HashMap<>();
        List<String> duplicates = new ArrayList<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                lineCount.put(trimmed, lineCount.getOrDefault(trimmed, 0) + 1);
            }
        }
        
        lineCount.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .forEach(entry -> duplicates.add(entry.getKey()));
        
        return duplicates;
    }
    
    private Priority mapSeverityToPriority(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return Priority.HIGH;
            case HIGH:
                return Priority.HIGH;
            case MEDIUM:
                return Priority.MEDIUM;
            case LOW:
            case INFO:
            default:
                return Priority.LOW;
        }
    }
    
    private String mapIssueCategoryToSuggestionCategory(String category) {
        switch (category.toUpperCase()) {
            case "SECURITY":
                return "SECURITY";
            case "PERFORMANCE":
                return "PERFORMANCE";
            case "STYLE":
                return "STYLE";
            case "MAINTAINABILITY":
                return "MAINTAINABILITY";
            default:
                return "GENERAL";
        }
    }
    
    private String generateSuggestionTitle(CodeIssue issue) {
        return "修复问题：" + issue.getTitle();
    }
    
    private String generateSuggestionDescription(CodeIssue issue) {
        return issue.getDescription() + " 建议立即修复以提高代码质量。";
    }
    
    private String generateCodeExample(CodeIssue issue) {
        // 基于问题类型生成示例代码
        return BEST_PRACTICE_TEMPLATES.getOrDefault(issue.getType(), "// 请参考相关最佳实践进行修复");
    }
    
    private Integer calculateEstimatedImpact(CodeIssue issue) {
        switch (issue.getSeverity()) {
            case CRITICAL:
                return 80;
            case HIGH:
                return 60;
            case MEDIUM:
                return 40;
            case LOW:
                return 20;
            default:
                return 10;
        }
    }
    
    private String mapSeverityToDifficulty(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL:
            case HIGH:
                return "HARD";
            case MEDIUM:
                return "MEDIUM";
            case LOW:
            case INFO:
            default:
                return "EASY";
        }
    }
    
    private List<String> generateTags(CodeIssue issue) {
        List<String> tags = new ArrayList<>();
        tags.add(issue.getCategory().toLowerCase());
        tags.add(issue.getType().name().toLowerCase().replace("_", "-"));
        tags.add("fix");
        return tags;
    }
    
    private static void initializeSuggestionRules() {
        // 字符串拼接优化规则
        SUGGESTION_RULES.put("STRING_CONCATENATION", SuggestionRule.builder()
            .pattern("String\\s+\\w+\\s*=.*\\+.*")
            .title("优化字符串拼接")
            .description("使用StringBuilder替代字符串拼接可以提高性能")
            .category("PERFORMANCE")
            .priority(Priority.MEDIUM)
            .estimatedImpact(25)
            .difficultyLevel("EASY")
            .exampleCode("StringBuilder sb = new StringBuilder().append(str1).append(str2);")
            .tags("performance,strings")
            .build());
            
        // 空指针检查规则
        SUGGESTION_RULES.put("NULL_CHECK", SuggestionRule.builder()
            .pattern("if\\s*\\(\\s*\\w+\\s*!=\\s*null\\s*\\)")
            .title("使用Optional避免空指针")
            .description("考虑使用Optional类来更好地处理可能为空的值")
            .category("MAINTAINABILITY")
            .priority(Priority.LOW)
            .estimatedImpact(15)
            .difficultyLevel("MEDIUM")
            .exampleCode("Optional<String> optionalValue = Optional.ofNullable(value);")
            .tags("null-safety,optional")
            .build());
            
        // 异常处理规则
        SUGGESTION_RULES.put("EMPTY_CATCH", SuggestionRule.builder()
            .pattern("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}")
            .title("完善异常处理")
            .description("空的catch块会隐藏错误，至少应该记录日志")
            .category("MAINTAINABILITY")
            .priority(Priority.HIGH)
            .estimatedImpact(40)
            .difficultyLevel("EASY")
            .exampleCode("catch (Exception e) {\n    log.error(\"Error occurred\", e);\n}")
            .tags("exception-handling,logging")
            .build());
    }
    
    private static void initializeBestPracticeTemplates() {
        BEST_PRACTICE_TEMPLATES.put("NAMING_VIOLATION", 
            "// 使用驼峰命名法\nprivate String userName; // 而不是 user_name");
            
        BEST_PRACTICE_TEMPLATES.put("MAGIC_NUMBER", 
            "// 定义有意义的常量\nprivate static final int MAX_ATTEMPTS = 3;");
            
        BEST_PRACTICE_TEMPLATES.put("LONG_METHOD", 
            "// 将长方法拆分为多个小方法\npublic void processOrder() {\n    validateOrder();\n    calculatePrice();\n    saveOrder();\n}");
            
        BEST_PRACTICE_TEMPLATES.put("RESOURCE_LEAK", 
            "// 使用try-with-resources\ntry (FileInputStream fis = new FileInputStream(file)) {\n    // 处理文件\n}");
    }
    
    // 内部类定义
    private static class SuggestionRule {
        private String pattern;
        private String title;
        private String description;
        private String category;
        private Priority priority;
        private Integer estimatedImpact;
        private String difficultyLevel;
        private String exampleCode;
        private String tags;
        
        public static SuggestionRuleBuilder builder() {
            return new SuggestionRuleBuilder();
        }
        
        // Getters
        public String getPattern() { return pattern; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public Priority getPriority() { return priority; }
        public Integer getEstimatedImpact() { return estimatedImpact; }
        public String getDifficultyLevel() { return difficultyLevel; }
        public String getExampleCode() { return exampleCode; }
        public String getTags() { return tags; }
        
        public static class SuggestionRuleBuilder {
            private SuggestionRule rule = new SuggestionRule();
            
            public SuggestionRuleBuilder pattern(String pattern) {
                rule.pattern = pattern;
                return this;
            }
            
            public SuggestionRuleBuilder title(String title) {
                rule.title = title;
                return this;
            }
            
            public SuggestionRuleBuilder description(String description) {
                rule.description = description;
                return this;
            }
            
            public SuggestionRuleBuilder category(String category) {
                rule.category = category;
                return this;
            }
            
            public SuggestionRuleBuilder priority(Priority priority) {
                rule.priority = priority;
                return this;
            }
            
            public SuggestionRuleBuilder estimatedImpact(Integer estimatedImpact) {
                rule.estimatedImpact = estimatedImpact;
                return this;
            }
            
            public SuggestionRuleBuilder difficultyLevel(String difficultyLevel) {
                rule.difficultyLevel = difficultyLevel;
                return this;
            }
            
            public SuggestionRuleBuilder exampleCode(String exampleCode) {
                rule.exampleCode = exampleCode;
                return this;
            }
            
            public SuggestionRuleBuilder tags(String tags) {
                rule.tags = tags;
                return this;
            }
            
            public SuggestionRule build() {
                return rule;
            }
        }
    }
}