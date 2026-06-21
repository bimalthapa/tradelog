package com.tradelog.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root path to index.html for SPA
        registry.addViewController("/")
                .setViewName("forward:/index.html");
        // Forward single-segment extensionless paths: /dashboard, /analytics, etc.
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
        // Forward multi-segment extensionless paths: /campaign/123, /campaigns/new, etc.
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
