package com.wso2persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RulesPersistenceEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulesPersistenceEngineApplication.class, args);
    }

}
