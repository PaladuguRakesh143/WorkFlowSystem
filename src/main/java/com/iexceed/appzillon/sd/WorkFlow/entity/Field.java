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
@Table(name = "TB_SDWF_STG_FLD_MASTER")
@Data
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private int id;
    @Column(name = "FIELD_NAME", nullable = false)
    private String fieldName;

    @ManyToOne
    @JoinColumn(name = "STAGE_ID", nullable = false)
    private Stage stage;

    @CreationTimestamp
    @Column(name="CREATE_TS",updatable = false,columnDefinition = "DATETIME") // Prevent updates to this column
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="UPDATE_TS",columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
}