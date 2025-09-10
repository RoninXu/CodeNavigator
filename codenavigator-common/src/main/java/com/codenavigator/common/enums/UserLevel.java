package com.codenavigator.common.enums;

public enum UserLevel {
    BEGINNER("初级", 1),
    INTERMEDIATE("中级", 2),
    ADVANCED("高级", 3),
    EXPERT("专家", 4);
    
    private final String displayName;
    private final int level;
    
    UserLevel(String displayName, int level) {
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