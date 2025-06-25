package com.iexceed.appzillon.sd.WorkFlow.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import lombok.Data;

@Entity
@Table(name = "TB_SDWF_WF_CUR_INSTANCE")
@Data
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;
    @Column(name = "TRANSACTION_ID", nullable = false)
    private String transactionId; // Renamed from uniqueId to transactionId
    @Column(name = "CURRENT_STG_ID", nullable = false)
    private int currentStage;
    @Column(name = "CURRENT_STG_NAME", nullable = false)
    private String CurrentStageName;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WorkflowInstanceStage> stages = new ArrayList<>();

    @Column(name = "APPROVAL_STATUS", nullable = true)
    private String approvalStatus;

    @Column(name = "WORKFLOW_STATUS", nullable = true)
    private String workFlowStatus;

    @CreationTimestamp
    @Column(name="CREATE_TS",updatable = false,columnDefinition = "DATETIME") 
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATE_TS",columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    // Getters and Setters
}
