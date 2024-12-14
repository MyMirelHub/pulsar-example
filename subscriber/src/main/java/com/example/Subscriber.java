package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;

@SpringBootApplication
@RestController
public class Subscriber {
    public static void main(String[] args) {
        // Set default port to 8082
        System.setProperty("server.port", "8082");
        SpringApplication.run(Subscriber.class, args);
        System.out.println("Subscriber  has started on port 8082. Listening for messages...");
    }

    @Topic(name = "messages", pubsubName = "pulsar-pubsub")
    @PostMapping("/messages")
    public void handleMessage(@RequestBody CloudEvent<String> message) {
        System.out.println("=== Message Received ===");
        System.out.println("ID: " + message.getId());
        System.out.println("Data: " + message.getData());
        System.out.println("=====================");
    }
}