package com.tradelog.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tradelog.price.PriceProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(PriceProperties props) {
        CaffeineCacheManager manager = new CaffeineCacheManager("prices");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(props.getCacheTtlMinutes()))
            .maximumSize(500));
        return manager;
    }
}
