package com.iexceed.appzillon.sd.WorkFlow.BussinessLogic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class updateAccountDetails {
    // "updateStatus": "success"
    private static final String API_URL ="http://localhost:8080/api/backend/v1/updateAccountDetails";

    // "updateStatus": "failed"
    // private static final String API_URL="https://mocki.io/v1/c2ba8a73-c474-4ef1-88cc-3a6c272a5c85";


    public Map<String, Object> updateAccountDetails(LinkedHashMap<String, Object> input, String presentDecisonLogic,String transactionId) {
        Map<String, Object> responseMap = null;
        try {
            // Create URL object
            URL url = new URL(API_URL);
            
            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            // Get the response code
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // Convert response to Map
            ObjectMapper objectMapper = new ObjectMapper();
            responseMap = objectMapper.readValue(response.toString(), Map.class);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return responseMap;
    }
}
