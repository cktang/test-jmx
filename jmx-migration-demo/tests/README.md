# API Test Script

This directory contains a test script for testing the `/api/test` endpoint of the Spring Bridge application.

## Prerequisites

- Bash shell
- `curl` command-line tool
- Spring Bridge running on `localhost:8080`
- JMX Server running

## How to Run Tests

1. Make sure both the JMX Server and Spring Bridge are running:
   ```bash
   # Terminal 1 - Start JMX Server
   cd jmx-migration-demo/jmx-server
   mvn clean package -DskipTests
   java -jar target/jmx-server-1.0-SNAPSHOT.jar

   # Terminal 2 - Start Spring Bridge
   cd jmx-migration-demo/spring-bridge
   mvn clean package -DskipTests
   java -jar target/spring-bridge-1.0-SNAPSHOT.jar
   ```

2. Make the test script executable:
   ```bash
   chmod +x test_api.sh
   ```

3. Run the tests:
   ```bash
   ./test_api.sh
   ```

## Test Cases

The script includes the following test cases:

1. **Basic Test**: Simple "hello world" message
2. **Empty Input**: Empty string
3. **Long Input**: Very long string to test buffer handling
4. **Special Characters**: String with special characters
5. **Unicode Characters**: String with Unicode characters
6. **Missing Content-Type**: Request without Content-Type header
7. **Wrong Method**: GET request instead of POST
8. **Multiple Requests**: Three requests in quick succession

## Expected Results

- Tests 1-5: Should return 200 OK with a response
- Test 6: Should return 415 Unsupported Media Type
- Test 7: Should return 405 Method Not Allowed
- Test 8: All requests should succeed, testing connection reuse

## Troubleshooting

If you encounter issues:

1. Check that both JMX Server and Spring Bridge are running
2. Verify the API endpoint is accessible: `curl http://localhost:8080/api/test`
3. Check the Spring Bridge logs for any error messages
4. Ensure the JMX connection is established (check JMX Server logs)

## Adding New Tests

To add new tests, edit `test_api.sh` and add a new `run_test` call with the appropriate parameters:

```bash
run_test "9" "New Test Name" "POST" "text/plain" "test data"
``` 