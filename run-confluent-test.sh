#!/bin/bash

echo "========================================="
echo "Confluent SerDe Test Script"
echo "========================================="
echo ""

# Generate random topic name
TIMESTAMP=$(date +%s)
RANDOM_ID=$(shuf -i 1000-9999 -n 1)
TOPIC_NAME="confluent-test-${TIMESTAMP}-${RANDOM_ID}"
echo "Generated random topic: $TOPIC_NAME"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Building confluent-schema-registry-serde project...${NC}"
echo ""

# Build Confluent project
echo "Building confluent-schema-registry-serde..."
cd confluent-schema-registry-serde
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build confluent-schema-registry-serde project${NC}"
    exit 1
fi
cd ..

echo -e "${GREEN}✓ Project built successfully${NC}"
echo ""

echo -e "${BLUE}Step 2: Running ConfluentSerializer (publishing 2 messages)...${NC}"
echo ""
cd confluent-schema-registry-serde
PRODUCER_OUTPUT=$(mvn exec:java -Dexec.mainClass="com.example.producer.ConfluentSerializer" -Dexec.args="--topicName $TOPIC_NAME" -q 2>&1)
PRODUCER_EXIT_CODE=$?
echo "$PRODUCER_OUTPUT"
cd ..

if [ $PRODUCER_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}❌ Producer failed (exit code: $PRODUCER_EXIT_CODE)${NC}"
    exit 1
fi
echo ""

echo -e "${BLUE}Step 3: Waiting 2 seconds for messages to be available...${NC}"
sleep 2
echo ""

echo -e "${BLUE}Step 4: Running ConfluentDeserializer (consuming messages)...${NC}"
echo ""
cd confluent-schema-registry-serde
CONSUMER_OUTPUT=$(timeout 30s mvn exec:java -Dexec.mainClass="com.example.consumer.ConfluentDeserializer" -Dexec.args="--topicName $TOPIC_NAME" -q 2>&1)
CONSUMER_EXIT_CODE=$?
echo "$CONSUMER_OUTPUT"
cd ..

echo ""
echo -e "${BLUE}Step 5: Validating results...${NC}"
echo ""

# Check if consumer ran successfully
if [ $CONSUMER_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}❌ Consumer failed (exit code: $CONSUMER_EXIT_CODE)${NC}"
    exit 1
fi

# Check for success indicators in output
SUCCESS_CHECKS=0

# Check 1: Look for "Done (success)" message
if echo "$CONSUMER_OUTPUT" | grep -q "Done (success)"; then
    echo -e "${GREEN}✓ Consumer completed successfully${NC}"
    ((SUCCESS_CHECKS++))
else
    echo -e "${RED}❌ Consumer did not complete successfully${NC}"
fi

# Check 2: Look for consumed messages
MESSAGE_COUNT=$(echo "$CONSUMER_OUTPUT" | grep -c "Consumed a message:")
if [ "$MESSAGE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ Consumed $MESSAGE_COUNT message(s)${NC}"
    ((SUCCESS_CHECKS++))
else
    echo -e "${RED}❌ No messages were consumed${NC}"
fi

# Check 3: Look for "Total messages consumed" with count > 0
TOTAL_CONSUMED=$(echo "$CONSUMER_OUTPUT" | grep "Total messages consumed:" | grep -o '[0-9]\+' | tail -1)
if [ -n "$TOTAL_CONSUMED" ] && [ "$TOTAL_CONSUMED" -gt 0 ]; then
    echo -e "${GREEN}✓ Total messages consumed: $TOTAL_CONSUMED${NC}"
    ((SUCCESS_CHECKS++))
else
    echo -e "${RED}❌ No total message count found or count is 0${NC}"
fi

# Check 4: Look for Person data in consumed messages
if echo "$CONSUMER_OUTPUT" | grep -q "TestFirst1\|TestLast1"; then
    echo -e "${GREEN}✓ Person data found in consumed messages${NC}"
    ((SUCCESS_CHECKS++))
else
    echo -e "${RED}❌ Expected Person data not found in consumed messages${NC}"
fi

echo ""
echo "========================================="
if [ $SUCCESS_CHECKS -eq 4 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! ($SUCCESS_CHECKS/4)${NC}"
    echo -e "${GREEN}Confluent SerDe is working correctly${NC}"
    EXIT_CODE=0
elif [ $SUCCESS_CHECKS -ge 2 ]; then
    echo -e "${YELLOW}⚠ PARTIAL SUCCESS ($SUCCESS_CHECKS/4)${NC}"
    echo -e "${YELLOW}Some tests passed but there may be issues${NC}"
    EXIT_CODE=1
else
    echo -e "${RED}❌ TESTS FAILED ($SUCCESS_CHECKS/4)${NC}"
    echo -e "${RED}Confluent SerDe is not working correctly${NC}"
    EXIT_CODE=2
fi

echo ""
echo "Topic used: $TOPIC_NAME"
echo "========================================="

exit $EXIT_CODE