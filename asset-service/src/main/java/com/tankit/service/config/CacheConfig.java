package com.tankit.service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.tankit.service.model.Asset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Caffeine cache
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.asset.max-size:30000}")
    private int maxCacheSize;

    @Value("${cache.asset.expire-after-access-minutes:30}")
    private int expireAfterAccessMinutes;

    @Value("${cache.asset.expire-after-write-minutes:60}")
    private int expireAfterWriteMinutes;

    /**
     * Configure Caffeine cache for assets
     */
    @Bean
    public Cache<String, Asset> assetCache() {
        return Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats() // Enable statistics for monitoring
                .build();
    }

    /**
     * Configure CacheManager for Spring Cache annotations
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("assets", "asset-batches");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache builder with configuration
     */
    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats();
    }
}