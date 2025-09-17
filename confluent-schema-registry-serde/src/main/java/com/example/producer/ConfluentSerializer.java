package com.example.producer;

import com.test.person.PersonOuterClass;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ConfluentSerializer {

    private static final String API_CURIO_CCOMPAT_REGISTRY_URL = "http://localhost:8080/apis/ccompat/v7";
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "multi-serde-test";
    private static final String SCHEMA_NAME = "PersonName2";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting example " + ConfluentSerializer.class.getSimpleName());

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

        // Create the producer.
        Producer<String, PersonOuterClass.Person> producer = createKafkaProducer();
        // Produce 2 messages.
        try {
            System.out.println("Producing (2) messages.");
            for (int idx = 0; idx < 2; idx++) {

                PersonOuterClass.Person person = PersonOuterClass.Person.newBuilder()
                        .setFirstName("TestFirst1").setLastName("TestLast1").setAge(31).build();

                // Send/produce the message on the Kafka Producer
                ProducerRecord<String, PersonOuterClass.Person> producedRecord = new ProducerRecord<>(topicName, key, person);
                producedRecord.headers().add("key", SCHEMA_NAME.getBytes());
                producer.send(producedRecord);

                Thread.sleep(100);
            }
            System.out.println("Messages successfully produced.");
        } finally {
            System.out.println("Closing the producer.");
            producer.flush();
            producer.close();
        }

        System.out.println("Done (success).");
        System.exit(0);
    }

    /**
     * Creates the Kafka producer.
     */
    private static Producer<String, PersonOuterClass.Person> createKafkaProducer() {
        Properties props = new Properties();

        // Configure kafka settings
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "Producer-" + TOPIC_NAME);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Use the confluent provided Kafka Serializer for Protobuf
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class.getName());
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, API_CURIO_CCOMPAT_REGISTRY_URL);
        props.put(AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class);
        // Register the schema if not found in the registry.
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, Boolean.TRUE);
        props.put(AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT, false);
        props.put(KafkaProtobufSerializerConfig.SKIP_KNOWN_TYPES_CONFIG, true);

        // Create the Kafka producer
        return new KafkaProducer<>(props);
    }

}
