# Kerberos Integration Guide for JMX-REST Bridge

## Overview

This guide explains how to implement proper Kerberos authentication for the JMX-REST bridge in production environments.

## Architecture

```
Angular App → [SPNEGO] → Spring Boot → [Kerberos Delegation] → JMX Server
```

## Spring Security Kerberos Configuration

### 1. Update Maven Dependencies

```xml
<dependency>
    <groupId>org.springframework.security.kerberos</groupId>
    <artifactId>spring-security-kerberos-web</artifactId>
    <version>1.0.1.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.springframework.security.kerberos</groupId>
    <artifactId>spring-security-kerberos-core</artifactId>
    <version>1.0.1.RELEASE</version>
</dependency>
```

### 2. Kerberos Security Configuration

```java
@Configuration
@EnableWebSecurity
public class KerberosSecurityConfig {
    
    @Value("${app.kerberos.service-principal}")
    private String servicePrincipal;
    
    @Value("${app.kerberos.keytab-location}")
    private String keytabLocation;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                spnegoAuthenticationProcessingFilter(authenticationManager()),
                BasicAuthenticationFilter.class
            )
            .exceptionHandling()
                .authenticationEntryPoint(spnegoEntryPoint());
        
        return http.build();
    }
    
    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }
    
    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        SpnegoAuthenticationProcessingFilter filter = 
            new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }
    
    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = 
            new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(kerberosUserDetailsService());
        return provider;
    }
    
    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        SunJaasKerberosTicketValidator validator = 
            new SunJaasKerberosTicketValidator();
        validator.setServicePrincipal(servicePrincipal);
        validator.setKeyTabLocation(new FileSystemResource(keytabLocation));
        validator.setDebug(true);
        return validator;
    }
    
    @Bean
    public UserDetailsService kerberosUserDetailsService() {
        return username -> {
            // Load user details from your user store
            // Map Kerberos principal to application user
            return User.builder()
                .username(username)
                .password("") // No password needed for Kerberos
                .authorities(loadAuthoritiesForUser(username))
                .build();
        };
    }
}
```

### 3. JMX Connection with Kerberos Delegation

```java
@Service
public class KerberosJmxClientService {
    
    @Value("${jmx.server.principal}")
    private String jmxServerPrincipal;
    
    public JMXConnector createKerberosConnection(String delegatedTicket) 
            throws Exception {
        
        // Create JAAS configuration for Kerberos
        Map<String, Object> env = new HashMap<>();
        
        // Use delegated credentials
        Subject subject = createSubjectFromDelegatedTicket(delegatedTicket);
        env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER, 
                this.getClass().getClassLoader());
        
        // Perform connection as delegated user
        return Subject.doAs(subject, 
            (PrivilegedExceptionAction<JMXConnector>) () -> {
                JMXServiceURL url = new JMXServiceURL(jmxServerUrl);
                return JMXConnectorFactory.connect(url, env);
            });
    }
    
    private Subject createSubjectFromDelegatedTicket(String ticket) {
        // Create Subject with Kerberos credentials
        Subject subject = new Subject();
        
        // Add Kerberos ticket to subject
        KerberosTicket krbTicket = decodeKerberosTicket(ticket);
        subject.getPrivateCredentials().add(krbTicket);
        
        // Add principal
        KerberosPrincipal principal = krbTicket.getClient();
        subject.getPrincipals().add(principal);
        
        return subject;
    }
}
```

### 4. Angular Kerberos Integration

```typescript
// Angular HTTP Interceptor for Kerberos
@Injectable()
export class KerberosInterceptor implements HttpInterceptor {
  
  intercept(req: HttpRequest<any>, next: HttpHandler): 
      Observable<HttpEvent<any>> {
    
    // Browser will handle SPNEGO negotiation automatically
    // when withCredentials is set to true
    const kerberosReq = req.clone({
      withCredentials: true
    });
    
    return next.handle(kerberosReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Handle Kerberos authentication failure
          console.error('Kerberos authentication failed');
        }
        return throwError(error);
      })
    );
  }
}

// Configure in app.module.ts
providers: [
  {
    provide: HTTP_INTERCEPTORS,
    useClass: KerberosInterceptor,
    multi: true
  }
]
```

## Configuration Properties

### application.yml

```yaml
app:
  kerberos:
    service-principal: HTTP/myserver.example.com@EXAMPLE.COM
    keytab-location: /etc/security/keytabs/http.keytab
    
jmx:
  server:
    principal: jmxserver/myserver.example.com@EXAMPLE.COM
    
spring:
  security:
    kerberos:
      debug: true  # Enable for troubleshooting
```

## JMX Server Kerberos Configuration

### 1. JMX Server Startup Options

```bash
java -Djavax.security.auth.useSubjectCredsOnly=false \
     -Djava.security.auth.login.config=/path/to/jaas.conf \
     -Djava.security.krb5.conf=/etc/krb5.conf \
     -Dcom.sun.management.jmxremote.authenticate=true \
     -Dcom.sun.management.jmxremote.login.config=JmxServer \
     -jar jmx-server.jar
```

### 2. JAAS Configuration (jaas.conf)

```
JmxServer {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/jmxserver.keytab"
    principal="jmxserver/myserver.example.com@EXAMPLE.COM"
    debug=true;
};
```

## Testing Kerberos Integration

### 1. Verify Kerberos Tickets

```bash
# Check if you have valid tickets
klist

# Get new tickets if needed
kinit username@EXAMPLE.COM
```

### 2. Test with cURL

```bash
# Test SPNEGO authentication
curl --negotiate -u : http://localhost:8080/api/users/current
```

### 3. Browser Configuration

Configure browsers for Kerberos:

**Firefox:**
- Navigate to `about:config`
- Set `network.negotiate-auth.trusted-uris` to include your server

**Chrome:**
- Start with `--auth-server-whitelist="*.example.com"`

## Troubleshooting

### Common Issues

1. **Clock Skew**: Ensure all servers have synchronized time (NTP)
2. **DNS**: Kerberos requires proper DNS resolution (forward and reverse)
3. **SPNs**: Verify Service Principal Names are correctly registered
4. **Keytabs**: Check keytab permissions and principals

### Debug Logging

Enable Kerberos debug logging:

```java
System.setProperty("sun.security.krb5.debug", "true");
System.setProperty("sun.security.spnego.debug", "true");
```

## Security Best Practices

1. **Credential Delegation**: Only enable when necessary
2. **Keytab Security**: Restrict keytab file permissions (600)
3. **Principal Validation**: Always validate principals before use
4. **Encryption**: Use strong encryption types (AES256)
5. **Audit Logging**: Log all authentication events

## Conclusion

Implementing Kerberos provides seamless single sign-on for users while maintaining the security requirements of JMX connections. The delegation mechanism allows the Spring Boot bridge to act on behalf of users when connecting to JMX servers. 