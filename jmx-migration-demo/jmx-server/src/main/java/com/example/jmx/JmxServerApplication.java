package com.example.jmx;

import com.example.jmx.mbean.test.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;

/**
 * JMX Server Application that exposes TestMBean.
 * Supports both local and remote JMX connections.
 */
public class JmxServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(JmxServerApplication.class);
    
    private static final String JMX_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi";
    private static final String MBEAN_NAME = "com.example.jmx:type=Test";
    
    private MBeanServer mbeanServer;
    private JMXConnectorServer connectorServer;
    
    public void start() throws Exception {
        logger.info("Starting JMX Server Application...");
        
        // Get the platform MBean server
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        // Create and register the MBean
        Test testMBean = new Test();
        ObjectName objectName = new ObjectName(MBEAN_NAME);
        
        try {
            mbeanServer.registerMBean(testMBean, objectName);
            logger.info("Registered MBean: {}", objectName);
        } catch (InstanceAlreadyExistsException e) {
            logger.warn("MBean already registered: {}", objectName);
        }
        
        // Start RMI registry for remote connections
        try {
            LocateRegistry.createRegistry(9999);
            logger.info("RMI registry started on port 9999");
        } catch (Exception e) {
            logger.info("RMI registry already running");
        }
        
        // Create JMX service URL
        JMXServiceURL serviceUrl = new JMXServiceURL(JMX_SERVICE_URL);
        
        // Create and start the JMX connector server without authentication
        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                serviceUrl, null, mbeanServer);
        
        connectorServer.start();
        logger.info("JMX Connector Server started at: {}", serviceUrl);
        
        // Print connection information
        printConnectionInfo();
    }
    
    private void printConnectionInfo() {
        logger.info("========================================");
        logger.info("JMX Server is running!");
        logger.info("Connection URL: {}", JMX_SERVICE_URL);
        logger.info("MBean ObjectName: {}", MBEAN_NAME);
        logger.info("========================================");
    }
    
    public void stop() {
        try {
            if (connectorServer != null) {
                connectorServer.stop();
                logger.info("JMX Connector Server stopped");
            }
        } catch (IOException e) {
            logger.error("Error stopping JMX Connector Server", e);
        }
    }
    
    public static void main(String[] args) {
        JmxServerApplication server = new JmxServerApplication();
        
        try {
            server.start();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down JMX Server...");
                server.stop();
            }));
            
            // Keep the server running
            logger.info("Press Ctrl+C to stop the server");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Error starting JMX Server", e);
            System.exit(1);
        }
    }
} 