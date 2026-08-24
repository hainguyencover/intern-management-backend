package com.holaho.intern.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.Validator;

@Configuration
public class WebConfig {
    @Autowired
    private MessageSource messageSource;

    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setValidationMessageSource(messageSource);
        return factoryBean;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.ShallowEtagHeaderFilter> filter =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new org.springframework.web.filter.ShallowEtagHeaderFilter());
        filter.addUrlPatterns("/api/v1/*");
        filter.setName("etagFilter");
        return filter;
    }

    /**
     * P-11: Enforce global pagination safety rules (Max size 100, default size 10)
     */
    @Bean
    public org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 10));
        };
    }
}

