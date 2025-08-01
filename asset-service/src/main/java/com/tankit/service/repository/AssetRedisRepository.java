package com.tankit.service.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tankit.service.model.Asset;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis-based repository for Asset storage and retrieval
 */
@Repository
public class AssetRedisRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssetRedisRepository.class);
    private static final String ASSET_KEY_PREFIX = "asset:";
    private static final String ASSET_SET_KEY = "assets:all";

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> commands;
    private ObjectMapper objectMapper;
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        String redisUri = String.format("redis://%s:%d", redisHost, redisPort);
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        commands = connection.sync();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        executorService = Executors.newFixedThreadPool(4);

        logger.info("Connected to Redis at {}:{}", redisHost, redisPort);
    }

    @PreDestroy
    public void cleanup() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Store a single asset in Redis
     */
    public void saveAsset(Asset asset) {
        try {
            String key = ASSET_KEY_PREFIX + asset.getAssetId();
            String value = objectMapper.writeValueAsString(asset);

            commands.set(key, value);
            commands.sadd(ASSET_SET_KEY, asset.getAssetId());

            logger.debug("Saved asset: {}", asset.getAssetId());
        } catch (Exception e) {
            logger.error("Error saving asset: {}", asset.getAssetId(), e);
            throw new RuntimeException("Failed to save asset", e);
        }
    }

    /**
     * Store multiple assets in Redis using pipeline for better performance
     */
    public void saveAssets(List<Asset> assets) {
        try {
            // Use async operations for better performance
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Asset asset : assets) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    saveAsset(asset);
                }, executorService);
                futures.add(future);
            }

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            logger.info("Saved {} assets to Redis", assets.size());
        } catch (Exception e) {
            logger.error("Error saving assets in batch", e);
            throw new RuntimeException("Failed to save assets", e);
        }
    }

    /**
     * Retrieve a single asset by ID
     */
    public Asset findAssetById(String assetId) {
        try {
            String key = ASSET_KEY_PREFIX + assetId;
            String value = commands.get(key);

            if (value != null) {
                Asset asset = objectMapper.readValue(value, Asset.class);
                logger.debug("Found asset: {}", assetId);
                return asset;
            }

            logger.debug("Asset not found: {}", assetId);
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving asset: {}", assetId, e);
            return null;
        }
    }

    /**
     * Retrieve multiple assets by IDs
     */
    public List<Asset> findAssetsByIds(List<String> assetIds) {
        List<Asset> assets = new ArrayList<>();

        try {
            for (String assetId : assetIds) {
                Asset asset = findAssetById(assetId);
                if (asset != null) {
                    assets.add(asset);
                }
            }

            logger.debug("Found {} out of {} requested assets", assets.size(), assetIds.size());
            return assets;
        } catch (Exception e) {
            logger.error("Error retrieving assets", e);
            throw new RuntimeException("Failed to retrieve assets", e);
        }
    }

    /**
     * Get total number of assets in Redis
     */
    public long getTotalAssetCount() {
        try {
            return commands.scard(ASSET_SET_KEY);
        } catch (Exception e) {
            logger.error("Error getting asset count", e);
            return 0;
        }
    }

    /**
     * Get all asset IDs (for testing purposes - use carefully with large datasets)
     */
    public Set<String> getAllAssetIds() {
        try {
            return commands.smembers(ASSET_SET_KEY);
        } catch (Exception e) {
            logger.error("Error getting all asset IDs", e);
            throw new RuntimeException("Failed to get asset IDs", e);
        }
    }

    /**
     * Check if Redis connection is healthy
     */
    public boolean isHealthy() {
        try {
            String response = commands.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return false;
        }
    }

    /**
     * Clear all assets (for testing purposes)
     */
    public void clearAllAssets() {
        try {
            Set<String> assetIds = getAllAssetIds();
            for (String assetId : assetIds) {
                commands.del(ASSET_KEY_PREFIX + assetId);
            }
            commands.del(ASSET_SET_KEY);

            logger.info("Cleared all assets from Redis");
        } catch (Exception e) {
            logger.error("Error clearing assets", e);
            throw new RuntimeException("Failed to clear assets", e);
        }
    }
}