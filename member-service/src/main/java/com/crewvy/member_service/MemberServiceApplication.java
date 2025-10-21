package com.crewvy.member_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.crewvy.member_service", "com.crewvy.common"})
public class MemberServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MemberServiceApplication.class, args);
	}
}
