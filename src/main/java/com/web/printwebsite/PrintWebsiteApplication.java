package com.web.printwebsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("com.web.model")
@ComponentScan(basePackages = {"com.web.*"})
@EnableJpaRepositories(basePackages = "com.web.repository")
@EnableScheduling
public class PrintWebsiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintWebsiteApplication.class, args);
    }

}
