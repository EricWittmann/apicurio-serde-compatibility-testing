package com.example.producer;

import com.test.person.PersonOuterClass;
import io.apicurio.registry.serde.config.KafkaSerdeConfig;
import io.apicurio.registry.serde.config.SerdeConfig;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class APICurioSerializer {

    private static final String API_CURIO_REGISTRY_URL = "http://localhost:8080/apis/registry/v3";
    private static final String SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "multi-serde-test";
    private static final String SCHEMA_NAME = "PersonName3";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting example " + APICurioSerializer.class.getSimpleName());

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

        // Create the producer
        Producer<String, PersonOuterClass.Person> producer = createKafkaProducer();
        // Produce 2 messages.
        try {
            System.out.println("Producing (2) messages.");
            for (int idx = 0; idx < 2; idx++) {

                PersonOuterClass.Person person = PersonOuterClass.Person.newBuilder()
                        .setFirstName("TestFirst3").setLastName("TestLast3").setAge(31).build();

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
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufKafkaSerializer.class.getName());
        // Configure Service Registry location
        props.putIfAbsent(SerdeConfig.REGISTRY_URL, API_CURIO_REGISTRY_URL);
        // Register the schema if not found in the registry.
        props.putIfAbsent(SerdeConfig.AUTO_REGISTER_ARTIFACT, Boolean.TRUE);
//        props.putIfAbsent(KafkaSerdeConfig.ENABLE_HEADERS, Boolean.TRUE);
        props.putIfAbsent(SerdeConfig.SEND_TYPE_REF, Boolean.FALSE);
        props.putIfAbsent(SerdeConfig.SEND_INDEXES, Boolean.TRUE);

        // Create the Kafka producer
        return new KafkaProducer<>(props);
    }

}
