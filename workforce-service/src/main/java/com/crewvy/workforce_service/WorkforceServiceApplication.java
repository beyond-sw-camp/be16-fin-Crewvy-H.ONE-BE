package com.crewvy.workforce_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.crewvy.workforce_service", "com.crewvy.common"})
public class WorkforceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkforceServiceApplication.class, args);
	}

}
