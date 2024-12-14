package com.example;

import java.net.InetAddress;
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

        // Get hostname (pod name in Kubernetes)
        String hostname = InetAddress.getLocalHost().getHostName();
        System.out.println("Publisher started on pod: " + hostname);

        int messageCount = 1;

        DaprClient client = new DaprClientBuilder().build();
        while (true) {
            //String partitionKey = "key-" + hostname; // Using 3 different keys
            String partitionKey = "authoring-event-topic"; // Using one key
            String message = String.format("Message #%d (p-Key: %s)", messageCount, partitionKey); 
            
            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("partitionKey", partitionKey);

            try {
                client.publishEvent(
                PUBSUB_NAME,
                TOPIC_NAME,
                message,
                metadata).block();
            
            System.out.println("Published message: " + message);
            } catch (Exception e) {
                System.out.println("Failed to send message: " + e);
            }
            
            messageCount++;
            TimeUnit.SECONDS.sleep(1);
        }
    }
}