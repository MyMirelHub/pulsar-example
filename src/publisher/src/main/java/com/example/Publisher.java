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

        int count = 0;

        // Create metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("partitionKey", "pk");
            
        DaprClient client = new DaprClientBuilder().build();
        while (true) {
            String message = "Message " + count++;
            client.publishEvent(
                PUBSUB_NAME,
                TOPIC_NAME,
                message,
                metadata).block();
            System.out.println("Published message: " + message);
            TimeUnit.SECONDS.sleep(1);
        }
    }
}