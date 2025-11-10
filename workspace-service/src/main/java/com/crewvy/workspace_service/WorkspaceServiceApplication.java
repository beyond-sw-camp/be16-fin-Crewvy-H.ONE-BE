package com.crewvy.workspace_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.crewvy.workspace_service", "com.crewvy.common"})
@EnableFeignClients
@EnableScheduling
@EnableAsync
public class WorkspaceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkspaceServiceApplication.class, args);
	}

}
