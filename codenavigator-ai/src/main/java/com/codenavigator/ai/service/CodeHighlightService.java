package com.codenavigator.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeHighlightService {
    
    // 不同语言的语法高亮规则
    private static final Map<String, HighlightRules> LANGUAGE_RULES = new HashMap<>();
    
    static {
        initializeLanguageRules();
    }
    
    public HighlightResult highlightCode(String code, String language, HighlightOptions options) {
        log.debug("高亮代码，语言: {}, 代码长度: {}", language, code.length());
        
        if (code == null || code.trim().isEmpty()) {
            return HighlightResult.builder()
                .originalCode(code)
                .highlightedCode(code)
                .language(language)
                .success(false)
                .errorMessage("代码内容为空")
                .build();
        }
        
        try {
            String highlightedCode;
            switch (options.getOutputFormat()) {
                case HTML:
                    highlightedCode = highlightToHtml(code, language, options);
                    break;
                case MARKDOWN:
                    highlightedCode = highlightToMarkdown(code, language, options);
                    break;
                case ANSI:
                    highlightedCode = highlightToAnsi(code, language, options);
                    break;
                case PLAIN:
                default:
                    highlightedCode = formatPlainText(code, options);
                    break;
            }
            
            return HighlightResult.builder()
                .originalCode(code)
                .highlightedCode(highlightedCode)
                .language(language)
                .outputFormat(options.getOutputFormat())
                .lineNumbers(options.isShowLineNumbers())
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("代码高亮失败", e);
            return HighlightResult.builder()
                .originalCode(code)
                .highlightedCode(code)
                .language(language)
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    public String formatCodeSnippet(String code, String language, int startLine, int endLine) {
        if (code == null || startLine < 1 || endLine < startLine) {
            return code;
        }
        
        String[] lines = code.split("\n");
        if (endLine > lines.length) {
            endLine = lines.length;
        }
        
        StringBuilder snippet = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            if (i < lines.length) {
                snippet.append(String.format("%3d: %s\n", i + 1, lines[i]));
            }
        }
        
        return snippet.toString();
    }
    
    public List<CodeAnnotation> generateAnnotations(String code, String language) {
        List<CodeAnnotation> annotations = new ArrayList<>();
        HighlightRules rules = LANGUAGE_RULES.get(language.toLowerCase());
        
        if (rules == null) {
            return annotations;
        }
        
        String[] lines = code.split("\n");
        
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            int lineNumber = lineIndex + 1;
            
            // 检查注释
            if (rules.getCommentPattern() != null) {
                Matcher commentMatcher = rules.getCommentPattern().matcher(line);
                if (commentMatcher.find()) {
                    annotations.add(CodeAnnotation.builder()
                        .lineNumber(lineNumber)
                        .startColumn(commentMatcher.start())
                        .endColumn(commentMatcher.end())
                        .type(AnnotationType.COMMENT)
                        .content(commentMatcher.group())
                        .build());
                }
            }
            
            // 检查关键字
            if (rules.getKeywordPattern() != null) {
                Matcher keywordMatcher = rules.getKeywordPattern().matcher(line);
                while (keywordMatcher.find()) {
                    annotations.add(CodeAnnotation.builder()
                        .lineNumber(lineNumber)
                        .startColumn(keywordMatcher.start())
                        .endColumn(keywordMatcher.end())
                        .type(AnnotationType.KEYWORD)
                        .content(keywordMatcher.group())
                        .build());
                }
            }
            
            // 检查字符串
            if (rules.getStringPattern() != null) {
                Matcher stringMatcher = rules.getStringPattern().matcher(line);
                while (stringMatcher.find()) {
                    annotations.add(CodeAnnotation.builder()
                        .lineNumber(lineNumber)
                        .startColumn(stringMatcher.start())
                        .endColumn(stringMatcher.end())
                        .type(AnnotationType.STRING_LITERAL)
                        .content(stringMatcher.group())
                        .build());
                }
            }
        }
        
        return annotations;
    }
    
    private String highlightToHtml(String code, String language, HighlightOptions options) {
        StringBuilder html = new StringBuilder();
        HighlightRules rules = LANGUAGE_RULES.get(language.toLowerCase());
        
        if (options.isIncludeStylesheet()) {
            html.append(generateCssStyles());
        }
        
        html.append("<div class=\"code-highlight\">");
        if (options.getTitle() != null) {
            html.append("<div class=\"code-title\">").append(escapeHtml(options.getTitle())).append("</div>");
        }
        
        html.append("<pre><code class=\"language-").append(language).append("\">");
        
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String highlightedLine = highlightLine(line, rules, OutputFormat.HTML);
            
            if (options.isShowLineNumbers()) {
                html.append("<span class=\"line-number\">")
                    .append(String.format("%3d", i + 1))
                    .append("</span>");
            }
            
            html.append("<span class=\"line-content\">")
                .append(highlightedLine)
                .append("</span>");
            
            if (i < lines.length - 1) {
                html.append("\n");
            }
        }
        
        html.append("</code></pre></div>");
        
        return html.toString();
    }
    
    private String highlightToMarkdown(String code, String language, HighlightOptions options) {
        StringBuilder markdown = new StringBuilder();
        
        if (options.getTitle() != null) {
            markdown.append("**").append(options.getTitle()).append("**\n\n");
        }
        
        markdown.append("```").append(language).append("\n");
        
        if (options.isShowLineNumbers()) {
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                markdown.append(String.format("%3d: %s\n", i + 1, lines[i]));
            }
        } else {
            markdown.append(code);
        }
        
        markdown.append("\n```");
        
        return markdown.toString();
    }
    
    private String highlightToAnsi(String code, String language, HighlightOptions options) {
        StringBuilder ansi = new StringBuilder();
        HighlightRules rules = LANGUAGE_RULES.get(language.toLowerCase());
        
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String highlightedLine = highlightLine(line, rules, OutputFormat.ANSI);
            
            if (options.isShowLineNumbers()) {
                ansi.append(AnsiColors.DARK_GRAY)
                    .append(String.format("%3d: ", i + 1))
                    .append(AnsiColors.RESET);
            }
            
            ansi.append(highlightedLine);
            
            if (i < lines.length - 1) {
                ansi.append("\n");
            }
        }
        
        return ansi.toString();
    }
    
    private String formatPlainText(String code, HighlightOptions options) {
        if (!options.isShowLineNumbers()) {
            return code;
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = code.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            formatted.append(String.format("%3d: %s", i + 1, lines[i]));
            if (i < lines.length - 1) {
                formatted.append("\n");
            }
        }
        
        return formatted.toString();
    }
    
    private String highlightLine(String line, HighlightRules rules, OutputFormat format) {
        if (rules == null) {
            return escapeForFormat(line, format);
        }
        
        String highlighted = line;
        
        // 高亮关键字
        if (rules.getKeywordPattern() != null) {
            highlighted = highlightPattern(highlighted, rules.getKeywordPattern(), 
                getColorForType(AnnotationType.KEYWORD, format), format);
        }
        
        // 高亮字符串
        if (rules.getStringPattern() != null) {
            highlighted = highlightPattern(highlighted, rules.getStringPattern(), 
                getColorForType(AnnotationType.STRING_LITERAL, format), format);
        }
        
        // 高亮注释
        if (rules.getCommentPattern() != null) {
            highlighted = highlightPattern(highlighted, rules.getCommentPattern(), 
                getColorForType(AnnotationType.COMMENT, format), format);
        }
        
        // 高亮数字
        if (rules.getNumberPattern() != null) {
            highlighted = highlightPattern(highlighted, rules.getNumberPattern(), 
                getColorForType(AnnotationType.NUMBER, format), format);
        }
        
        return highlighted;
    }
    
    private String highlightPattern(String text, Pattern pattern, String color, OutputFormat format) {
        if (format == OutputFormat.PLAIN) {
            return text;
        }
        
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String matched = matcher.group();
            String replacement;
            
            switch (format) {
                case HTML:
                    replacement = String.format("<span style=\"%s\">%s</span>", color, escapeHtml(matched));
                    break;
                case ANSI:
                    replacement = color + matched + AnsiColors.RESET;
                    break;
                default:
                    replacement = matched;
                    break;
            }
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private String getColorForType(AnnotationType type, OutputFormat format) {
        switch (format) {
            case HTML:
                switch (type) {
                    case KEYWORD: return "color: #0066CC; font-weight: bold;";
                    case STRING_LITERAL: return "color: #008000;";
                    case COMMENT: return "color: #808080; font-style: italic;";
                    case NUMBER: return "color: #FF6600;";
                    default: return "";
                }
            case ANSI:
                switch (type) {
                    case KEYWORD: return AnsiColors.BLUE + AnsiColors.BOLD;
                    case STRING_LITERAL: return AnsiColors.GREEN;
                    case COMMENT: return AnsiColors.DARK_GRAY;
                    case NUMBER: return AnsiColors.YELLOW;
                    default: return "";
                }
            default:
                return "";
        }
    }
    
    private String escapeForFormat(String text, OutputFormat format) {
        switch (format) {
            case HTML:
                return escapeHtml(text);
            default:
                return text;
        }
    }
    
    private String escapeHtml(String text) {
        if (text == null) return null;
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
    
    private String generateCssStyles() {
        return "<style>\n" +
               ".code-highlight { border: 1px solid #ddd; border-radius: 4px; background: #f8f9fa; }\n" +
               ".code-title { background: #e9ecef; padding: 8px 12px; font-weight: bold; border-bottom: 1px solid #ddd; }\n" +
               ".code-highlight pre { margin: 0; padding: 12px; overflow-x: auto; }\n" +
               ".line-number { color: #6c757d; margin-right: 12px; user-select: none; }\n" +
               ".line-content { white-space: pre; }\n" +
               "</style>\n";
    }
    
    private static void initializeLanguageRules() {
        // Java语法规则
        LANGUAGE_RULES.put("java", HighlightRules.builder()
            .keywordPattern(Pattern.compile("\\b(public|private|protected|static|final|abstract|class|interface|extends|implements|import|package|if|else|for|while|do|switch|case|default|break|continue|return|try|catch|finally|throw|throws|new|this|super|null|true|false|void|int|double|float|long|short|byte|char|boolean|String)\\b"))
            .stringPattern(Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"))
            .commentPattern(Pattern.compile("//.*$|/\\*.*?\\*/"))
            .numberPattern(Pattern.compile("\\b\\d+(\\.\\d+)?[fFlL]?\\b"))
            .build());
        
        // JavaScript语法规则
        LANGUAGE_RULES.put("javascript", HighlightRules.builder()
            .keywordPattern(Pattern.compile("\\b(var|let|const|function|if|else|for|while|do|switch|case|default|break|continue|return|try|catch|finally|throw|new|this|null|undefined|true|false|class|extends|import|export|from|async|await)\\b"))
            .stringPattern(Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`"))
            .commentPattern(Pattern.compile("//.*$|/\\*.*?\\*/"))
            .numberPattern(Pattern.compile("\\b\\d+(\\.\\d+)?\\b"))
            .build());
        
        // Python语法规则  
        LANGUAGE_RULES.put("python", HighlightRules.builder()
            .keywordPattern(Pattern.compile("\\b(def|class|if|elif|else|for|while|try|except|finally|import|from|as|return|yield|break|continue|pass|with|lambda|and|or|not|in|is|None|True|False|global|nonlocal)\\b"))
            .stringPattern(Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|\"\"\".*?\"\"\"|'''.*?'''"))
            .commentPattern(Pattern.compile("#.*$"))
            .numberPattern(Pattern.compile("\\b\\d+(\\.\\d+)?\\b"))
            .build());
    }
    
    // 枚举和内部类定义
    public enum OutputFormat {
        HTML, MARKDOWN, ANSI, PLAIN
    }
    
    public enum AnnotationType {
        KEYWORD, STRING_LITERAL, COMMENT, NUMBER, IDENTIFIER, OPERATOR, BRACKET
    }
    
    public static class HighlightOptions {
        private OutputFormat outputFormat = OutputFormat.HTML;
        private boolean showLineNumbers = false;
        private boolean includeStylesheet = true;
        private String title;
        
        public static HighlightOptionsBuilder builder() {
            return new HighlightOptionsBuilder();
        }
        
        // Getters
        public OutputFormat getOutputFormat() { return outputFormat; }
        public boolean isShowLineNumbers() { return showLineNumbers; }
        public boolean isIncludeStylesheet() { return includeStylesheet; }
        public String getTitle() { return title; }
        
        public static class HighlightOptionsBuilder {
            private HighlightOptions options = new HighlightOptions();
            
            public HighlightOptionsBuilder outputFormat(OutputFormat format) { 
                options.outputFormat = format; return this; }
            public HighlightOptionsBuilder showLineNumbers(boolean show) { 
                options.showLineNumbers = show; return this; }
            public HighlightOptionsBuilder includeStylesheet(boolean include) { 
                options.includeStylesheet = include; return this; }
            public HighlightOptionsBuilder title(String title) { 
                options.title = title; return this; }
            
            public HighlightOptions build() { return options; }
        }
    }
    
    public static class HighlightResult {
        private String originalCode;
        private String highlightedCode;
        private String language;
        private OutputFormat outputFormat;
        private boolean lineNumbers;
        private boolean success;
        private String errorMessage;
        
        public static HighlightResultBuilder builder() {
            return new HighlightResultBuilder();
        }
        
        // Getters
        public String getOriginalCode() { return originalCode; }
        public String getHighlightedCode() { return highlightedCode; }
        public String getLanguage() { return language; }
        public OutputFormat getOutputFormat() { return outputFormat; }
        public boolean isLineNumbers() { return lineNumbers; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        public static class HighlightResultBuilder {
            private HighlightResult result = new HighlightResult();
            
            public HighlightResultBuilder originalCode(String code) { result.originalCode = code; return this; }
            public HighlightResultBuilder highlightedCode(String code) { result.highlightedCode = code; return this; }
            public HighlightResultBuilder language(String language) { result.language = language; return this; }
            public HighlightResultBuilder outputFormat(OutputFormat format) { result.outputFormat = format; return this; }
            public HighlightResultBuilder lineNumbers(boolean lineNumbers) { result.lineNumbers = lineNumbers; return this; }
            public HighlightResultBuilder success(boolean success) { result.success = success; return this; }
            public HighlightResultBuilder errorMessage(String error) { result.errorMessage = error; return this; }
            
            public HighlightResult build() { return result; }
        }
    }
    
    public static class CodeAnnotation {
        private int lineNumber;
        private int startColumn;
        private int endColumn;
        private AnnotationType type;
        private String content;
        
        public static CodeAnnotationBuilder builder() {
            return new CodeAnnotationBuilder();
        }
        
        // Getters
        public int getLineNumber() { return lineNumber; }
        public int getStartColumn() { return startColumn; }
        public int getEndColumn() { return endColumn; }
        public AnnotationType getType() { return type; }
        public String getContent() { return content; }
        
        public static class CodeAnnotationBuilder {
            private CodeAnnotation annotation = new CodeAnnotation();
            
            public CodeAnnotationBuilder lineNumber(int lineNumber) { annotation.lineNumber = lineNumber; return this; }
            public CodeAnnotationBuilder startColumn(int startColumn) { annotation.startColumn = startColumn; return this; }
            public CodeAnnotationBuilder endColumn(int endColumn) { annotation.endColumn = endColumn; return this; }
            public CodeAnnotationBuilder type(AnnotationType type) { annotation.type = type; return this; }
            public CodeAnnotationBuilder content(String content) { annotation.content = content; return this; }
            
            public CodeAnnotation build() { return annotation; }
        }
    }
    
    private static class HighlightRules {
        private Pattern keywordPattern;
        private Pattern stringPattern;
        private Pattern commentPattern;
        private Pattern numberPattern;
        
        public static HighlightRulesBuilder builder() {
            return new HighlightRulesBuilder();
        }
        
        // Getters
        public Pattern getKeywordPattern() { return keywordPattern; }
        public Pattern getStringPattern() { return stringPattern; }
        public Pattern getCommentPattern() { return commentPattern; }
        public Pattern getNumberPattern() { return numberPattern; }
        
        public static class HighlightRulesBuilder {
            private HighlightRules rules = new HighlightRules();
            
            public HighlightRulesBuilder keywordPattern(Pattern pattern) { rules.keywordPattern = pattern; return this; }
            public HighlightRulesBuilder stringPattern(Pattern pattern) { rules.stringPattern = pattern; return this; }
            public HighlightRulesBuilder commentPattern(Pattern pattern) { rules.commentPattern = pattern; return this; }
            public HighlightRulesBuilder numberPattern(Pattern pattern) { rules.numberPattern = pattern; return this; }
            
            public HighlightRules build() { return rules; }
        }
    }
    
    // ANSI颜色常量
    private static class AnsiColors {
        public static final String RESET = "\u001B[0m";
        public static final String BOLD = "\u001B[1m";
        public static final String BLACK = "\u001B[30m";
        public static final String RED = "\u001B[31m";
        public static final String GREEN = "\u001B[32m";
        public static final String YELLOW = "\u001B[33m";
        public static final String BLUE = "\u001B[34m";
        public static final String PURPLE = "\u001B[35m";
        public static final String CYAN = "\u001B[36m";
        public static final String WHITE = "\u001B[37m";
        public static final String DARK_GRAY = "\u001B[90m";
    }
}