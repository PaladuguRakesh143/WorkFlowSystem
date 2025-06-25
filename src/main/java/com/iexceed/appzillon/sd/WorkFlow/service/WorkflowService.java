package com.iexceed.appzillon.sd.WorkFlow.service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexceed.appzillon.sd.WorkFlow.entity.Field;
import com.iexceed.appzillon.sd.WorkFlow.entity.Stage;
import com.iexceed.appzillon.sd.WorkFlow.entity.Workflow;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstance;
import com.iexceed.appzillon.sd.WorkFlow.entity.WorkflowInstanceStage;
import com.iexceed.appzillon.sd.WorkFlow.repository.FieldRepository;
import com.iexceed.appzillon.sd.WorkFlow.repository.StageRepository;
import com.iexceed.appzillon.sd.WorkFlow.repository.WorkflowInstanceRepository;
import com.iexceed.appzillon.sd.WorkFlow.repository.WorkflowInstanceStageRepository;
import com.iexceed.appzillon.sd.WorkFlow.repository.WorkflowRepository;

import jakarta.transaction.Transactional;

@Service
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private WorkflowInstanceStageRepository workflowInstanceStageRepository;

    @Transactional
    public Map<String, Object> saveWorkflowConfiguration(Map<String, Map<String, Object>> configuration)
            throws JsonProcessingException {
        logger.info("Starting to save workflow configuration");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> workflowDetails = new ArrayList<>();

        // Process the incoming configuration
        for (Map.Entry<String, Map<String, Object>> workflowEntry : configuration.entrySet()) {
            String workflowType = workflowEntry.getKey();
            logger.debug("Processing workflow: {}", workflowType);
            // Retrieve or create the Workflow
            Workflow workflow = workflowRepository.findByName(workflowType);
            if (workflow == null) {
                logger.info("Creating new workflow: {}", workflowType);
                workflow = new Workflow();
                workflow.setName(workflowType);
                workflowRepository.save(workflow);
            }

            // Ensure the workflow's stages list is initialized
            if (workflow.getStages() == null) {
                workflow.setStages(new ArrayList<>());
            }

            // Fetch existing stages for this workflow
            List<Stage> existingStages = stageRepository.findByWorkflowOrderByStageNumberAsc(workflow);
            Map<Integer, Stage> existingStagesMap = existingStages.stream()
                    .collect(Collectors.toMap(Stage::getStageNumber, stage -> stage, (s1, s2) -> s1)); // Avoid
                                                                                                       // duplicates

            // Track the new stages and stage numbers for this workflow
            Set<Integer> newStageNumbers = new HashSet<>();

            for (Map.Entry<String, Object> stageEntry : workflowEntry.getValue().entrySet()) {
                String stageName = stageEntry.getKey();
                Map<String, Object> stageData = (Map<String, Object>) stageEntry.getValue();

                int newStageNumber = (Integer) stageData.get("stage");
                newStageNumbers.add(newStageNumber);

                Stage stage;
                if (existingStagesMap.containsKey(newStageNumber)) {
                    // Update existing stage
                    stage = existingStagesMap.get(newStageNumber);
                    logger.debug("Updating existing stage: {}", newStageNumber);
                    // Remove fields that are not in the request body
                    Set<String> requestBody = new HashSet<>((List<String>) stageData.get("requestBody"));
                    List<Field> fieldsToRemove = new ArrayList<>();
                    for (Field existingField : stage.getFields()) {
                        if (!requestBody.contains(existingField.getFieldName())) {
                            fieldsToRemove.add(existingField);
                        }
                    }
                    stage.getFields().removeAll(fieldsToRemove);
                    fieldRepository.deleteAll(fieldsToRemove); // Delete fields not in the request body
                } else {
                    // Shift existing stages if inserting in the middle
                    if (newStageNumber <= existingStages.size()) {
                        logger.debug("Shifting existing stages to accommodate new stage number: {}", newStageNumber);
                        for (Stage s : existingStages) {
                            if (s.getStageNumber() >= newStageNumber) {
                                s.setStageNumber(s.getStageNumber() + 1);
                                stageRepository.save(s);
                            }
                        }
                    }
                    // Create a new stage
                    stage = new Stage();
                    stage.setStageNumber(newStageNumber);
                    stage.setWorkflow(workflow);
                    workflow.getStages().add(stage);
                }

                // Set the stage name and method name,and DecisionJson
                stage.setStageName(stageName);
                stage.setMethodName((String) stageData.get("methodName"));

                // saving the decision json in the stage entity
                Object decisionJsonObject = stageData.get("decisionJson");
                if (decisionJsonObject != null) {
                    // Convert the decisionJsonObject to a JSON string if it exists
                    ObjectMapper objectMapper = new ObjectMapper();
                    String decisionJson = objectMapper.writeValueAsString(decisionJsonObject);

                    // Set the JSON string in the Stage entity
                    stage.setDecisionJson(decisionJson);
                } else {
                    // Set the decisionJson field to null if decisionJsonObject is absent
                    stage.setDecisionJson(null);
                }

                List<Field> fields = stage.getFields();
                if (fields == null) {
                    fields = new ArrayList<>();
                    stage.setFields(fields);
                }

                // Track existing field names
                Set<String> existingFieldNames = fields.stream()
                        .map(Field::getFieldName)
                        .collect(Collectors.toSet());

                // Add new fields
                @SuppressWarnings("unchecked")
                List<String> requestBody = (List<String>) stageData.get("requestBody");
                for (String fieldName : requestBody) {
                    if (!existingFieldNames.contains(fieldName)) {
                        Field field = new Field();
                        field.setFieldName(fieldName);
                        field.setStage(stage);
                        fields.add(field);
                    }
                }

                // Save the new or updated stage
                stageRepository.save(stage);
            }

            // Delete stages that are no longer in the new configuration
            for (Stage existingStage : existingStages) {
                if (!newStageNumbers.contains(existingStage.getStageNumber())) {
                    // Delete associated fields first
                    fieldRepository.deleteByStage(existingStage);
                    stageRepository.delete(existingStage);
                }
            }
        }

        // Skip removing workflows that no longer exist in the incoming configuration
        // This is the key change as we no longer delete workflows from the database

        // Fetch all workflows after saving the configuration
        List<Workflow> allWorkflows = workflowRepository.findAll();
        logger.info("Fetched {} workflows after saving configuration", allWorkflows.size());

        // Add workflows to the response
        for (Workflow wf : allWorkflows) {
            Map<String, Object> workflowInfo = new HashMap<>();
            workflowInfo.put("name", wf.getName());
            workflowInfo.put("id", wf.getId());
            workflowDetails.add(workflowInfo);
        }

        response.put("workflows", workflowDetails);
        return response;
    }

    // Method to execute the workflow based on id and inputs
    public Map<String, Object> executeWorkflow(Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();
        if (!request.containsKey("workflowId") || request.get("workflowId") == null) {
            return generateNoWorkflowIdResponse();
        }
        if (!request.containsKey("input") || !(request.get("input") instanceof Map)) {
            return generateNoInputResponse();
        }

        int workflowId = (int) request.get("workflowId");
        Map<String, Object> inputs = (Map<String, Object>) request.get("input");
        Optional<Workflow> OptWorkflow = workflowRepository.findById(workflowId);
        if (!OptWorkflow.isPresent()) {

            // Generate Workflow not found response
            response = generateWorkflowNotFoundResponse();
            return response;
        }
        Workflow workflow = OptWorkflow.get();
        String transactionId = (String) inputs.get("transactionId");
        logger.debug("Retrieved transactionId: {}", transactionId);
        // Handle new workflow instance creation if no transactionId is present
        if (transactionId == null) {
            logger.info("No transactionId present, handling new workflow instance creation");
            // Get the first stage for the workflow
            List<Stage> stages = stageRepository.findByWorkflow(workflow); // all stages for the workflow
            Optional<Stage> optionalFirstStage = stages.stream() // sort the stages and get the first stage
                    .sorted(Comparator.comparingInt(Stage::getStageNumber))
                    .findFirst();
            Stage currentStage = optionalFirstStage.get();

            if (currentStage == null) {

                response = generateInitialStageNotFoundResponse();
                return response;
            }

            // Check for the missingFields in the inputs for that particular stage by
            // calling checkFields() method.
            List<String> missingFields = checkFields(inputs, currentStage);

            if (missingFields.isEmpty()) {
                // There are no missingFields so creating a new workflowInstance.
                Stage presentStageData = getDecisionLogicAndMethodName(currentStage);

                response = handleNewWorkflowInstance(workflow, currentStage, inputs);

                return response;
            } else {
                // Generate missing fields response by calling generateMissingFieldsResponse()
                // method.
                return generateMissingFieldsResponse(missingFields, currentStage);
            }
        } else {
            // Transaction ID is present so handle that existing workflowInstance.
            response = handleExistingWorkflowInstance(transactionId, inputs);
            return response;
        }
    }

    private Stage getDecisionLogicAndMethodName(Stage currentStage) {
        String currentStageName = currentStage.getStageName();
        Stage stageOpt = stageRepository.findByStageName(currentStageName);
        return stageOpt;
    }

    private List<String> checkFields(Map<String, Object> inputs, Stage stage) {
        logger.info("Checking fields for stage: {}", stage.getStageNumber());
        List<Field> fields = fieldRepository.findByStage(stage);
        List<String> missingFields = new ArrayList<>();
        for (Field field : fields) {
            if (!inputs.containsKey(field.getFieldName())) {
                missingFields.add(field.getFieldName());
                logger.debug("Missing field: {}", field.getFieldName());
            }
        }
        logger.info("Missing fields for stage {}: {}", stage.getStageNumber(), missingFields);
        return missingFields;
    }

    private Map<String, Object> handleExistingWorkflowInstance(String transactionId, Map<String, Object> inputs) {
        logger.info("Handling existing workflow instance for transaction ID: {}", transactionId);
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> presentStage = new HashMap<>();
        // Find the existing workflow instance by transactionId
        WorkflowInstance workflowInstance = workflowInstanceRepository.findByTransactionId(transactionId);
        if (workflowInstance == null) {
            logger.error("Workflow instance not found for transaction ID: {}", transactionId);
            // Generate invalid transactionId as workflowInstance for that ID is not found
            response = generateInvalidTransactionIdResponse();
            return response;

        } else {
            int noOfStages = workflowInstance.getWorkflow().getStages().size();
            int presentStageNumber = workflowInstance.getCurrentStage();
            int currenStageNumber = presentStageNumber + 1;
            logger.info("Current stage number: {}", presentStageNumber);
            logger.info("Next stage number: {}", currenStageNumber);
            // Checking whether all stages are completed
            if (presentStageNumber < noOfStages) {
                List<WorkflowInstanceStage> instanceStage = workflowInstanceStageRepository
                        .findByTransactionIdAndCurrentStageNumber(transactionId, currenStageNumber);
                WorkflowInstance presentWorkflowInstance = workflowInstanceRepository
                        .findByTransactionId(transactionId);

                if (!instanceStage.isEmpty() && !("PND".equals(presentWorkflowInstance.getApprovalStatus()))) {
                    // if(false){
                    response.put("message", "This stage is already completed");
                    return response;
                } else {
                    Stage currentStage = stageRepository
                            .findByWorkflowIdAndStageNumber(workflowInstance.getWorkflow().getId(), currenStageNumber);

                    List<String> missingFields = checkFields(inputs, currentStage);
                    if (missingFields.isEmpty()) {

                        // Process the stage for that particular workflowInstance and create a
                        // workflowInstanceStage
                        workflowInstance.setCurrentStage(currentStage.getStageNumber());
                        workflowInstance.setCurrentStageName(currentStage.getStageName());
                        // createWorkflowInstanceStage(workflowInstance, currentStage,"COM");
                        Stage presentStageData = getDecisionLogicAndMethodName(currentStage);
                        String presentMethodName = presentStageData.getMethodName();
                        String presentDecisonLogic = presentStageData.getDecisionJson();
                        if (presentDecisonLogic != null && presentMethodName != null) {
                            String packageName = "com.iexceed.appzillon.sd.WorkFlow.BussinessLogic";
                            String className = presentMethodName;
                            Map<String, Object> BussinessLogicResponse = null;
                            try {
                                // Load the class
                                Class<?> clazz = Class.forName(packageName + "." + className);

                                // Get the method by name
                                Method method = clazz.getMethod(presentMethodName, LinkedHashMap.class, String.class,
                                        String.class);

                                // Use ApplicationContext to get the Spring-managed bean instance
                                Object beanInstance = applicationContext.getBean(clazz);
                                // Invoke the method
                                BussinessLogicResponse = (Map<String, Object>) method.invoke(beanInstance, inputs,
                                        presentDecisonLogic, transactionId);
                                if ("PND".equals(BussinessLogicResponse.get("approvalStatus"))
                                        || "RCL".equals(BussinessLogicResponse.get("approvalStatus"))) {
                                    WorkflowInstance presentWorkflow = workflowInstanceRepository
                                            .findByTransactionId(transactionId);
                                    presentWorkflow
                                            .setApprovalStatus("PND");
                                    presentWorkflow.setCurrentStage(presentStageNumber);
                                    workflowInstanceRepository.save(presentWorkflow);
                                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                                    presentStage.put("status", BussinessLogicResponse.get("approvalStatus"));
                                    response.put("currentStage", presentStage);
                                    response.put("transactionId", transactionId);
                                    response.put("message", BussinessLogicResponse.get("message"));
                                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage,
                                            (String) BussinessLogicResponse.get("approvalStatus"));
                                    return response;
                                } else if ("REJ".equals(BussinessLogicResponse.get("approvalStatus"))) {
                                    WorkflowInstance presentWorkflow = workflowInstanceRepository
                                            .findByTransactionId(transactionId);
                                    presentWorkflow
                                            .setApprovalStatus((String) BussinessLogicResponse.get("approvalStatus"));
                                    presentWorkflow.setTransactionId(transactionId + "-D");
                                    workflowInstanceRepository.save(presentWorkflow);
                                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                                    presentStage.put("status", BussinessLogicResponse.get("approvalStatus"));
                                    response.put("currentStage", presentStage);
                                    response.put("transactionId", transactionId);
                                    response.put("message", "The process has been rejected.");
                                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage,
                                            (String) BussinessLogicResponse.get("approvalStatus"));
                                    return response;
                                } else {
                                    WorkflowInstance presentWorkflow = workflowInstanceRepository
                                            .findByTransactionId(transactionId);
                                    presentWorkflow
                                            .setApprovalStatus((String) BussinessLogicResponse.get("approvalStatus"));
                                    workflowInstanceRepository.save(presentWorkflow);
                                    response.put("transactionId", transactionId);
                                }
                                String status = (String) BussinessLogicResponse.get("status");
                                logger.info("BussinessLogicResponse " + BussinessLogicResponse.get("status"));
                                int stageToMoved = getMovedStage(status, presentDecisonLogic);
                                logger.info("StageToMoved " + stageToMoved);
                                boolean isStageMoved = stageJumper(transactionId, stageToMoved);
                                if (currenStageNumber >= stageToMoved) {
                                    boolean isStageHistorySoftDelete = stageHistorySoftDelete(transactionId,
                                            stageToMoved - 1);
                                }
                                if (isStageMoved) {
                                    Stage movedStage = stageRepository.findByWorkflowIdAndStageNumber(
                                            workflowInstance.getWorkflow().getId(), stageToMoved);

                                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                                    presentStage.put("status", "COM");
                                    response.put("currentStage", presentStage);
                                    response.put("nextStage", toPascalCaseWithSpaces(movedStage.getStageName()));
                                    if ((int) BussinessLogicResponse.get("statusCode") == 400) {
                                        response.put("message", BussinessLogicResponse.get("message"));
                                        response.put("status", "Failed");
                                    } else {
                                        response.put("message", "Successfully moved to stage " + stageToMoved + ".");
                                        response.put("status", "Success");
                                    }
                                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage, "COM");
                                    isWorkFLowCompleted(transactionId,noOfStages);
                                    return response;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.error("Error invoking method: " + e.getMessage());
                                // throw new RuntimeException("An error occurred during method execution.", e);
                            }
                        }
                        // Generate the stage completion response by calling the
                        // generateStageCompletionResponse method
                        response = generateStageCompletionResponse(workflowInstance, currentStage);
                        createWorkflowInstanceStage(workflowInstance, transactionId, currentStage, "COM");
                        isWorkFLowCompleted(transactionId,noOfStages);
                        return response;

                    } else {
                        // Generate response for missing fields.
                        response = generateMissingFieldsResponse(missingFields, currentStage);
                        return response;
                    }
                }
            } else {

                // Generate all stages completion response by calling
                // generateAllStagesCompletionResponse() method.
                response = generateAllStagesCompletionResponse();
                return response;
            }
        }
    }

    public void  isWorkFLowCompleted(String transactionId,int noOfStages){
        logger.info("isWorkFLowCompleted is Triggered");
        WorkflowInstance presentWorkFlow=workflowInstanceRepository.findByTransactionId(transactionId);
        if(noOfStages==presentWorkFlow.getCurrentStage()){
            presentWorkFlow.setWorkFlowStatus("COM");
            workflowInstanceRepository.save(presentWorkFlow);
        }
    }

    public boolean stageHistorySoftDelete(String transactionId, int stageToMoved) {
        // Fetch all workflow stages for the transaction ID
        List<WorkflowInstanceStage> workflowInstanceStages = workflowInstanceStageRepository
                .findAllByTransactionId(transactionId);

        boolean isUpdated = false; // Flag to track if any updates were made

        for (WorkflowInstanceStage stage : workflowInstanceStages) {
            // Check if the current stage number is greater than the given stageToMoved
            if (stage.getCurrentStageNumber() > stageToMoved) {
                // Add "-D" to the transaction ID
                String updatedTransactionId = stage.getTransactionId() + "-D";
                stage.setTransactionId(updatedTransactionId);

                // Save the updated entity to persist the changes
                workflowInstanceStageRepository.save(stage);

                // Set flag to true as an update has been made
                isUpdated = true;
            }
        }

        return isUpdated; // Return true if any updates were made, false otherwise
    }

    public boolean stageJumper(String transactionId, int stageToMove) {
        try {
            logger.info("Transaction ID: " + transactionId);

            // Retrieve the WorkflowInstance using the transactionId
            WorkflowInstance presentWorkflowInstance = workflowInstanceRepository.findByTransactionId(transactionId);

            // If no WorkflowInstance is found, return false
            if (presentWorkflowInstance == null) {
                logger.error("No WorkflowInstance found with Transaction ID: " + transactionId);
                return false;
            }

            // Set the new stage and save
            presentWorkflowInstance.setCurrentStage(stageToMove - 1);
            workflowInstanceRepository.save(presentWorkflowInstance);

            // Operation successful, return true
            return true;
        } catch (Exception e) {
            logger.error("Error while updating stage for Transaction ID: " + transactionId, e);
            return false; // Return false if any exception occurs
        }
    }

    private int getMovedStage(String status, String presentDecisonLogic) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;

        try {
            // Parse JSON string into Map
            jsonMap = objectMapper.readValue(presentDecisonLogic, Map.class);

            // Get the "outgoing" list from the map
            List<Map<String, Object>> outgoingList = (List<Map<String, Object>>) jsonMap.get("outgoing"); // Iterate
                                                                                                          // through the
                                                                                                          // outgoingList
                                                                                                          // to find the
                                                                                                          // matching
                                                                                                          // kycStatus
            for (Map<String, Object> item : outgoingList) {
                // Extract the condition map
                Map<String, Object> condition = (Map<String, Object>) item.get("condition");

                // Check if the kycStatus matches the condition
                if (condition != null && status.equals(condition.get("status"))) {
                    // Return the target value
                    return (Integer) item.get("target");
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Handle exception
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception
        }

        // Return a default value if no matching kycStatus is found
        return -1;
    }

    private Map<String, Object> handleNewWorkflowInstance(Workflow workflow, Stage currentStage,
            Map<String, Object> inputs) {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> presentStage = new HashMap<>();
        // Generating transactionId
        String transactionId = "TXNID" + String.format("%010d", new Random().nextInt(1_000_000_000));

        // Create a new workflow instance
        WorkflowInstance workflowInstance = createWorkflowInstance(transactionId, workflow, currentStage);
        int noOfStages = workflowInstance.getWorkflow().getStages().size();
        int presentStageNumber = workflowInstance.getCurrentStage();
        int currenStageNumber = presentStageNumber + 1;
        // Save the new workflow instance
        workflowInstanceRepository.save(workflowInstance);

        // Create a workflowInstanceStage for this workflowInstance and Stage
        // createWorkflowInstanceStage(workflowInstance, currentStage,"COM");
        workflowInstance.setCurrentStage(currentStage.getStageNumber());
        workflowInstance.setCurrentStageName(currentStage.getStageName());
        Stage presentStageData = getDecisionLogicAndMethodName(currentStage);
        String presentMethodName = presentStageData.getMethodName();
        String presentDecisonLogic = presentStageData.getDecisionJson();
        if (presentDecisonLogic != null && presentMethodName != null) {
            String packageName = "com.iexceed.appzillon.sd.WorkFlow.BussinessLogic";
            String className = presentMethodName;
            Map<String, Object> BussinessLogicResponse = null;
            try {
                // Load the class
                Class<?> clazz = Class.forName(packageName + "." + className);

                // Get the method by name
                Method method = clazz.getMethod(presentMethodName, LinkedHashMap.class, String.class);

                // Use ApplicationContext to get the Spring-managed bean instance
                Object beanInstance = applicationContext.getBean(clazz);
                // Invoke the method
                BussinessLogicResponse = (Map<String, Object>) method.invoke(beanInstance, inputs, transactionId);
                // if((boolean) BussinessLogicResponse.get("applicationId")){
                // response.put("applicationId",BussinessLogicResponse.get("applicationId"));
                // }
                if ("PND".equals(BussinessLogicResponse.get("approvalStatus"))
                        || "RCL".equals(BussinessLogicResponse.get("approvalStatus"))) {
                    boolean isStageMoved = stageJumper(transactionId, 1);
                    WorkflowInstance presentWorkflowInstance = workflowInstanceRepository
                            .findByTransactionId(transactionId);
                    presentWorkflowInstance.setApprovalStatus("PND");
                    workflowInstanceRepository.save(presentWorkflowInstance);
                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                    presentStage.put("status", BussinessLogicResponse.get("approvalStatus"));
                    response.put("currentStage", presentStage);
                    response.put("transactionId", transactionId);
                    response.put("message", BussinessLogicResponse.get("message"));
                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage,
                            (String) BussinessLogicResponse.get("approvalStatus"));
                    return response;
                } else if ("REJ".equals(BussinessLogicResponse.get("approvalStatus"))) {
                    WorkflowInstance presentWorkflow = workflowInstanceRepository.findByTransactionId(transactionId);
                    presentWorkflow.setApprovalStatus((String) BussinessLogicResponse.get("approvalStatus"));
                    presentWorkflow.setTransactionId(transactionId + "-D");
                    workflowInstanceRepository.save(presentWorkflow);
                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                    presentStage.put("status", BussinessLogicResponse.get("approvalStatus"));
                    response.put("currentStage", presentStage);
                    response.put("transactionId", transactionId);
                    response.put("message", "The process has been rejected.");
                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage,
                            (String) BussinessLogicResponse.get("approvalStatus"));
                    return response;
                } else {
                    WorkflowInstance presentWorkflow = workflowInstanceRepository.findByTransactionId(transactionId);
                    presentWorkflow.setApprovalStatus((String) BussinessLogicResponse.get("approvalStatus"));
                    workflowInstanceRepository.save(presentWorkflow);
                }
                String status = (String) BussinessLogicResponse.get("status");
                logger.info("BussinessLogicResponse " + BussinessLogicResponse.get("status"));
                int stageToMoved = getMovedStage(status, presentDecisonLogic);
                logger.info("StageToMoved " + stageToMoved);
                boolean isStageMoved = stageJumper(transactionId, stageToMoved);
                if (currenStageNumber > stageToMoved) {
                    boolean isStageHistorySoftDelete = stageHistorySoftDelete(transactionId, stageToMoved - 1);
                }
                if (isStageMoved) {
                    Stage movedStage = stageRepository
                            .findByWorkflowIdAndStageNumber(workflowInstance.getWorkflow().getId(), stageToMoved);

                    presentStage.put("stageName", toPascalCaseWithSpaces(currentStage.getStageName()));
                    presentStage.put("status", "COM");
                    response.put("transactionId", transactionId);
                    response.put("currentStage", presentStage);
                    response.put("nextStage", toPascalCaseWithSpaces(movedStage.getStageName()));
                    if ((int) BussinessLogicResponse.get("statusCode") == 400) {
                        response.put("message", BussinessLogicResponse.get("message"));
                        response.put("status", "Failed");
                    } else {
                        response.put("message", "Successfully moved to stage " + stageToMoved + ".");
                        response.put("status", "Success");

                    }
                    createWorkflowInstanceStage(workflowInstance, transactionId, currentStage, "COM");
                    return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error invoking method: " + e.getMessage());
                throw new RuntimeException("An error occurred during method execution.", e);
            }
        }
        // Generate the stage completion response by calling the generateStageCompletion
        // Method
        response = generateStageCompletionResponse(workflowInstance, currentStage);
        return response;
    }

    private WorkflowInstance createWorkflowInstance(String transactionId, Workflow workflow, Stage currentStage) {

        // Create a new workflow instance
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTransactionId(transactionId); // Renamed
        workflowInstance.setCurrentStageName(currentStage.getStageName());
        workflowInstance.setWorkflow(workflow);
        workflowInstance.setCurrentStage(currentStage.getStageNumber());
        workflowInstance.setWorkFlowStatus("PND");
        workflowInstance.setStages(new ArrayList<>());

        return workflowInstance;
    }

    private void createWorkflowInstanceStage(WorkflowInstance workflowInstance, String transactionId,
            Stage currentStage, String status) {
        int currenStageNumber = currentStage.getStageNumber();
        WorkflowInstanceStage isWorkflowInstanceStagePresent = workflowInstanceStageRepository
                .findTopByTransactionIdAndCurrentStageNumber(transactionId, currenStageNumber);
        if (isWorkflowInstanceStagePresent != null) {
            isWorkflowInstanceStagePresent.setStatus(status);
            workflowInstanceStageRepository.save(isWorkflowInstanceStagePresent);
        } else {
            // Create a workflowInstanceStage for this workflowInstance and Stage
            WorkflowInstanceStage workflowInstanceStage = new WorkflowInstanceStage();
            workflowInstanceStage.setWorkflowInstance(workflowInstance);
            workflowInstanceStage.setCurrentStageNumber(currentStage.getStageNumber());
            workflowInstanceStage.setTransactionId(workflowInstance.getTransactionId()); // Renamed
            workflowInstanceStage.setStatus(status);

            // Add the stage to the workflow instance
            List<WorkflowInstanceStage> stagesList = workflowInstance.getStages();
            if (stagesList == null) {
                stagesList = new ArrayList<>();
            }
            stagesList.add(workflowInstanceStage);
            workflowInstance.setStages(stagesList);

            // Save the updated workflow instance
            workflowInstanceRepository.save(workflowInstance);
        }

    }

    private Map<String, Object> generateWorkflowNotFoundResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Failure");
        response.put("message", "Workflow not found");
        return response;
    }

    private Map<String, Object> generateInitialStageNotFoundResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Failure");
        response.put("message", "Initial stage not found");
        return response;
    }

    private Map<String, Object> generateMissingFieldsResponse(List<String> missingFields, Stage currentStage) {

        // Prepare the response.
        Map<String, Object> response = new HashMap<>();
        String fields = String.join(", ", missingFields);
        response.put("status", "failure");
        response.put("message", "Some fields are missing: " + fields);

        // Add the current stage to the response
        Map<String, Object> presentStage = new HashMap<>();
        presentStage.put("stageName", currentStage.getStageName());
        presentStage.put("status", "REJ");
        response.put("currentStage", presentStage);

        return response;
    }

    private Map<String, Object> generateStageCompletionResponse(WorkflowInstance workflowInstance, Stage currentStage) {

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", workflowInstance.getTransactionId()); // Renamed
        response.put("status", "success");

        // Add the current stage to the response
        Map<String, Object> presentStage = new HashMap<>();
        presentStage.put("stageName", currentStage.getStageName());
        presentStage.put("status", "COM");
        response.put("currentStage", presentStage);

        // Add the next Stage for the response if it is present
        Stage nextStage = stageRepository.findByWorkflowIdAndStageNumber(workflowInstance.getWorkflow().getId(),
                currentStage.getStageNumber() + 1);
        if (nextStage != null) {
            response.put("nextStage", nextStage.getStageName());
        }

        return response;
    }

    private Map<String, Object> generateInvalidTransactionIdResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failure");
        response.put("message", "Invalid Transaction ID");
        return response;
    }

    private Map<String, Object> generateAllStagesCompletionResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();

        // Generate response for all stages completion.
        response.put("message", "All Stages are completed");
        response.put("status", "Success");
        return response;
    }

    private Map<String, Object> generateNoInputResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "input is missing");
        return response;
    }

    private Map<String, Object> generateNoWorkflowIdResponse() {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "workflowId is missing.");
        return response;
    }

    public Map<String, Object> workflowInfo(Map<String, Object> inputs) {
        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        // workflow response to add to the response
        Map<String, Object> workflowDetails = new HashMap<>();

        // Check if the inputs contains the workflowId
        if (!inputs.containsKey("workflowId")) {
            return generateNoWorkflowIdResponse();
        }

        // Check if the workflowId is in correct format
        int workflowId;
        try {
            workflowId = (int) inputs.get("workflowId");
        } catch (ClassCastException e) {

            return generateInvalidWorkflowIdFormat();
        }

        // FInd the workflow from the repository using the id
        Optional<Workflow> optionalWorkflow = workflowRepository.findById(workflowId);
        if (!optionalWorkflow.isPresent()) {
            response = generateWorkflowNotFoundResponse();
            return response;
        }

        // Get workflow
        Workflow workflow = optionalWorkflow.get();

        // Check the inputs contains stageId
        if (inputs.containsKey("stageId")) {

            int stageId = (int) inputs.get("stageId");
            Stage stage = stageRepository.findByWorkflowIdAndStageNumber(workflowId, stageId);
            List<Field> fields = fieldRepository.findByStageNumberAndWorkflowId(stageId, workflowId);
            List<String> fieldNames = fields.stream().map(Field::getFieldName).collect(Collectors.toList());

            // Prepare the stage response
            Map<String, Object> stageDetails = new HashMap<>();
            stageDetails.put("requiredFields", fieldNames);
            stageDetails.put("stageName", stage.getStageName());

            if (inputs.containsKey("transactionId")) {
                String transactionId = (String) inputs.get("transactionId");
                WorkflowInstance workflowInstance;
                workflowInstance = workflowInstanceRepository.findByTransactionIdAndWorkflow(transactionId, workflow);
                if (workflowInstance == null) {
                    return generateWorkflowIdTransactionIdMisMatch();
                }

                workflowInstance = workflowInstanceRepository.findByTransactionId(transactionId);
                if (workflowInstance == null) {
                    return generateInvalidTransactionIdResponse();
                }

                Optional<WorkflowInstanceStage> instanceStage = workflowInstanceStageRepository
                        .findByTransactionIdAndCurrentStageNumberAndWorkflowInstance(transactionId, stageId,
                                workflowInstance);
                if (instanceStage.isPresent()) {
                    stageDetails.put("status", instanceStage.get().getStatus());
                } else {
                    stageDetails.put("status", "PENDING");
                }
            }

            // put the stage details to the response
            response.put("stage", stageDetails);
            // add workflow id and name in response
            response.put("workflowId", workflowId);
            response.put("workflowName", workflow.getName());

            return response;

        } else {
            List<Stage> stages = stageRepository.findByWorkflow(workflow); // find stages for the workflow

            // Map the stage response to the stageName
            List<Map<String, Object>> stagesList = new ArrayList<>();

            stages.forEach(stage -> { // traverse through each stage
                // get fields and the field names
                List<Field> fields = fieldRepository.findByStageNumberAndWorkflowId(stage.getStageNumber(), workflowId);
                List<String> fieldNames = fields.stream().map(Field::getFieldName).collect(Collectors.toList());

                // prepare a individual stage response
                Map<String, Object> stageDetail = new HashMap<>();
                stageDetail.put("stageId", stage.getStageNumber());
                stageDetail.put("requiredFields", fieldNames);
                stageDetail.put("stageName", stage.getStageName());

                // add the each stage detail with the stageName to the response
                stagesList.add(stageDetail);

            });

            // add the workflowId and workflowName - all stages to the response
            response.put("workflowId", workflowId);
            response.put("workflowName", workflow.getName());
            response.put("stages", stagesList);
            return response;
        }
    }

    public Map<String, Object> generateInvalidWorkflowIdFormat() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid workflowId format. Must be an integer.");
        return response;
    }

    public Map<String, Object> generateWorkflowIdTransactionIdMisMatch() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "No Data Found,Mismatch in the workflowId and transactionId");
        return response;
    }

    public static String toPascalCaseWithSpaces(String input) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // Add a space before any uppercase letter (except the first letter)
            if (Character.isUpperCase(c) && i != 0) {
                result.append(" ");
            }

            // Append the character, making the first character uppercase
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
