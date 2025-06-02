package com.example.bridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
// import org.springframework.security.kerberos.authentication.KerberosAuthenticationToken;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;
import javax.management.remote.JMXPrincipal;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class JmxClientService {
    
    @Value("${jmx.server.url:service:jmx:jmxmp://localhost:9999}")
    private String jmxServerUrl;
    
    @Value("${jmx.mbean.name:com.example.jmx:type=Test}")
    private String mbeanName;
    
    private JMXConnector connector;
    
    private JMXConnector getConnection() throws IOException {
        if (connector == null) {
            try {
                log.info("Creating new JMXMP connection to: {}", jmxServerUrl);
                JMXServiceURL url = new JMXServiceURL(jmxServerUrl);
                
                // Set up environment for JMXMP connection
                Map<String, Object> environment = new HashMap<>();
                
                connector = JMXConnectorFactory.connect(url, environment);
                log.info("Successfully connected to JMXMP server");
            } catch (Exception e) {
                log.error("Failed to create JMXMP connection", e);
                throw new RuntimeException("Failed to create JMXMP connection", e);
            }
        }
        return connector;
    }
    
    /**
     * DEMONSTRATION METHOD: Shows how you would extract a Kerberos subject from HTTP request
     * This is NOT called in the current implementation - just for understanding the concept
     */
    private Subject extractKerberosSubjectFromHttpRequest() {
        try {
            // Get the current Spring Security authentication context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            log.info("Current authentication type: {}", 
                    authentication != null ? authentication.getClass().getSimpleName() : "null");
            
            // Option 1: If you have Kerberos authentication configured, you'd check for KerberosAuthenticationToken
            // Commented out since we don't have Kerberos setup yet
            /*
            if (authentication instanceof KerberosAuthenticationToken) {
                KerberosAuthenticationToken kerberosToken = (KerberosAuthenticationToken) authentication;
                Subject kerberosSubject = kerberosToken.getTicket(); // This contains the original Kerberos ticket
                
                if (kerberosSubject != null) {
                    log.info("Found Kerberos subject with {} principals: {}", 
                            kerberosSubject.getPrincipals().size(), kerberosSubject.getPrincipals());
                    return kerberosSubject;
                }
            }
            */
            
            // Option 2: Alternative approach - get subject from current access control context
            // This would work if the HTTP request was processed with Subject.doAs()
            Subject currentSubject = Subject.getSubject(AccessController.getContext());
            if (currentSubject != null && !currentSubject.getPrincipals().isEmpty()) {
                log.info("Found subject in access control context with principals: {}", 
                        currentSubject.getPrincipals());
                return currentSubject;
            }
            
            // Option 3: Check if authentication has Kerberos-like properties
            if (authentication != null) {
                log.info("Authentication principal: {}", authentication.getPrincipal());
                log.info("Authentication details: {}", authentication.getDetails());
                log.info("Authentication authorities: {}", authentication.getAuthorities());
                
                // In a real Kerberos setup, you might extract subject from authentication details
                // or create a subject based on the principal name
            }
            
            log.debug("No Kerberos subject found in current HTTP request context");
            return null;
            
        } catch (Exception e) {
            log.warn("Error extracting Kerberos subject from HTTP request", e);
            return null;
        }
    }
    
    /**
     * DEMONSTRATION METHOD: Shows how you would use a Kerberos subject with JMX
     * This shows the Subject.doAs pattern you'd use once you have the subject
     */
    private Object demonstrateSubjectForwarding(Subject kerberosSubject, String operationName, 
            Object[] params, String[] signature) throws Exception {
        
        log.info("DEMO: This is how you'd forward Kerberos subject to JMX");
        log.info("Subject principals: {}", kerberosSubject.getPrincipals());
        
        // The key pattern: Subject.doAs() - same as your mock implementation but with real subject
        return Subject.doAs(kerberosSubject, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                // Inside this block, the current subject is the Kerberos subject
                Subject currentSubject = Subject.getSubject(AccessController.getContext());
                log.info("Subject available in JMX call context: {}", 
                        currentSubject != null ? currentSubject.getPrincipals() : "null");
                
                // This is where you'd make the actual JMX call
                // The JMX server can now access the Kerberos subject via Subject.getSubject()
                return invokeRegular(operationName, params, signature);
            }
        });
    }
    
    public Object invokeOperation(String operationName, Object[] params, String[] signature) 
            throws Exception {
        
        // Special handling for retrieveCommandUser to test subject propagation
        if ("retrieveCommandUser".equals(operationName)) {
            return invokeWithMockSubject(operationName, params, signature);
        }
        
        // Regular invocation for other operations
        return invokeRegular(operationName, params, signature);
    }
    
    private Object invokeWithMockSubject(String operationName, Object[] params, String[] signature) 
            throws Exception {
        log.info("Creating mock subject for JMXMP operation: {}", operationName);
        
        // Create mock subject with JMXPrincipal
        Set<Principal> principals = new HashSet<>();
        principals.add(new JMXPrincipal("jmxmp-bridge-user"));
        
        Subject mockSubject = new Subject(false, principals, new HashSet<>(), new HashSet<>());
        
        log.info("Executing JMXMP call with mock subject principals: {}", mockSubject.getPrincipals());
        
        // Execute the JMX call within the subject context
        return Subject.doAs(mockSubject, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                // Test that the subject is available locally in Spring Bridge
                Subject currentSubject = Subject.getSubject(AccessController.getContext());
                if (currentSubject != null) {
                    log.info("SUCCESS: Mock subject is available in Spring Bridge JMXMP context with principals: {}", 
                            currentSubject.getPrincipals());
                } else {
                    log.warn("Mock subject not found in current access control context");
                }
                
                return invokeRegular(operationName, params, signature);
            }
        });
    }
    
    private Object invokeRegular(String operationName, Object[] params, String[] signature) 
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