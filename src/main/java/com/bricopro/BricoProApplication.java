package com.bricopro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   
public class BricoProApplication {
    public static void main(String[] args) {
        SpringApplication.run(BricoProApplication.class, args);
    }
}
