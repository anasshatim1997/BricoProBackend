package com.bricopro.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        return manager;
    }

@Bean("workerPerformanceCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> workerPerformanceCache() {
        return Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    @Bean("leaderboardCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> leaderboardCache() {
        return Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    @Bean("serviceCategoriesCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> serviceCategoriesCache() {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }
}
