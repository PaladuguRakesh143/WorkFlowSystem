package com.iexceed.appzillon.sd.WorkFlow.controller;

import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iexceed.appzillon.sd.WorkFlow.service.WorkflowService;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

     private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(@RequestBody Map<String, Object> inputs) {
        logger.info("Received request to execute workflow with inputs: {}", inputs);

        Map<String, Object> response = null;
        try {
            // Execute workflow and log the success
            response = workflowService.executeWorkflow(inputs);
            logger.info("Workflow executed successfully with response: {}", response);
        } catch (Exception e) {
            // Log the exception in case of error
            logger.error("Error occurred while executing workflow", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String,Object>> workflowInfo(@RequestBody Map<String,Object> inputs){
        Map<String,Object> response = workflowService.workflowInfo(inputs);
        return ResponseEntity.ok(response);
    }
}
