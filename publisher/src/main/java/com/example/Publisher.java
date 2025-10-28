package com.example;

import java.net.InetAddress;
import java.util.Date;
import java.text.SimpleDateFormat; 
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
        System.out.println(getTimestamp() + "Pattern: Random bursts with random gaps");
        System.out.println(getTimestamp() + "==========================================\n");

        DaprClient client = new DaprClientBuilder().build();
        Random random = new Random();
        int messageCount = 1;

        while (true) {
            // Burst size: 1-10 messages
            int burstSize = random.nextInt(10) + 1;
            System.out.println(getTimestamp() + "[Burst of " + burstSize + " messages]");
            
            for (int i = 0; i < burstSize; i++) {
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
                
                // Variable delay within burst: 100ms to 3s
                int inBurstDelay = random.nextInt(2900) + 100;
                TimeUnit.MILLISECONDS.sleep(inBurstDelay);
            }
            
            int gapSeconds;
            if (random.nextInt(10) == 0) {
                gapSeconds = 60 + random.nextInt(121); // 60-180 seconds
                System.out.println(getTimestamp() + "[LONG idle for " + gapSeconds + "s (" + (gapSeconds/60) + " min)]\n");
            } else {
                gapSeconds = 5 + random.nextInt(41); // 5-45 seconds
                System.out.println(getTimestamp() + "[Idle for " + gapSeconds + "s]\n");
            }
            
            TimeUnit.SECONDS.sleep(gapSeconds);
        }
    }
}