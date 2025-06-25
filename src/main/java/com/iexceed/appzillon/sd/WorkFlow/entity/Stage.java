package com.iexceed.appzillon.sd.WorkFlow.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "TB_SDWF_STG_MASTER",
       uniqueConstraints = @UniqueConstraint(columnNames = {"WORKFLOW_ID", "STAGE_ID"})) // Composite unique constraint
@Data
public class Stage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private int id;
    @Column(name = "STAGE_ID", nullable = false)
    private int stageNumber;
    @Column(name = "STAGE_NAME", nullable = false)
    private String stageName;
    @Column(name = "METHOD_NAME", nullable = true)
    private String methodName;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID", nullable = false)
    @JsonBackReference
    private Workflow workflow;
    
    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Field> fields;

    @Column(name = "DECISION_JSON", columnDefinition = "json", nullable = true)
    private String decisionJson;

    @CreationTimestamp
    @Column(updatable = false,name = "CREATE_TS",columnDefinition = "DATETIME") // Prevent updates to this column
    private LocalDateTime createdAt;    

    @UpdateTimestamp
    @Column(name = "UPDATE_TS",columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "Stage [id=" + id +
               ", stageName=" + stageName +
               ", methodName=" + methodName +
               ", decisionJson=" + decisionJson + "]";
    }

    
}
