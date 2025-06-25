package com.iexceed.appzillon.sd.WorkFlow.BussinessLogic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class decideAction {
    //requestedAction:fundTransfer
    // private static final String API_URL = "https://mocki.io/v1/f09ed32c-e3d0-44f6-8f1a-80d4f981b215";
    //requestedAction: billPayment
    // private static final String API_URL = "https://mocki.io/v1/b1bc27ef-f1ce-4af6-9466-f26a592d7442";
    // requestedAction:loanApplication
    // private static final String API_URL="https://mocki.io/v1/8da8317f-b056-4c04-a8f1-90316a8213c8";
    // requestedAction:accountManagement
    // private static final String API_URL="https://mocki.io/v1/01f31007-9693-4de0-bb01-a07be611ec7c";
    // requestedAction:customerSupport
    private static final String API_URL ="http://localhost:8080/api/backend/v1/decideAction";
    public Map<String, Object> decideAction(LinkedHashMap<String, Object> input, String presentDecisonLogic,String transactionId) {
        Map<String, Object> responseMap = null;
        try {
            // Create URL object
            URL url = new URL(API_URL);
    
            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);  // Enable writing to the connection
    
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
    
            // Convert response to Map
            responseMap = objectMapper.readValue(response.toString(), Map.class);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseMap;
    }
}
