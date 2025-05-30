# JMX to Angular Migration Demo

This demo shows how to migrate Java applications using JMX communication to a modern Angular web application while preserving the existing JMX infrastructure.

## Architecture Overview

```
Angular App → REST API (Spring Boot) → JMX → Legacy Java App
```

## Components

### 1. JMX Server (`jmx-server`)
A simple Java application that exposes JMX MBeans with:
- User management operations
- Authentication and authorization checks
- Complex object parameters and return types
- Simulated Kerberos authentication (using basic auth for demo)

### 2. Spring Boot Bridge (`spring-bridge`)
REST API bridge that:
- Exposes RESTful endpoints for Angular
- Maintains JMX connections per user
- Handles authentication and forwards credentials to JMX
- Serializes/deserializes complex Java objects to/from JSON
- Provides Swagger UI for API documentation

### 3. Angular Client (example usage)
Modern web application that consumes the REST API.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Node.js 14+ and npm (for Angular client)

## Quick Start

### 1. Build the JMX Server

```bash
cd jmx-server
mvn clean package
```

### 2. Build the Spring Bridge

First, install the JMX server JAR to local Maven repository:

```bash
cd jmx-server
mvn clean install
```

Then build the Spring Bridge:

```bash
cd ../spring-bridge
mvn clean package
```

### 3. Run the Applications

#### Start JMX Server (Terminal 1):
```bash
cd jmx-server
java -jar target/jmx-server-1.0-SNAPSHOT.jar
```

The JMX server will start on port 9999.

#### Start Spring Bridge (Terminal 2):
```bash
cd spring-bridge
java -jar target/spring-bridge-1.0-SNAPSHOT.jar
```

The REST API will be available at http://localhost:8080

### 4. Access the Applications

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## Test Credentials

- **Admin User**: username=`admin`, password=`admin123`
- **Regular User**: username=`user1`, password=`user123`

## API Examples

### Get Server Status (No Auth Required)
```bash
curl http://localhost:8080/api/users/server-status
```

### Authenticate User
```bash
curl -u admin:admin123 \
  http://localhost:8080/api/users/current
```

### Create User (Admin Only)
```bash
curl -u admin:admin123 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "roles": ["USER"],
    "address": {
      "street": "789 Elm St",
      "city": "Boston",
      "state": "MA",
      "zipCode": "02101"
    }
  }' \
  http://localhost:8080/api/users
```

### Search Users
```bash
curl -u admin:admin123 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username": "admin"}' \
  http://localhost:8080/api/users/search
```

## Angular Integration Example

Here's a simple Angular service to interact with the API:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private apiUrl = 'http://localhost:8080/api/users';
  
  constructor(private http: HttpClient) {}
  
  private getAuthHeaders(): HttpHeaders {
    const credentials = btoa('admin:admin123');
    return new HttpHeaders({
      'Authorization': `Basic ${credentials}`,
      'Content-Type': 'application/json'
    });
  }
  
  getServerStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/server-status`);
  }
  
  createUser(userProfile: any): Observable<any> {
    return this.http.post(this.apiUrl, userProfile, {
      headers: this.getAuthHeaders()
    });
  }
  
  searchUsers(criteria: any): Observable<any[]> {
    return this.http.post<any[]>(`${this.apiUrl}/search`, criteria, {
      headers: this.getAuthHeaders()
    });
  }
}
```

## Security Considerations

### Current Demo Implementation
- Uses Basic Authentication for simplicity
- Credentials are passed through to JMX
- CORS is configured for local development

### Production Recommendations
1. **Use Kerberos Authentication**:
   - Configure Spring Security Kerberos
   - Use delegation tokens for JMX connections
   - Implement proper SPNEGO negotiation

2. **Secure Communication**:
   - Enable HTTPS/TLS
   - Use secure JMX connections
   - Implement proper certificate validation

3. **Connection Management**:
   - Implement connection pooling limits
   - Add connection timeout handling
   - Monitor and log connection usage

4. **Error Handling**:
   - Sanitize error messages
   - Implement rate limiting
   - Add request validation

## Troubleshooting

### JMX Connection Errors
- Ensure JMX server is running on port 9999
- Check firewall settings
- Verify credentials are correct

### CORS Issues
- Update `app.cors.allowed-origins` in application.yml
- Ensure Angular app URL is whitelisted

### Authentication Failures
- Check Spring Security logs for details
- Verify Basic Auth header is properly formatted
- Ensure user has required roles for the operation

## Next Steps

1. **Implement Kerberos**: Replace basic auth with proper Kerberos authentication
2. **Add Caching**: Implement caching for frequently accessed JMX data
3. **Monitoring**: Add metrics and monitoring for JMX connections
4. **Error Recovery**: Implement automatic reconnection and circuit breakers
5. **WebSocket Support**: Add real-time updates for JMX notifications 