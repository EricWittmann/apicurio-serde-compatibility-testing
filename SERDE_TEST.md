# Multi-SerDe Test Script

This script demonstrates and tests the differences between Apicurio Registry and Confluent Schema Registry serialization formats.

## What the script does:

1. **APICurioSerializer**: Publishes 2 messages using Apicurio Registry SerDe
2. **ConfluentSerializer**: Publishes 2 messages using Confluent Schema Registry SerDe
3. **ByteArrayDeserializer**: Consumes all available messages as raw bytes and outputs base64-encoded payloads
4. **ConfluentDeserializer**: Consumes all messages using Confluent Schema Registry deserializer
5. **Extract & Display**: Shows all base64-encoded message formats found

## Usage:

```bash
# Use random topic name (generated automatically)
./run-serde-test.sh

# Use custom topic name
./run-serde-test.sh my-custom-topic
```

**Random Topic Generation**: When run without arguments, the script generates a unique topic name like `serde-test-1726500123-4567` using timestamp and random number to avoid conflicts between test runs.

## Expected Output:

The script will show:
- Build status for both projects
- Output from each serializer (2 messages each)
- Output from ByteArrayDeserializer showing raw bytes + parsed JSON
- Output from ConfluentDeserializer showing deserialized messages
- **Summary with all base64-encoded payloads** showing the different serialization formats

## Message Structure:

- **Apicurio messages**: Include Apicurio Registry metadata headers (first 14 bytes) + protobuf
- **Confluent messages**: Include Confluent Schema Registry metadata headers + protobuf
- **Base64 output**: Complete raw bytes including all metadata + protobuf payload

## Running Individual Classes:

All Java classes now support an optional `--topicName` command line argument:

```bash
# APICurio Serializer
cd apicurio-registry-serde
mvn exec:java -Dexec.mainClass="com.example.producer.APICurioSerializer" -Dexec.args="--topicName my-topic"

# Confluent Serializer
cd confluent-schema-registry-serde
mvn exec:java -Dexec.mainClass="com.example.producer.ConfluentSerializer" -Dexec.args="--topicName my-topic"

# ByteArray Deserializer
cd apicurio-registry-serde
mvn exec:java -Dexec.mainClass="com.example.consumer.ByteArrayDeserializer" -Dexec.args="--topicName my-topic"

# Confluent Deserializer
cd confluent-schema-registry-serde
mvn exec:java -Dexec.mainClass="com.example.consumer.ConfluentDeserializer" -Dexec.args="--topicName my-topic"
```

If no `--topicName` argument is provided, each class uses its default topic name. This argument format allows for easy extension with additional parameters.

## Prerequisites:

- Kafka running on localhost:9092
- Apicurio Registry running on localhost:8080
- Both projects built successfully
- Topic (generated randomly each run, or specify custom name)
- `xxd` utility (for hex dump display of base64 decoded payloads)

## Troubleshooting:

If fewer messages than expected are consumed:
- Check that both Kafka and registries are running
- Verify topic creation and message persistence
- Run consumer separately: `cd apicurio-registry-serde && mvn exec:java -Dexec.mainClass="com.example.consumer.ByteArrayDeserializer" -Dexec.args="--topicName your-topic-name"`