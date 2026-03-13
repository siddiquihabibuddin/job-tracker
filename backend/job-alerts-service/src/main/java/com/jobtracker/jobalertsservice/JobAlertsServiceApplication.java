package com.jobtracker.jobalertsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobAlertsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobAlertsServiceApplication.class, args);
    }
}
