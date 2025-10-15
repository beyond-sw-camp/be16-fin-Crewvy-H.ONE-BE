package com.crewvy.workforce_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.crewvy.workforce_service", "com.crewvy.common"})
@EnableFeignClients
public class WorkforceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkforceServiceApplication.class, args);
	}

}
