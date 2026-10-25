
package com.smartsem.smartsem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartsemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartsemApplication.class, args);
    }
}
