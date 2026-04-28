package com.synapsecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
@EnableAsync
public class SynapseCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseCoreApplication.class, args);
    }
}
