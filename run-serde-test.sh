#!/bin/bash

echo "========================================="
echo "Multi-SerDe Test Script"
echo "========================================="
echo ""

# Generate random topic name or use provided argument
if [ $# -eq 0 ]; then
    # Generate random topic name with timestamp and random number
    TIMESTAMP=$(date +%s)
    RANDOM_ID=$(shuf -i 1000-9999 -n 1)
    TOPIC_NAME="serde-test-${TIMESTAMP}-${RANDOM_ID}"
    echo "Generated random topic: $TOPIC_NAME"
else
    TOPIC_NAME="$1"
    echo "Using provided topic: $TOPIC_NAME"
fi
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to extract base64 payloads from output
extract_base64() {
    local output="$1"
    echo "$output" | grep "Raw bytes (base64):" | sed 's/Raw bytes (base64): //'
}

echo -e "${BLUE}Step 1: Building both projects...${NC}"
echo ""

# Build Apicurio project
echo "Building apicurio-registry-serde..."
cd apicurio-registry-serde
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build apicurio-registry-serde project${NC}"
    exit 1
fi
cd ..

# Build Confluent project
echo "Building confluent-schema-registry-serde..."
cd confluent-schema-registry-serde
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build confluent-schema-registry-serde project${NC}"
    exit 1
fi
cd ..

echo -e "${GREEN}✓ Both projects built successfully${NC}"
echo ""

echo -e "${BLUE}Step 2: Running APICurioSerializer (publishing 2 messages)...${NC}"
echo ""
cd apicurio-registry-serde
APICURIO_OUTPUT=$(mvn exec:java -Dexec.mainClass="com.example.producer.APICurioSerializer" -Dexec.args="--topicName $TOPIC_NAME" -q)
echo "$APICURIO_OUTPUT"
cd ..
echo ""

echo -e "${BLUE}Step 3: Running ConfluentSerializer (publishing 2 more messages)...${NC}"
echo ""
cd confluent-schema-registry-serde
CONFLUENT_OUTPUT=$(mvn exec:java -Dexec.mainClass="com.example.producer.ConfluentSerializer" -Dexec.args="--topicName $TOPIC_NAME" -q)
echo "$CONFLUENT_OUTPUT"
cd ..
echo ""

echo -e "${BLUE}Step 4: Waiting 2 seconds for messages to be available...${NC}"
sleep 2
echo ""

echo -e "${BLUE}Step 4: Running ByteArrayDeserializer (consuming all available messages)...${NC}"
echo ""
cd apicurio-registry-serde
CONSUMER_OUTPUT=$(timeout 30s mvn exec:java -Dexec.mainClass="com.example.consumer.ByteArrayDeserializer" -Dexec.args="--topicName $TOPIC_NAME" -q 2>&1)
CONSUMER_EXIT_CODE=$?
echo "$CONSUMER_OUTPUT"
cd ..

if [ $CONSUMER_EXIT_CODE -ne 0 ]; then
    echo -e "${YELLOW}⚠ Consumer may not have received all messages (exit code: $CONSUMER_EXIT_CODE)${NC}"
fi
echo ""

echo -e "${BLUE}Step 5: Extracting base64 encoded payloads...${NC}"
echo ""

# Extract base64 payloads
BASE64_PAYLOADS=$(extract_base64 "$CONSUMER_OUTPUT")

# Convert to array
IFS=$'\n' read -rd '' -a PAYLOAD_ARRAY <<< "$BASE64_PAYLOADS"

echo "========================================="
echo -e "${YELLOW}SUMMARY: Base64 Encoded Message Payloads${NC}"
echo "========================================="
echo ""

if [ ${#PAYLOAD_ARRAY[@]} -eq 0 ]; then
    echo -e "${RED}❌ No base64 payloads found in consumer output${NC}"
    echo ""
    echo "Consumer output was:"
    echo "$CONSUMER_OUTPUT"
else
    echo -e "${GREEN}✓ Found ${#PAYLOAD_ARRAY[@]} message payload(s):${NC}"
    echo ""

    for i in "${!PAYLOAD_ARRAY[@]}"; do
        echo -e "${YELLOW}Message $((i+1)):${NC}"
        echo "${PAYLOAD_ARRAY[i]}"
        echo "${PAYLOAD_ARRAY[i]}" | base64 -d | xxd
        echo ""
    done
fi
echo ""

echo -e "${BLUE}Step 6: Running ConfluentDeserializer (consuming all messages)...${NC}"
echo ""
cd confluent-schema-registry-serde
CONFLUENT_CONSUMER_OUTPUT=$(timeout 30s mvn exec:java -Dexec.mainClass="com.example.consumer.ConfluentDeserializer" -Dexec.args="--topicName $TOPIC_NAME" -q 2>&1)
CONFLUENT_CONSUMER_EXIT_CODE=$?
echo "$CONFLUENT_CONSUMER_OUTPUT"
cd ..

if [ $CONFLUENT_CONSUMER_EXIT_CODE -ne 0 ]; then
    echo -e "${YELLOW}⚠ ConfluentDeserializer may not have received all messages (exit code: $CONFLUENT_CONSUMER_EXIT_CODE)${NC}"
fi

echo "========================================="
echo -e "${GREEN}Test completed!${NC}"
echo "========================================="