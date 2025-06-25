package com.iexceed.appzillon.sd.WorkFlow.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "TB_SDWF_WF_MASTER")
@Data
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WORKFLOW_ID", nullable = false) 
    private int id;
    @Column(name = "WORKFLOW_NAME", nullable = false)
    private String name; // e.g., "Order Workflow"

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Stage> stages;

    @CreationTimestamp
    @Column(name="CREATE_TS",updatable = false,columnDefinition = "DATETIME") // Prevent updates to this column
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATE_TS",columnDefinition = "DATETIME") 
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "Workflow [id=" + id + ", name=" + name + "]";
    }

    
}