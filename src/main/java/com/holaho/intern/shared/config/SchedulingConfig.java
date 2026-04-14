package com.holaho.intern.shared.config;

import com.holaho.intern.service.BackupService;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    // This enables @Scheduled annotations in BackupService
}

