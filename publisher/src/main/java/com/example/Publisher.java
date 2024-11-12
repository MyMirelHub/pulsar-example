package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

@SpringBootApplication
public class Publisher {
    private static final String PUBSUB_NAME = "pulsar-pubsub";
    private static final String TOPIC_NAME = "messages";

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Publisher.class, args);        

        int messageCount = 1;

        DaprClient client = new DaprClientBuilder().build();
        while (true) {
            String partitionKey = "key-partition";// + (messageCount % 3); // Using 3 different keys
            String message = String.format("Message #%d (p-Key: %s)", messageCount, partitionKey); 
            
            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("partitionKey", partitionKey);

            client.publishEvent(
                PUBSUB_NAME,
                TOPIC_NAME,
                message,
                metadata).block();
            
            System.out.println("Published message: " + message);
            messageCount++;
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
