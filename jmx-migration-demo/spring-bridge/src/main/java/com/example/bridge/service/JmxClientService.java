package com.example.bridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.management.*;
import javax.management.remote.*;
import java.io.IOException;

@Service
@Slf4j
public class JmxClientService {
    
    @Value("${jmx.server.url:service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi}")
    private String jmxServerUrl;
    
    @Value("${jmx.mbean.name:com.example.jmx:type=Test}")
    private String mbeanName;
    
    private JMXConnector connector;
    
    private JMXConnector getConnection() throws IOException {
        if (connector == null) {
            try {
                log.info("Creating new JMX connection");
                JMXServiceURL url = new JMXServiceURL(jmxServerUrl);
                connector = JMXConnectorFactory.connect(url);
            } catch (Exception e) {
                log.error("Failed to create JMX connection", e);
                throw new RuntimeException("Failed to create JMX connection", e);
            }
        }
        return connector;
    }
    
    public Object invokeOperation(String operationName, Object[] params, String[] signature) 
            throws Exception {
        JMXConnector connector = getConnection();
        MBeanServerConnection mbeanServerConnection = connector.getMBeanServerConnection();
        ObjectName objectName = new ObjectName(mbeanName);
        
        log.info("Invoking JMX operation: {} with {} parameters", operationName, 
                params != null ? params.length : 0);
        
        try {
            return mbeanServerConnection.invoke(objectName, operationName, params, signature);
        } catch (MBeanException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
    
    public void closeConnection() {
        if (connector != null) {
            try {
                connector.close();
                log.info("Closed JMX connection");
            } catch (IOException e) {
                log.error("Error closing JMX connection", e);
            }
            connector = null;
        }
    }
} 