package com.iexceed.appzillon.sd.WorkFlow.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstance;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstanceStage;
@Repository
public interface WorkflowInstanceStageRepository extends JpaRepository<WorkflowInstanceStage, Integer> {
    // Custom query method to find by transactionId and currentStageNumber
    List<WorkflowInstanceStage> findByTransactionIdAndCurrentStageNumber(String transactionId, int currentStageNumber);
    WorkflowInstanceStage findTopByTransactionIdAndCurrentStageNumber(String transactionId, int currentStageNumber);
    Optional<WorkflowInstanceStage> findByTransactionIdAndCurrentStageNumberAndWorkflowInstance(String transactionId, int currentStageNumber, WorkflowInstance workflowInstance);
    List<WorkflowInstanceStage> findAllByTransactionId(String transactionId);
    List<WorkflowInstanceStage> findByTransactionId(String transactionId);

}
