// package com.iexceed.appzillon.sd.Backend.service;

// import java.io.IOException;
// import java.util.HashMap;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.iexceed.appzillon.sd.Backend.entity.SDJson;
// import com.iexceed.appzillon.sd.Backend.repository.ScreenDataRepository;

// @Service
// public class BackendService {

//     @Autowired
//     private ScreenDataRepository screenDataRepository;

//     private final ObjectMapper objectMapper = new ObjectMapper();

//     public Map<String, Object> fetchConfigData() {
//         SDJson screenData = screenDataRepository.findByModuleNameAndScreenName("CRMModule", "Config_BackEnd");
//         Map<String,Object> response= new HashMap<>();
//         if (screenData != null) {
//             try {
//                 // Convert JSON string to Map
//                 return objectMapper.readValue(screenData.getJson(), new TypeReference<Map<String, Object>>() {});
//             } catch (IOException e) {
//                 // Handle JSON parsing exception
//                 throw new RuntimeException("Error parsing JSON", e);
//             }
//         } else {
//             response.put("Message","null data");
//             return response;
//         }
//     }
// }


package com.iexceed.appzillon.sd.Backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexceed.appzillon.sd.Backend.entity.SDJson;
import com.iexceed.appzillon.sd.Backend.repository.ScreenDataRepository;
import com.iexceed.appzillon.sd.WorkFlow.service.WorkflowService;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackendService {


     private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    @Autowired
    private ScreenDataRepository screenDataRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> configData;

    @PostConstruct
    public void init() {
        fetchConfigData();
    }

    public Map<String, Object> fetchConfigData() {
        logger.info("Fetch config data is Triggered");
        SDJson screenData = screenDataRepository.findByModuleNameAndScreenName("CRMModule", "Config_BackEnd");
        configData = new HashMap<>();
        if (screenData != null) {
            try {
                // Convert JSON string to Map and store it
                configData = objectMapper.readValue(screenData.getJson(), new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                // Handle JSON parsing exception
                configData.put("Message", "Error parsing JSON");
                configData.put("Error", e.getMessage());
            }
        } else {
            configData.put("Message", "No data found");
        }
        return configData;
    }

    public Map<String, Object> getConfigData() {
        // Return the preloaded config data
        return configData;
    }


    public Map<String, Object> handleApiCall(String apiName, Map<String, Object> requestBody) {
        System.out.println("Handle Api call is Triggered");
        this.fetchConfigData();
        List<Map<String, Object>> configList = (List<Map<String, Object>>) configData.get("config");

        // Find the API configuration by apiName
        Map<String, Object> apiConfig = configList.stream()
        .filter(config -> apiName.equals(config.get("endpoint")))  // Ensure you're using the correct key
        .findFirst()
        .orElse(null);

        if (apiConfig == null) {
            throw new RuntimeException("Endpoint not found in configuration");
        }
    
        // Extract request formatting details from the config (if present)
    Map<String, Object> requestConfig = (Map<String, Object>) apiConfig.get("request");
    List<String> params = requestConfig != null ? (List<String>) requestConfig.get("params") : null;
    Map<String, String> headersConfig = requestConfig != null ? (Map<String, String>) requestConfig.get("headers") : new HashMap<>();

    // Validate and format the request only if a request body is provided and request format is defined
    Map<String, Object> formattedRequestBody = new HashMap<>();
    if (requestBody != null) {
        if (params != null) {
            formattedRequestBody = formatRequestBody(requestBody, params);
        } else {
            // If no params are defined, use the request body as is
            formattedRequestBody = requestBody;
        }
    }

    // Create headers for the third-party API request
    HttpHeaders headers = new HttpHeaders();
    headersConfig.forEach(headers::set);
    // Create the request entity with headers and body
    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(formattedRequestBody, headers);

    // Retrieve the third-party API endpoint
    String thirdPartyApiEndpoint = (String) apiConfig.get("thirdPartyApiEndpoint");
    String type = (String) apiConfig.get("type");
    RestTemplate restTemplate = new RestTemplate();
    HttpMethod httpMethod = getHttpMethod(type);
    // Making the third-party API call
    ResponseEntity<Map> responseEntity = restTemplate.exchange(thirdPartyApiEndpoint, httpMethod, requestEntity, Map.class);

    // Get the response structure from the configuration (if present)
    Map<String, Object> responseConfig = (Map<String, Object>) apiConfig.get("response");

    // Format the response according to the configuration or return the full response if no structure is provided
    if (responseConfig == null || responseConfig.isEmpty()) {
        return responseEntity.getBody();  // Return full response if no response format is defined
    }

    // Format the response based on the provided structure
    return formatResponse(responseEntity.getBody(), responseConfig);
    }

    private Map<String, Object> formatResponse(Map<String, Object> responseBody, Map<String, Object> responseConfig) {
        Map<String, Object> formattedResponse = new HashMap<>();

        // Extract the structure part of the response configuration
        Map<String, String> responseStructure = (Map<String, String>) responseConfig.get("structure");

        // Map fields from the third-party API response to the configured response structure
        responseStructure.forEach((key, value) -> {
            if (responseBody.containsKey(value)) {
                formattedResponse.put(key, responseBody.get(value));
            } else {
                throw new RuntimeException("Missing expected field in third-party response: " + value);
            }
        });

        return formattedResponse;
    }

    private Map<String, Object> formatRequestBody(Map<String, Object> requestBody, List<String> requiredParams) {
        Map<String, Object> formattedRequestBody = new HashMap<>();

        // Validate and include only the required parameters from the request body
        for (String param : requiredParams) {
            if (!requestBody.containsKey(param)) {
                throw new RuntimeException("Missing required parameter: " + param);
            }
            formattedRequestBody.put(param, requestBody.get(param));
        }

        return formattedRequestBody;
    }

    private HttpMethod getHttpMethod(String method) {
        switch (method.toUpperCase()) {
            case "GET":
                return HttpMethod.GET;
            case "POST":
                return HttpMethod.POST;
            case "PUT":
                return HttpMethod.PUT;
            case "DELETE":
                return HttpMethod.DELETE;
            case "PATCH":
                return HttpMethod.PATCH;
            case "OPTIONS":
                return HttpMethod.OPTIONS;
            case "HEAD":
                return HttpMethod.HEAD;
            case "TRACE":
                return HttpMethod.TRACE;
            default:
                return null;  // Return null for invalid method types
        }
    }
}
