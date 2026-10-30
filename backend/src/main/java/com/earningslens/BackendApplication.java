package com.earningslens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the EarningsLens backend.
 *
 * @SpringBootApplication turns on auto-configuration and scans this package
 * (com.earningslens) and its sub-packages for Spring components.
 * @ConfigurationPropertiesScan finds every @ConfigurationProperties record
 * (for example config.LlmProperties) and fills it from application.yaml.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
