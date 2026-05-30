package com.leoraggy.centroid_finder;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CentroidFinderApplication {

    public static void main(String[] args) {
        // 1. Load the .env file from the root directory
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Prevents crashes if deployed to cloud environments later
                .load();

        // 2. Inject the .env properties directly into Java's system properties
        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );

        // 3. Now start Spring Boot safely
        SpringApplication.run(CentroidFinderApplication.class, args);
    }
}