package com.zeezaglobal.gymmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymmanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymmanagementApplication.class, args);
	}

}
