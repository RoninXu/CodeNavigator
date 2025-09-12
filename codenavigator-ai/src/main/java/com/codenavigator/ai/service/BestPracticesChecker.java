package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BestPracticesChecker {
    
    // 最佳实践规则
    private static final Map<String, BestPracticeRule> BEST_PRACTICE_RULES;
    static {
        Map<String, BestPracticeRule> rules = new HashMap<>();
        
        // 异常处理最佳实践
        rules.put("exception-handling", new BestPracticeRule(
            "异常处理",
            "应该正确处理异常，避免空catch块",
            IssueSeverity.HIGH,
            "catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}"
        ));
        
        // 资源管理
        rules.put("resource-management", new BestPracticeRule(
            "资源管理",
            "使用try-with-resources管理可关闭的资源",
            IssueSeverity.MEDIUM,
            "new\\s+(FileInputStream|FileOutputStream|BufferedReader|BufferedWriter)"
        ));
        
        // 字符串拼接
        rules.put("string-concatenation", new BestPracticeRule(
            "字符串拼接",
            "在循环中避免使用+进行字符串拼接，建议使用StringBuilder",
            IssueSeverity.MEDIUM,
            "for\\s*\\([^)]*\\)\\s*\\{[^}]*\\+\\s*="
        ));
        
        // 集合初始化
        rules.put("collection-size", new BestPracticeRule(
            "集合初始化",
            "初始化集合时应指定适当的初始容量",
            IssueSeverity.LOW,
            "new\\s+(ArrayList|HashMap|HashSet)\\s*\\(\\s*\\)"
        ));
        
        BEST_PRACTICE_RULES = Collections.unmodifiableMap(rules);
    }
    
    public BestPracticeResult checkBestPractices(String code, String language) {
        log.debug("Checking best practices for language: {}", language);
        
        List<CodeIssue> issues = new ArrayList<>();
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        // 检查各类最佳实践
        checkExceptionHandling(code, issues, suggestions);
        checkResourceManagement(code, issues, suggestions);
        checkPerformancePractices(code, issues, suggestions);
        checkSecurityPractices(code, issues, suggestions);
        checkDesignPrinciples(code, issues, suggestions);
        checkTestingPractices(code, issues, suggestions);
        
        // 计算最佳实践指标
        QualityMetrics metrics = calculateBestPracticeMetrics(code, issues);
        
        return new BestPracticeResult(issues, suggestions, metrics);
    }
    
    private void checkExceptionHandling(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        String[] lines = code.split("\n");
        boolean inCatchBlock = false;
        int catchStartLine = 0;
        List<String> catchContent = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // 检测catch块开始
            if (trimmed.matches("catch\\s*\\([^)]+\\)\\s*\\{?")) {
                inCatchBlock = true;
                catchStartLine = i + 1;
                catchContent.clear();
                continue;
            }
            
            // 收集catch块内容
            if (inCatchBlock) {
                if (trimmed.equals("}")) {
                    inCatchBlock = false;
                    
                    // 检查空catch块
                    if (catchContent.stream().allMatch(String::isEmpty)) {
                        issues.add(createBestPracticeIssue(
                            "empty-catch",
                            "空的catch块会隐藏异常，应该至少记录异常信息",
                            catchStartLine,
                            IssueSeverity.HIGH,
                            Arrays.asList(
                                "记录异常日志: logger.error(\"Error occurred\", e)",
                                "重新抛出异常: throw new CustomException(e)",
                                "提供默认处理逻辑"
                            )
                        ));
                        
                        suggestions.add(createBestPracticeSuggestion(
                            "改进异常处理",
                            "空catch块是不良实践，建议添加适当的异常处理逻辑",
                            "良好的异常处理有助于调试和监控应用程序健康状况",
                            Arrays.asList("提高系统可维护性", "便于问题诊断", "增强系统稳定性"),
                            8
                        ));
                    }
                    
                    // 检查是否只是打印异常
                    else if (catchContent.stream().anyMatch(content -> 
                        content.contains("printStackTrace()") && catchContent.size() == 1)) {
                        suggestions.add(createBestPracticeSuggestion(
                            "使用日志替代printStackTrace",
                            "使用日志框架记录异常而不是printStackTrace",
                            "日志框架提供更好的异常信息管理和控制",
                            Arrays.asList("更好的日志管理", "支持不同日志级别", "便于生产环境监控"),
                            6
                        ));
                    }
                } else {
                    catchContent.add(trimmed);
                }
            }
            
            // 检查异常类型
            if (trimmed.contains("throw new Exception") || trimmed.contains("throw new RuntimeException")) {
                suggestions.add(createBestPracticeSuggestion(
                    "使用具体的异常类型",
                    "避免抛出通用的Exception或RuntimeException",
                    "具体的异常类型能够更准确地表达错误性质",
                    Arrays.asList("提高异常处理精确性", "便于异常分类处理", "增强代码表达能力"),
                    5
                ));
            }
        }
    }
    
    private void checkResourceManagement(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查文件操作资源管理
        Pattern fileResourcePattern = Pattern.compile(
            "(?:FileInputStream|FileOutputStream|BufferedReader|BufferedWriter|FileReader|FileWriter)\\s+\\w+\\s*=\\s*new"
        );
        
        java.util.regex.Matcher matcher = fileResourcePattern.matcher(code);
        while (matcher.find()) {
            // 检查是否在try-with-resources中
            String beforeMatch = code.substring(0, matcher.start());
            boolean inTryWithResources = beforeMatch.lastIndexOf("try (") > beforeMatch.lastIndexOf("}");
            
            if (!inTryWithResources) {
                issues.add(createBestPracticeIssue(
                    "resource-management",
                    "文件资源应使用try-with-resources语句自动管理",
                    countLines(beforeMatch) + 1,
                    IssueSeverity.MEDIUM,
                    Arrays.asList(
                        "使用try-with-resources: try (FileInputStream fis = new FileInputStream(file))",
                        "确保在finally块中关闭资源",
                        "考虑使用工具类简化文件操作"
                    )
                ));
                
                suggestions.add(createBestPracticeSuggestion(
                    "使用try-with-resources",
                    "使用try-with-resources自动管理资源生命周期",
                    "自动资源管理可以避免资源泄露和忘记关闭资源的问题",
                    Arrays.asList("防止资源泄露", "简化代码", "自动异常处理"),
                    7
                ));
            }
        }
        
        // 检查数据库连接管理
        if (code.contains("Connection") && code.contains("DriverManager.getConnection")) {
            if (!code.contains("try (Connection")) {
                suggestions.add(createBestPracticeSuggestion(
                    "数据库连接管理",
                    "数据库连接应该使用try-with-resources或连接池管理",
                    "正确的连接管理对于数据库应用的性能和稳定性至关重要",
                    Arrays.asList("避免连接泄露", "提高数据库性能", "增强应用稳定性"),
                    9
                ));
            }
        }
    }
    
    private void checkPerformancePractices(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查字符串拼接
        Pattern stringConcatInLoop = Pattern.compile("for\\s*\\([^)]*\\)[^{]*\\{[^}]*\\+=.*String");
        if (stringConcatInLoop.matcher(code).find()) {
            issues.add(createBestPracticeIssue(
                "string-concatenation-loop",
                "在循环中使用+进行字符串拼接效率低下",
                1, // 简化，实际应计算具体行号
                IssueSeverity.MEDIUM,
                Arrays.asList(
                    "使用StringBuilder: StringBuilder sb = new StringBuilder()",
                    "使用String.join()进行批量拼接",
                    "考虑使用流式API处理"
                )
            ));
            
            suggestions.add(createBestPracticeSuggestion(
                "优化字符串拼接",
                "在循环中使用StringBuilder替代+操作符",
                "StringBuilder在大量字符串拼接时性能远优于+操作符",
                Arrays.asList("显著提升性能", "减少内存分配", "降低GC压力"),
                8
            ));
        }
        
        // 检查集合初始化
        Pattern collectionInit = Pattern.compile("new\\s+(ArrayList|HashMap|HashSet)\\s*\\(\\s*\\)");
        java.util.regex.Matcher matcher = collectionInit.matcher(code);
        while (matcher.find()) {
            suggestions.add(createBestPracticeSuggestion(
                "指定集合初始容量",
                "已知集合大小时建议指定初始容量",
                "适当的初始容量可以减少集合扩容操作，提高性能",
                Arrays.asList("减少内存重新分配", "提高插入性能", "降低GC频率"),
                4
            ));
        }
        
        // 检查原始类型包装
        if (code.contains("new Integer(") || code.contains("new Long(") || code.contains("new Double(")) {
            issues.add(createBestPracticeIssue(
                "primitive-wrapper",
                "避免显式创建原始类型包装对象",
                1,
                IssueSeverity.MEDIUM,
                Arrays.asList(
                    "使用自动装箱: Integer i = 42",
                    "使用valueOf方法: Integer.valueOf(42)",
                    "直接使用原始类型"
                )
            ));
        }
    }
    
    private void checkSecurityPractices(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查SQL注入风险
        if (code.contains("Statement") && code.contains("executeQuery") && code.contains("+")) {
            issues.add(createBestPracticeIssue(
                "sql-injection",
                "字符串拼接构建SQL可能导致SQL注入漏洞",
                1,
                IssueSeverity.HIGH,
                Arrays.asList(
                    "使用PreparedStatement",
                    "使用参数化查询",
                    "对用户输入进行验证和转义"
                )
            ));
            
            suggestions.add(createBestPracticeSuggestion(
                "防止SQL注入",
                "使用PreparedStatement替代Statement进行数据库操作",
                "PreparedStatement可以有效防止SQL注入攻击",
                Arrays.asList("提高安全性", "防止SQL注入", "提高查询性能"),
                10
            ));
        }
        
        // 检查密码硬编码
        Pattern passwordPattern = Pattern.compile("(?i)(password|pwd|pass)\\s*=\\s*[\"'][^\"']+[\"']");
        if (passwordPattern.matcher(code).find()) {
            issues.add(createBestPracticeIssue(
                "hardcoded-password",
                "代码中不应硬编码密码或敏感信息",
                1,
                IssueSeverity.CRITICAL,
                Arrays.asList(
                    "使用配置文件存储敏感信息",
                    "使用环境变量",
                    "使用密钥管理系统"
                )
            ));
        }
        
        // 检查随机数生成
        if (code.contains("new Random()") && (code.contains("password") || code.contains("token"))) {
            suggestions.add(createBestPracticeSuggestion(
                "使用安全随机数",
                "生成密码或令牌时应使用SecureRandom",
                "SecureRandom提供加密安全的随机数生成",
                Arrays.asList("提高安全性", "符合加密标准", "防止预测攻击"),
                7
            ));
        }
    }
    
    private void checkDesignPrinciples(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查单一职责原则
        String[] lines = code.split("\n");
        Map<String, Integer> methodCount = new HashMap<>();
        
        for (String line : lines) {
            if (line.trim().startsWith("public ") && line.contains("(")) {
                String className = extractClassName(code);
                methodCount.merge(className, 1, Integer::sum);
            }
        }
        
        for (Map.Entry<String, Integer> entry : methodCount.entrySet()) {
            if (entry.getValue() > 20) {
                suggestions.add(createBestPracticeSuggestion(
                    "考虑拆分大类",
                    String.format("类 %s 有 %d 个方法，可能违反单一职责原则", entry.getKey(), entry.getValue()),
                    "单一职责原则有助于提高代码的可维护性和可测试性",
                    Arrays.asList("提高代码可维护性", "便于单元测试", "降低类的复杂度"),
                    6
                ));
            }
        }
        
        // 检查魔法数字
        Pattern magicNumberPattern = Pattern.compile("\\b(?!0|1)\\d{2,}\\b");
        java.util.regex.Matcher matcher = magicNumberPattern.matcher(code);
        while (matcher.find()) {
            if (!code.substring(0, matcher.start()).contains("final") && 
                !code.substring(matcher.start()).startsWith("//")) {
                suggestions.add(createBestPracticeSuggestion(
                    "定义命名常量",
                    "将魔法数字 " + matcher.group() + " 定义为命名常量",
                    "命名常量可以提高代码的可读性和可维护性",
                    Arrays.asList("提高代码可读性", "便于维护和修改", "避免重复定义"),
                    5
                ));
            }
        }
    }
    
    private void checkTestingPractices(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查是否有对应的测试
        if (!code.contains("Test") && !code.contains("@Test")) {
            String className = extractClassName(code);
            if (className != null && !className.contains("Test")) {
                suggestions.add(createBestPracticeSuggestion(
                    "添加单元测试",
                    "为类 " + className + " 添加相应的单元测试",
                    "单元测试是保证代码质量的重要手段",
                    Arrays.asList("提高代码质量", "便于重构", "防止回归问题"),
                    7
                ));
            }
        }
        
        // 检查测试方法命名
        if (code.contains("@Test")) {
            Pattern testMethodPattern = Pattern.compile("@Test[^}]*?public\\s+void\\s+(\\w+)");
            java.util.regex.Matcher matcher = testMethodPattern.matcher(code);
            while (matcher.find()) {
                String methodName = matcher.group(1);
                if (!methodName.startsWith("test") && !methodName.contains("should") && !methodName.contains("when")) {
                    suggestions.add(createBestPracticeSuggestion(
                        "改进测试方法命名",
                        "测试方法名 " + methodName + " 应该描述测试场景",
                        "描述性的测试方法名有助于理解测试意图",
                        Arrays.asList("提高测试可读性", "便于理解测试意图", "改善测试文档"),
                        4
                    ));
                }
            }
        }
    }
    
    private QualityMetrics calculateBestPracticeMetrics(String code, List<CodeIssue> issues) {
        long criticalIssues = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();
        long highIssues = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.HIGH).count();
        long mediumIssues = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.MEDIUM).count();
        
        // 最佳实践评分
        int bestPractices = Math.max(40, 100 - (int)(criticalIssues * 20 + highIssues * 10 + mediumIssues * 5));
        
        // 安全性评分
        long securityIssues = issues.stream()
            .filter(i -> i.getRule().contains("sql-injection") || i.getRule().contains("password"))
            .count();
        int security = Math.max(60, 100 - (int)(securityIssues * 25));
        
        // 可维护性评分
        int maintainability = Math.max(50, 100 - (int)(highIssues * 8 + mediumIssues * 4));
        
        return QualityMetrics.builder()
            .codeStyle(null) // 最佳实践检查不影响代码风格评分
            .readability(null)
            .maintainability(maintainability)
            .performance(null) // 需要专门的性能分析
            .security(security)
            .bestPractices(bestPractices)
            .build();
    }
    
    // 辅助方法
    private CodeIssue createBestPracticeIssue(String ruleId, String description, int lineNumber, 
                                            IssueSeverity severity, List<String> fixSuggestions) {
        return CodeIssue.builder()
            .id(UUID.randomUUID().toString())
            .type(IssueType.BEST_PRACTICE_VIOLATION)
            .severity(severity)
            .title("最佳实践违规")
            .description(description)
            .lineNumber(lineNumber)
            .rule(ruleId)
            .fixSuggestions(fixSuggestions)
            .build();
    }
    
    private CodeSuggestion createBestPracticeSuggestion(String title, String description, 
                                                      String explanation, List<String> benefits, 
                                                      int impact) {
        return CodeSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(SuggestionType.BEST_PRACTICE)
            .priority(impact > 7 ? Priority.HIGH : (impact > 4 ? Priority.MEDIUM : Priority.LOW))
            .title(title)
            .description(description)
            .explanation(explanation)
            .benefits(benefits)
            .estimatedImpact(impact)
            .build();
    }
    
    private int countLines(String text) {
        return text.split("\n").length;
    }
    
    private String extractClassName(String code) {
        Pattern classPattern = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
        java.util.regex.Matcher matcher = classPattern.matcher(code);
        return matcher.find() ? matcher.group(1) : "UnknownClass";
    }
    
    // 内部类
    private static class BestPracticeRule {
        final String title;
        final String description;
        final IssueSeverity severity;
        final String pattern;
        
        BestPracticeRule(String title, String description, IssueSeverity severity, String pattern) {
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.pattern = pattern;
        }
    }
    
    public static class BestPracticeResult {
        private final List<CodeIssue> issues;
        private final List<CodeSuggestion> suggestions;
        private final QualityMetrics metrics;
        
        public BestPracticeResult(List<CodeIssue> issues, List<CodeSuggestion> suggestions, QualityMetrics metrics) {
            this.issues = issues;
            this.suggestions = suggestions;
            this.metrics = metrics;
        }
        
        public List<CodeIssue> getIssues() {
            return issues;
        }
        
        public List<CodeSuggestion> getSuggestions() {
            return suggestions;
        }
        
        public QualityMetrics getMetrics() {
            return metrics;
        }
    }
}