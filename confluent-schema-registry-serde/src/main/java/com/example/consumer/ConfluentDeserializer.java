package com.example.consumer;

import com.test.person.PersonOuterClass;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConfluentDeserializer {

    private static final String API_CURIO_CCOMPAT_REGISTRY_URL = "http://localhost:8080/apis/ccompat/v7";
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "multi-serde-test";
    private static final String SCHEMA_NAME = "PersonName4";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting example " + ConfluentDeserializer.class.getSimpleName());

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
        KafkaConsumer<String, PersonOuterClass.Person> consumer = createKafkaConsumer();

        // Subscribe to the topic
        // Consume currently available messages.
        try (consumer) {
            System.out.println("Subscribing to topic " + topicName);
            consumer.subscribe(Collections.singletonList(topicName));
            int messageCount = 0;

            System.out.println("Consuming currently available messages...");

            // Single poll to get all currently available messages
            final ConsumerRecords<String, PersonOuterClass.Person> records = consumer.poll(Duration.ofSeconds(5));

            if (records.count() == 0) {
                System.out.println("No messages currently available.");
            } else {
                messageCount = records.count();
                System.out.println("Found " + records.count() + " messages.");

                records.forEach(record -> {
                    PersonOuterClass.Person value = record.value();
                    System.out.println("Consumed a message: " + value.toString());
                });
            }

            System.out.println("Total messages consumed: " + messageCount);
        }
        System.out.println("Done (success).");
        System.exit(0);
    }

    /**
     * Creates the Kafka consumer.
     */
    private static KafkaConsumer<String, PersonOuterClass.Person> createKafkaConsumer() {
        Properties props = new Properties();

        // Configure Kafka
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "ConfluentConsumer-" + TOPIC_NAME);
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.putIfAbsent(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Use the apicurio provided Kafka Deserializer for Protobuf
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class.getName());
        // Configure Service Registry location
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, API_CURIO_CCOMPAT_REGISTRY_URL);
        props.putIfAbsent(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, PersonOuterClass.Person.class);
        // Create the Kafka Consumer
        return new KafkaConsumer<>(props);
    }

}
