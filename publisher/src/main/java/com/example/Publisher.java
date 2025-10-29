package com.example;

import java.net.InetAddress;
import java.util.Date;
import java.text.SimpleDateFormat; 
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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String getTimestamp() {
        return "[" + DATE_FORMAT.format(new Date()) + "] ";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Publisher.class, args);        

        String hostname = InetAddress.getLocalHost().getHostName();
        System.out.println(getTimestamp() + "Publisher started on pod: " + hostname);
        System.out.println(getTimestamp() + "Pattern: Continuous messages every 2 seconds");
        System.out.println(getTimestamp() + "==========================================\n");

        DaprClient client = new DaprClientBuilder().build();
        int messageCount = 1;

        while (true) {
            String partitionKey = "authoring-event-topic";
            String message = String.format("Message #%d (p-Key: %s)", messageCount, partitionKey); 
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("partitionKey", partitionKey);

            long start = System.currentTimeMillis();
            try {
                client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message, metadata).block();
                long duration = System.currentTimeMillis() - start;
                System.out.println(getTimestamp() + 
                    String.format("✓ Message %d sent (%.1fs)", messageCount, duration/1000.0));
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                System.out.println(getTimestamp() + 
                    String.format("✗ Message %d FAILED (%.1fs): %s", 
                    messageCount, duration/1000.0, e.getMessage()));
            }
            
            messageCount++;
            TimeUnit.SECONDS.sleep(2);
        }
    }
}