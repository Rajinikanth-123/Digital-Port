package com.portshipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigitalPortShippingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalPortShippingApplication.class, args);
    }
}
