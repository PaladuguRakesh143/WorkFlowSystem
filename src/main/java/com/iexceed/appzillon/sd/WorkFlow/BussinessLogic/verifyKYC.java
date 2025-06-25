package com.iexceed.appzillon.sd.WorkFlow.BussinessLogic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexceed.appzillon.sd.Backend.service.BackendService;

@Component
public class verifyKYC {
    @Autowired
    public BackendService backendService;

    // status:verified
    // private static final String API_URL ="http://192.168.10.251:8081/requests/create";
    // status:failed
    private static final String API_URL = "http://localhost:8080/api/backend/v1/getverifyKYC";

    public Map<String, Object> verifyKYC(LinkedHashMap<String, Object> input) {
        return verifyKYC(input, null, null); // Pass null when presentDecisonLogic is not provided
    }

    public Map<String, Object> verifyKYC(LinkedHashMap<String, Object> input, String transactionId) {
        return verifyKYC(input, null, transactionId);
    }

    public Map<String, Object> verifyKYC(LinkedHashMap<String, Object> input, String presentDecisonLogic,
            String transactionId) {
        Map<String, Object> responseMap = null;

        Map<String, Object> RequestMap = new LinkedHashMap<>();

        // Adding transaction details to the response
        RequestMap.put("wfTransactionId", transactionId);
        RequestMap.put("wfStageId", 1);
        RequestMap.put("ApprovalTypeId", 3);
        // RequestMap.put("employeeId", 4);

        // Creating a nested userData structure
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("fullName", input.get("fullName"));
        userData.put("dateOfBirth", input.get("dateOfBirth"));
        userData.put("socialSecurityNumber", input.get("socialSecurityNumber"));
        userData.put("email", input.get("email"));
        userData.put("phone", input.get("phone"));
        userData.put("initialDeposit", String.valueOf(input.get("initialDeposit"))); // Convert to String if necessary
        userData.put("kycDocuments", input.get("kycDocuments"));

        // Add userData to the responseMap
        RequestMap.put("userData", userData);

        // Log and return the responseMap
        System.out.println("Formatted Response: " + RequestMap);

        try {
            // Create URL object
            URL url = new URL(API_URL);

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true); // Enable writing to the connection

            // Convert input map to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonInputString = objectMapper.writeValueAsString(input);

            // Send the JSON input in the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] inputBytes = jsonInputString.getBytes("utf-8");
                os.write(inputBytes, 0, inputBytes.length);
            }

            // Get the response code
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            

            responseMap = objectMapper.readValue(response.toString(), Map.class);
            if("COM".equals(responseMap.get("approvalStatus"))){
                responseMap.put("status","success");
                responseMap.put("statusCode",200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseMap;
    }
}
