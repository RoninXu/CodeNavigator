package com.codenavigator.core.entity;

import com.codenavigator.common.enums.ModuleType;
import jakarta.persistence.*;

@Entity
@Table(name = "learning_modules")
public class LearningModule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "text")
    private String description;
    
    @Column(columnDefinition = "text")
    private String requirements;
    
    @Column(columnDefinition = "text")
    private String hints;
    
    @Column(nullable = false)
    private Integer sequence;
    
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleType type;
    
    @Column(name = "success_criteria", columnDefinition = "json")
    private String successCriteria;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 无参构造函数
    public LearningModule() {
    }
    
    // 构造函数
    public LearningModule(String title, Integer sequence, ModuleType type, LearningPath learningPath) {
        this.title = title;
        this.sequence = sequence;
        this.type = type;
        this.learningPath = learningPath;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRequirements() {
        return requirements;
    }
    
    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
    
    public String getHints() {
        return hints;
    }
    
    public void setHints(String hints) {
        this.hints = hints;
    }
    
    public Integer getSequence() {
        return sequence;
    }
    
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
    
    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }
    
    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
    
    public LearningPath getLearningPath() {
        return learningPath;
    }
    
    public void setLearningPath(LearningPath learningPath) {
        this.learningPath = learningPath;
    }
    
    public ModuleType getType() {
        return type;
    }
    
    public void setType(ModuleType type) {
        this.type = type;
    }
    
    public String getSuccessCriteria() {
        return successCriteria;
    }
    
    public void setSuccessCriteria(String successCriteria) {
        this.successCriteria = successCriteria;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}