package com.iexceed.appzillon.sd.WorkFlow.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iexceed.appzillon.sd.WorkFlow.entity.Stage;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstance;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstanceStage;
import com.iexceed.appzillon.sd.WorkFlow.repository.WorkflowInstanceRepository;
import com.iexceed.appzillon.sd.WorkFlow.repository.WorkflowInstanceStageRepository;

@Service
public class WorkFlowStatusService {

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private WorkflowInstanceStageRepository workflowInstanceStageRepository;

    public List<WorkflowInstance> getPendingWorkflows() {
        // Fetch all instances where workFlowStatus is "PND"
        return workflowInstanceRepository.findByWorkFlowStatus("PND");
    }

    public List<Map<String, Object>> getWorkflowInstanceStagesByTransactionId(String transactionId) {
        List<WorkflowInstanceStage> stages = workflowInstanceStageRepository.findByTransactionId(transactionId);
        
        List<Map<String, Object>> responseList = new ArrayList<>();
        
        for (WorkflowInstanceStage stage : stages) {
            WorkflowInstance workflowInstance = stage.getWorkflowInstance();
            int currentStageNumber = stage.getCurrentStageNumber();
            String stageName = "";
            
            // Find matching stageName from the workflow stages
            for (Stage wfStage : workflowInstance.getWorkflow().getStages()) {
                if (wfStage.getStageNumber() == currentStageNumber) {
                    stageName = wfStage.getStageName();
                    break;
                }
            }
            
            // Create the response map
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("id", stage.getId());
            responseMap.put("transactionId", stage.getTransactionId());
            responseMap.put("stageName", stageName); // Add the found stageName
            responseMap.put("currentStageNumber", currentStageNumber);
            responseMap.put("status", stage.getStatus());
            responseMap.put("createdAt", stage.getCreatedAt());
            responseMap.put("updatedAt", stage.getUpdatedAt());

            // Include workflow details
            if (workflowInstance != null) {
                Map<String, Object> workflowDetails = new HashMap<>();
                workflowDetails.put("workflowId", workflowInstance.getWorkflow().getId());
                workflowDetails.put("workflowName", workflowInstance.getWorkflow().getName());
                // Add any other workflow fields you need
                
                responseMap.put("workflow", workflowDetails);
            }
            
            responseList.add(responseMap);
        }
        
        return responseList;
    }
}
