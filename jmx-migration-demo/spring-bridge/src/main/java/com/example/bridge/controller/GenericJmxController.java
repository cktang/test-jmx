package com.example.bridge.controller;

import com.example.bridge.service.JmxClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jmx")
public class GenericJmxController {
    private static final Logger logger = LoggerFactory.getLogger(GenericJmxController.class);

    @Autowired
    private JmxClientService jmxClientService;

    @PostMapping("/invoke")
    public Object invokeMethod(@RequestBody Map<String, Object> request) {
        try {
            String methodName = (String) request.get("methodName");
            List<Object> args = (List<Object>) request.get("arguments");
            List<String> argTypes = (List<String>) request.get("argumentTypes");

            if (methodName == null) {
                throw new IllegalArgumentException("methodName is required");
            }

            logger.info("Invoking JMX method: {} with arguments: {} and types: {}", 
                methodName, args, argTypes);

            Object result = jmxClientService.invokeOperation(
                methodName,
                args != null ? args.toArray() : new Object[0],
                argTypes != null ? argTypes.toArray(new String[0]) : new String[0]
            );

            logger.info("JMX operation result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Failed to invoke JMX operation", e);
            throw new RuntimeException("Failed to invoke JMX operation: " + e.getMessage(), e);
        }
    }
}