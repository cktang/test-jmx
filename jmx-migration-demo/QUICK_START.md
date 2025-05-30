# Quick Start Guide for macOS

## Installing Maven

Since you have Homebrew installed, run:

```bash
brew install maven
```

This will install the latest Maven version.

## Running the Demo

### 1. Build and Run JMX Server

Open Terminal 1:
```bash
cd jmx-migration-demo/jmx-server
mvn clean install
java -jar target/jmx-server-1.0-SNAPSHOT.jar
```

### 2. Build and Run Spring Bridge

Open Terminal 2:
```bash
cd jmx-migration-demo/spring-bridge
mvn clean package
java -jar target/spring-bridge-1.0-SNAPSHOT.jar
```

### 3. Test the API

Open Terminal 3 or use a web browser:

Test server status (no auth required):
```bash
curl http://localhost:8080/api/users/server-status
```

Test with authentication:
```bash
curl -u admin:admin123 http://localhost:8080/api/users/current
```

Access Swagger UI:
Open http://localhost:8080/swagger-ui.html in your browser

## Troubleshooting

### Maven not found after installation
If Maven is not found after installation, you may need to refresh your shell:
```bash
source ~/.zshrc
```
Or open a new terminal window.

### Port already in use
If ports 9999 or 8080 are already in use:
- Kill the processes using those ports
- Or modify the ports in the configuration files

### Build failures
Make sure you're in the correct directory and that you've built the jmx-server first (it's a dependency for spring-bridge).

## Alternative: Running without Maven (using compiled JARs)

If you prefer not to install Maven, I can compile the JARs for you. Let me know! 