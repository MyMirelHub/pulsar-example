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
        System.setProperty("server.port", "8082");
        SpringApplication.run(Subscriber.class, args);
        System.out.println("Subscriber started on port 8082. Listening for ordered messages...");
    }

    @Topic(name = "messages", pubsubName = "pulsar-pubsub", metadata = "{\"subscribeType\": \"key_shared\"}")
    @PostMapping("/messages")
    public void handleMessage(@RequestBody CloudEvent<String> message) {
    //@RequestMapping Map<String, String> headers
        System.out.println("Processed message: " + message.getData());
    }
}
