package io.digital.patterns.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class IdentityServiceApiApplication {

    public static void main (String[] args) {
        log.info("Starting identity service...");
        SpringApplication.run(IdentityServiceApiApplication.class, args);
    }
}
