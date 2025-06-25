package com.iexceed.appzillon.sd.WorkFlow.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstance;
import com.iexceed.appzillon.sd.WorkFlow.service.WorkFlowStatusService;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/workflow")
public class WorkFlowStatusController {

    @Autowired
    private WorkFlowStatusService workflowInstanceService;

    @PostMapping("/getPendingTransactions")
     public ResponseEntity<List<WorkflowInstance>> getPendingWorkflows() {
        try {
            List<WorkflowInstance> pendingWorkflows = workflowInstanceService.getPendingWorkflows();
            return ResponseEntity.ok(pendingWorkflows);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(null);
        }
    }
    
    @PostMapping("/getLogsByTransactionId")
    public  ResponseEntity<List<Map<String, Object>>>  getLogsByTransactionId(@RequestBody Map<String, String> request) {
        try {
            String transactionId = request.get("transactionId");
            List<Map<String, Object>> stages = workflowInstanceService.getWorkflowInstanceStagesByTransactionId(transactionId);
            return ResponseEntity.ok(stages);
        } catch (Exception e) {
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }
    
}
