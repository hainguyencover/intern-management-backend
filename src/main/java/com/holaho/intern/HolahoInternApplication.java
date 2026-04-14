package com.holaho.intern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.cache.annotation.EnableCaching
public class HolahoInternApplication {

    public static void main(String[] args) {
        SpringApplication.run(HolahoInternApplication.class, args);
    }

}


