package com.example.jmx.mbean.test;

/**
 * Simple JMX MBean interface for testing purposes.
 */
public interface TestMBean {
    /**
     * Test method that echoes the input string.
     * @param input The input string to echo
     * @return The echoed string with a prefix
     */
    String testStringArgument(String input);
} 