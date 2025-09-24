package com.crewvy.member_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.crewvy.member_service", "crewvy.common"})
public class MemberServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MemberServiceApplication.class, args);
	}
}
