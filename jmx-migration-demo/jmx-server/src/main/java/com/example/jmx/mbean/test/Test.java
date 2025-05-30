package com.example.jmx.mbean.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of TestMBean for testing purposes.
 */
public class Test implements TestMBean {
    
    private static final Logger logger = LoggerFactory.getLogger(Test.class);
    
    @Override
    public String testStringArgument(String input) {
        logger.info("Test method called with input: {}", input);
        return "Echo: " + input;
    }
} 