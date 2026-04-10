package com.synapsecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SynapseCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseCoreApplication.class, args);
    }
}
