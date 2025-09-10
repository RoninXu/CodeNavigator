package com.codenavigator.common.enums;

public enum ProgressStatus {
    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    PAUSED("已暂停"),
    ABANDONED("已放弃");
    
    private final String displayName;
    
    ProgressStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}