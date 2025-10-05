package com.helmx.tutorial;

import com.helmx.tutorial.utils.SpringBeanHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@SpringBootApplication
@Validated
public class ApplicationRun {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationRun.class, args);
	}

	@Bean
	public SpringBeanHolder springContextHolder() {
		return new SpringBeanHolder();
	}
}
