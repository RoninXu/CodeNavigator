package com.codenavigator.common.enums;

public enum ModuleType {
    CODE_IMPLEMENTATION("代码实现"),
    THEORY_STUDY("理论学习"),
    CODE_REVIEW("代码审查"),
    PRACTICE_EXERCISE("实践练习"),
    PROJECT_TASK("项目任务");
    
    private final String displayName;
    
    ModuleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}