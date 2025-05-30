#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BLUE='\033[0;34m'

# API endpoint
API_URL="http://localhost:8080/api/test"

# Function to print test header
print_test_header() {
    echo -e "\n${BLUE}=== Test $1: $2 ===${NC}"
}

# Function to run a test
run_test() {
    local test_num=$1
    local test_name=$2
    local method=$3
    local content_type=$4
    local data=$5

    print_test_header "$test_num" "$test_name"
    
    if [ -z "$content_type" ]; then
        curl -v -X "$method" "$API_URL" -d "$data"
    else
        curl -v -X "$method" "$API_URL" -H "Content-Type: $content_type" -d "$data"
    fi
    
    echo -e "\n"
}

# Test 1: Basic Test
run_test "1" "Basic Test" "POST" "text/plain" "hello world"

# Test 2: Empty Input
run_test "2" "Empty Input" "POST" "text/plain" ""

# Test 3: Long Input
run_test "3" "Long Input" "POST" "text/plain" "This is a very long string that might test any potential buffer or length limitations in the system. Let's see how it handles this much data. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."

# Test 4: Special Characters
run_test "4" "Special Characters" "POST" "text/plain" "!@#$%^&*()_+{}|:<>?[]\\;',./~\`"

# Test 5: Unicode Characters
run_test "5" "Unicode Characters" "POST" "text/plain" "Hello 世界! こんにちは! 안녕하세요!"

# Test 6: Missing Content-Type
run_test "6" "Missing Content-Type" "POST" "" "hello world"

# Test 7: Wrong Method
run_test "7" "Wrong Method" "GET" "text/plain" "hello world"

# Test 8: Multiple Requests
print_test_header "8" "Multiple Requests"
for i in {1..3}; do
    echo -e "${BLUE}Request $i${NC}"
    curl -v -X POST "$API_URL" -H "Content-Type: text/plain" -d "Request $i"
    echo -e "\n"
    sleep 1
done

echo -e "${GREEN}All tests completed!${NC}" 