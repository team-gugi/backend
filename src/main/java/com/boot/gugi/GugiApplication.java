package com.boot.gugi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GugiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GugiApplication.class, args);
	}

}
