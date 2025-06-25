package com.iexceed.appzillon.sd.WorkFlow.controller;

import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iexceed.appzillon.sd.WorkFlow.service.WorkflowService;

@RestController
@RequestMapping("/admin")
public class AdminController {
     private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/saveWorkflowConfig")
public ResponseEntity<Map<String, Object>> saveWorkflowConfig(@RequestBody Map<String, Map<String, Object>> config) {
    logger.info("Received request to save workflow configuration: {}", config);

    Map<String, Object> response = null;
    try {
        response = workflowService.saveWorkflowConfiguration(config);
        logger.info("Workflow configuration saved successfully with response: {}", response);
    } catch (Exception e) {
        logger.error("Error occurred while saving workflow configuration", e);
        return ResponseEntity.status(500).body(Map.of("error", "Failed to save workflow configuration"));
    }

    return ResponseEntity.ok(response);
}

}