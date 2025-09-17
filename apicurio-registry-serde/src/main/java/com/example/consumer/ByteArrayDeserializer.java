package com.example.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.test.person.PersonOuterClass;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

public class ByteArrayDeserializer {

    private static final String API_CURIO_REGISTRY_URL = "http://localhost:8080/apis/registry/v3";
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "multi-serde-test";
    private static final String SCHEMA_NAME = "PersonName3";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting example " + ByteArrayDeserializer.class.getSimpleName());

        // Parse command line arguments
        String topicName = TOPIC_NAME; // default value

        for (int i = 0; i < args.length - 1; i++) {
            if ("--topicName".equals(args[i])) {
                topicName = args[i + 1];
                break;
            }
        }

        String key = SCHEMA_NAME;

        System.out.println("Using topic: " + topicName);

        // Create the consumer
        System.out.println("Creating the consumer.");
        KafkaConsumer<String, byte[]> consumer = createKafkaConsumer();

        // Subscribe to the topic
        // Consume all available messages.
        try (consumer) {
            System.out.println("Subscribing to topic " + topicName);
            consumer.subscribe(Collections.singletonList(topicName));
            int messageCount = 0;
            int emptyPollCount = 0;
            final int maxEmptyPolls = 3; // Stop after 3 consecutive empty polls

            System.out.println("Consuming all available messages...");
            while (emptyPollCount < maxEmptyPolls) {
                final ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(2));

                if (records.count() == 0) {
                    emptyPollCount++;
                    System.out.println("No messages in poll " + emptyPollCount + "/" + maxEmptyPolls + "...");
                } else {
                    emptyPollCount = 0; // Reset counter when we get messages
                    messageCount += records.count();
                    System.out.println("Found " + records.count() + " messages in this poll...");

                    records.forEach(record -> {
                        byte[] raw = record.value();

                        // Output the raw byte array as base64 encoded string
                        String base64Raw = Base64.getEncoder().encodeToString(raw);
                        System.out.println("Raw bytes (base64): " + base64Raw);
//
//                        try {
//                            byte[] payload = Arrays.copyOfRange(raw, 14, raw.length);
//                            PersonOuterClass.Person msg = PersonOuterClass.Person.parseFrom(payload);
//                            String json = JsonFormat.printer().includingDefaultValueFields().print(msg);
//                            System.out.println("Parsed JSON: " + json);
//                        } catch (InvalidProtocolBufferException e) {
//                            throw new RuntimeException(e);
//                        }
                    });
                }
            }

            System.out.println("Total messages consumed: " + messageCount);
        }
        System.out.println("Done (success).");
        System.exit(0);
    }

    /**
     * Creates the Kafka consumer.
     */
    private static KafkaConsumer<String, byte[]> createKafkaConsumer() {
        Properties props = new Properties();

        // Configure Kafka
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "ByteArrayConsumer-" + TOPIC_NAME);
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.putIfAbsent(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Use the byte-array provided Kafka Deserializer for Protobuf
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArrayDeserializer.class.getName());

        // Create the Kafka Consumer
        return new KafkaConsumer<>(props);
    }

}
