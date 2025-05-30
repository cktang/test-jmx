#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BLUE='\033[0;34m'

# API endpoint
API_URL="http://localhost:8080/api/jmx/invoke"

# Function to print test header
print_test_header() {
    echo -e "\n${BLUE}=== Test $1: $2 ===${NC}"
}

# Function to run a test
run_test() {
    local test_num=$1
    local test_name=$2
    local json_data=$3
    local expected_status=$4

    print_test_header "$test_num" "$test_name"
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "$json_data")
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ Test passed (Status: $status_code)${NC}"
        echo "Response: $body"
    else
        echo -e "${RED}✗ Test failed (Expected: $expected_status, Got: $status_code)${NC}"
        echo "Response: $body"
    fi
    
    echo -e "\n"
}

# Test 1: Basic String Method
run_test "1" "Basic String Method" '{
    "methodName": "testStringArgument",
    "arguments": ["hello world"],
    "argumentTypes": ["java.lang.String"]
}' 200

# Test 2: Empty String
run_test "2" "Empty String" '{
    "methodName": "testStringArgument",
    "arguments": [""],
    "argumentTypes": ["java.lang.String"]
}' 200

# Test 3: Special Characters
run_test "3" "Special Characters" '{
    "methodName": "testStringArgument",
    "arguments": ["!@#$%^&*()_+{}|:<>?"],
    "argumentTypes": ["java.lang.String"]
}' 200

# Test 4: Missing Method Name
run_test "4" "Missing Method Name" '{
    "arguments": ["hello world"],
    "argumentTypes": ["java.lang.String"]
}' 500

# Test 5: Invalid Argument Type
run_test "5" "Invalid Argument Type" '{
    "methodName": "testStringArgument",
    "arguments": ["hello world"],
    "argumentTypes": ["java.lang.Integer"]
}' 500

# Test 6: Missing Arguments
run_test "6" "Missing Arguments" '{
    "methodName": "testStringArgument",
    "argumentTypes": ["java.lang.String"]
}' 500

echo -e "${GREEN}All tests completed!${NC}" 