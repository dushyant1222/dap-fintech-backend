package com.dapfintech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DapFintechBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            DapFintechBackendApplication.class,args);
    }
}