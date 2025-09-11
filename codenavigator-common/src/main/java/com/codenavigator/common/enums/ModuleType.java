package com.codenavigator.common.enums;

public enum ModuleType {
    CODE_IMPLEMENTATION("代码实现"),
    THEORY_STUDY("理论学习"),
    CODE_REVIEW("代码审查"),
    PRACTICE_EXERCISE("实践练习"),
    PROJECT_TASK("项目任务"),
    
    // 新增的枚举值
    THEORY("理论学习"),
    PRACTICE("实践练习"),
    PROJECT("项目实战"),
    TUTORIAL("教程学习"),
    QUIZ("测验练习");
    
    private final String displayName;
    
    ModuleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}