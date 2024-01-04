package com.pro.mybooklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MybooklistApplication {
	public static void main(String[] args) {
		SpringApplication.run(MybooklistApplication.class, args);
	}
}
