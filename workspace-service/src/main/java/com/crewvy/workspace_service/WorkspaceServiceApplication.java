package com.crewvy.workspace_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.crewvy.workspace_service", "com.crewvy.common"})
@EnableFeignClients
public class WorkspaceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkspaceServiceApplication.class, args);
	}

}
