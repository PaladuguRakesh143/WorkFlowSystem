package com.iexceed.appzillon.sd.WorkFlow.entity;


import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "TB_SDWF_WF_STG_HISTORY")
@Data
public class WorkflowInstanceStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private String transactionId; // Renamed from uniqueId to transactionId
    
    @ManyToOne
    @JoinColumn(name = "WORKFLOW_INSTANCE_ID", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(name = "CURRENT_STG_ID", nullable = false)
    private int currentStageNumber;
   

    @Column(name = "STATUS", nullable = false)
    private String status; // e.g., "Completed", "Pending"

    @CreationTimestamp
    @Column(name="CREATE_TS",updatable = false,columnDefinition = "DATETIME") 
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATE_TS",columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    // Getters and Setters
}
