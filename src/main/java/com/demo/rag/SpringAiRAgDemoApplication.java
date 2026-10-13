package com.demo.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. Starts the embedded web server on
 * http://localhost:8080. Spring AI auto-configures the ChatClient for us
 * based on application.properties — we never create a Bedrock client by hand.
 */
@SpringBootApplication
public class SpringAiRAgDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringAiRAgDemoApplication.class, args);
    }
}
