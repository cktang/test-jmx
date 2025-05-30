package com.example.bridge.controller;

import com.example.bridge.service.JmxClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private JmxClientService jmxClientService;

    @PostMapping("/test")
    public String testMethod(@RequestBody String input) {
        try {
            logger.info("Received test request with input: {}", input);
            Object result = jmxClientService.invokeOperation("testStringArgument", 
                new Object[]{input}, 
                new String[]{"java.lang.String"});
            logger.info("JMX operation result: {}", result);
            return (String) result;
        } catch (Exception e) {
            logger.error("Failed to invoke JMX operation", e);
            throw new RuntimeException("Failed to invoke JMX operation: " + e.getMessage(), e);
        }
    }
} 