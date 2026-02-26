package com.jobtracker.apps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.jobtracker.apps")
public class ApplicationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationsServiceApplication.class, args);
	}

}
