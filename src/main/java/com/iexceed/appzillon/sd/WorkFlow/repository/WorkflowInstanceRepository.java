package com.iexceed.appzillon.sd.WorkFlow.repository;

import org.apache.el.stream.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.iexceed.appzillon.sd.WorkFlow.entity.Workflow;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstance;
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Integer>{
    WorkflowInstance findByTransactionId(String transactionId); 

    WorkflowInstance findByTransactionIdAndWorkflow(String transactionId,Workflow workflow);
    
    List<WorkflowInstance> findByWorkFlowStatus(String workFlowStatus);
}
