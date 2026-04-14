package com.holaho.intern.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.holaho.intern.elasticsearch.repository")
public class ElasticsearchConfig {
}

