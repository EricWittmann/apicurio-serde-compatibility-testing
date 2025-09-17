# Confluent SerDe Test Script

This script tests the Confluent Schema Registry serialization and deserialization flow end-to-end.

## What the script does:

1. **Builds** confluent-schema-registry-serde project
2. **ConfluentSerializer**: Publishes 2 messages using Confluent Schema Registry SerDe
3. **ConfluentDeserializer**: Consumes the messages using Confluent Schema Registry SerDe
4. **Validates**: Checks that the round-trip serialization/deserialization worked correctly

## Usage:

```bash
./run-confluent-test.sh
```

The script automatically generates a random topic name like `confluent-test-1726500123-4567`.

## Validation Checks:

The script performs 4 validation checks:

1. ✅ **Consumer Completion**: Looks for "Done (success)" message
2. ✅ **Message Consumption**: Counts "Consumed a message:" entries
3. ✅ **Total Count**: Verifies "Total messages consumed" > 0
4. ✅ **Data Integrity**: Checks for expected Person data (TestFirst1/TestLast1)

## Exit Codes:

- **0**: All tests passed (4/4) ✅
- **1**: Partial success (2-3/4) ⚠️
- **2**: Tests failed (0-1/4) ❌

## Sample Output:

```
========================================
Confluent SerDe Test Script
========================================

Generated random topic: confluent-test-1726500123-4567

Step 1: Building confluent-schema-registry-serde project...
✓ Project built successfully

Step 2: Running ConfluentSerializer (publishing 2 messages)...
[Producer output...]

Step 3: Waiting 2 seconds for messages to be available...

Step 4: Running ConfluentDeserializer (consuming messages)...
[Consumer output...]

Step 5: Validating results...
✓ Consumer completed successfully
✓ Consumed 2 message(s)
✓ Total messages consumed: 2
✓ Person data found in consumed messages

========================================
🎉 ALL TESTS PASSED! (4/4)
Confluent SerDe is working correctly
========================================
```

## Prerequisites:

- Kafka running on localhost:9092
- Apicurio Registry running on localhost:8080 (for ccompat endpoint)
- confluent-schema-registry-serde project buildable

## Troubleshooting:

If tests fail:
- Check that Kafka and Apicurio Registry are running
- Verify the ccompat endpoint is available at localhost:8080/apis/ccompat/v7
- Run individual components to isolate issues:
  ```bash
  cd confluent-schema-registry-serde
  mvn exec:java -Dexec.mainClass="com.example.producer.ConfluentSerializer" -Dexec.args="--topicName test-topic"
  mvn exec:java -Dexec.mainClass="com.example.consumer.ConfluentDeserializer" -Dexec.args="--topicName test-topic"
  ```