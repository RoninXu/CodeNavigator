package com.codenavigator.core.entity;

import com.codenavigator.common.enums.ModuleType;
import com.codenavigator.common.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "learning_modules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningModule {
    
    @Id
    private String id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "text")
    private String description;
    
    @Column(columnDefinition = "text")
    private String requirements;
    
    @Column(columnDefinition = "text")
    private String hints;
    
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
    
    @Column(name = "estimated_hours")
    private Integer estimatedHours;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleType moduleType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;
    
    @Column(name = "success_criteria", columnDefinition = "json")
    private String successCriteria;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;
    
    @ElementCollection
    @CollectionTable(name = "module_prerequisites", joinColumns = @JoinColumn(name = "module_id"))
    @Column(name = "prerequisite_id")
    private List<String> prerequisites;
    
    @Column(name = "content_url")
    private String contentUrl;
    
    @Column(name = "resources", columnDefinition = "json")
    private String resources;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}