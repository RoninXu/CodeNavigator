package com.codenavigator.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.codenavigator")
@EntityScan(basePackages = "com.codenavigator.core.entity")
@EnableJpaRepositories(basePackages = "com.codenavigator.core.repository")
public class CodeNavigatorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CodeNavigatorApplication.class, args);
    }
}