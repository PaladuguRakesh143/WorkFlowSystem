// package com.iexceed.appzillon.sd.Backend.controller;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.iexceed.appzillon.sd.Backend.service.BackendService;

// @RestController
// @RequestMapping("/api/backend")
// public class BackendController {

//     @Autowired
//     private BackendService backendService;

//     @GetMapping("/config")
//     public Map<String,Object> getConfigData() {
//         return backendService.fetchConfigData();
//     }
// }


package com.iexceed.appzillon.sd.Backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iexceed.appzillon.sd.Backend.service.BackendService;

@RestController
@RequestMapping("/api/backend")
public class BackendController {

    @Autowired
    private BackendService backendService;

    @GetMapping("/config")
    public Map<String, Object> getConfigData() {
        // Fetch preloaded config data
        return backendService.getConfigData();
    }

    @PostMapping("/v1/{apiName}")
    public Map<String, Object> callPostApi(@PathVariable String apiName, @RequestBody Map<String, Object> requestBody) {
        // Call the backend service to handle the API call and formatting
        System.out.println("Triggered the POST API");
        return backendService.handleApiCall(apiName, requestBody);
    }

    @GetMapping("/v1/{apiName}")
    public Map<String, Object> callGetApi(@PathVariable String apiName, @RequestBody(required = false) Map<String, Object> requestBody) {
        // Call the backend service to handle the API call and formatting
        return backendService.handleApiCall(apiName, requestBody);
    }
}
