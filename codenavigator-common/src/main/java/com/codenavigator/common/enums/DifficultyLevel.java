package com.codenavigator.common.enums;

public enum DifficultyLevel {
    BEGINNER("入门", 1),
    BASIC("基础", 2),
    INTERMEDIATE("中级", 3),
    ADVANCED("高级", 4),
    EXPERT("专家", 5);
    
    private final String displayName;
    private final int level;
    
    DifficultyLevel(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
}