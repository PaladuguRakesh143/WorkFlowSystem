package com.iexceed.appzillon.sd.WorkFlow.BussinessLogic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class processLoanApplication {
    // "creditScore": {"$gte": 700}
    private static final String API_URL="http://localhost:8080/api/backend/v1/processLoanApplication";
    // {"creditScore": {"$lt": 700}
    // private static final String API_URL="https://mocki.io/v1/e8abe8bf-a9df-4ada-b68e-939a54470eaf";

    public Map<String, Object> processLoanApplication(LinkedHashMap<String, Object> input, String presentDecisonLogic,String transactionId) throws JsonMappingException, JsonProcessingException  {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Convert the decision logic string to a Map<String, Object>
        Map<String, Object> decisionLogicMap = objectMapper.readValue(presentDecisonLogic, LinkedHashMap.class);
        System.out.println("Input: " + input);
        System.out.println("Decision Logic Map: " + decisionLogicMap);
        
        // Extract the initialDeposit from input
        Integer creditScore = (Integer) input.get("creditScore");
        System.out.println("creditScore: " + creditScore);

        // Extract the outgoing conditions from decisionLogicMap
        List<Map<String, Object>> outgoingConditions = (List<Map<String, Object>>) decisionLogicMap.get("outgoing");

        // Iterate through each condition and evaluate based on initialDeposit
        for (Map<String, Object> conditionObj : outgoingConditions) {
            Map<String, Object> condition = (Map<String, Object>) conditionObj.get("condition");

            // Extract the deposit conditions
            Map<String, Object> depositCondition = (Map<String, Object>) condition.get("creditScore");

            // Check if there's a condition for $gte (greater than or equal to)
            if (depositCondition.containsKey("$gte")) {
                Integer gteValue = (Integer) depositCondition.get("$gte");
                if (creditScore >= gteValue) {
                    responseMap.put("status", condition.get("status"));
                    responseMap.put("statusCode",200);
                    break;
                }
            }

            // Check if there's a condition for $lt (less than)
            if (depositCondition.containsKey("$lt")) {
                Integer ltValue = (Integer) depositCondition.get("$lt");
                if (creditScore < ltValue) {
                    responseMap.put("message", "A credit score of at least 700 is required.");
                    responseMap.put("status", condition.get("status"));
                    responseMap.put("statusCode",400);
                    break;
                }
            }
        }
        
        return responseMap;
    }
}
