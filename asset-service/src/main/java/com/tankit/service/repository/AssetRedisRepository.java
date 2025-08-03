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
import java.util.*;
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

    /** Store a single asset in Redis as a Hash. */
    public void saveAssetAsHash(Asset asset) {
        try {
            String key = ASSET_KEY_PREFIX + asset.getAssetId();
            Map<String, String> hash = assetToMap(asset);
            commands.hset(key, hash);
            commands.sadd(ASSET_SET_KEY, asset.getAssetId());
            logger.debug("Saved asset (as hash): {}", asset.getAssetId());
        } catch (Exception e) {
            logger.error("Error saving asset as hash: {}", asset.getAssetId(), e);
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

    /** Batch save as hash. */
    public void saveAssetsAsHash(List<Asset> assets) {
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Asset asset : assets) {
                futures.add(CompletableFuture.runAsync(() -> saveAssetAsHash(asset), executorService));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            logger.info("Saved {} assets to Redis (hash)", assets.size());
        } catch (Exception e) {
            logger.error("Error saving assets in batch", e);
            throw new RuntimeException("Failed to save assets", e);
        }
    }

    /**
     * Retrieve single asset by Id
     */
    public Asset findHashedAssetById(String assetId) {
        try {
            String key = ASSET_KEY_PREFIX + assetId;
            Map<String, String> hash = commands.hgetall(key);
            if (hash != null && !hash.isEmpty()) {
                return mapToAsset(hash);
            }
            logger.debug("Asset not found: {}", assetId);
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving asset as hash: " + assetId, e);
            return null;
        }
    }

    /** Retrieve multiple assets. */
    public List<Asset> findHashedAssetsByIds(List<String> assetIds) {
        List<Asset> assets = new ArrayList<>();
        try {
            for (String assetId : assetIds) {
                Asset asset = findAssetById(assetId);
                if (asset != null) assets.add(asset);
            }
            logger.debug("Found {} out of {} requested assets", assets.size(), assetIds.size());
            return assets;
        } catch (Exception e) {
            logger.error("Error retrieving assets as hash", e);
            throw new RuntimeException("Failed to retrieve assets", e);
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

    private Map<String, String> assetToMap(Asset asset) {
        Map<String, String> m = new HashMap<>();
        m.put("assetId", asset.getAssetId());
        m.put("name", n(asset.getName()));
        m.put("description", n(asset.getDescription()));
        m.put("cusip", n(asset.getCusip()));
        m.put("bloombergId", n(asset.getBloombergId()));
        m.put("isin", n(asset.getIsin()));
        m.put("sedol", n(asset.getSedol()));
        m.put("currency", n(asset.getCurrency()));
        if (asset.getCreatedTimestamp() != null) m.put("createdTimestamp", String.valueOf(asset.getCreatedTimestamp().toEpochMilli()));
        if (asset.getMarketValue() != null) m.put("marketValue", String.valueOf(asset.getMarketValue()));
        return m;
    }

    private Asset mapToAsset(Map<String, String> map) {
        Asset a = new Asset();
        a.setAssetId(map.getOrDefault("assetId", ""));
        a.setName(map.getOrDefault("name", ""));
        a.setDescription(map.getOrDefault("description", ""));
        a.setCusip(map.getOrDefault("cusip", ""));
        a.setBloombergId(map.getOrDefault("bloombergId", ""));
        a.setIsin(map.getOrDefault("isin", ""));
        a.setSedol(map.getOrDefault("sedol", ""));
        a.setCurrency(map.getOrDefault("currency", ""));
        if (map.containsKey("createdTimestamp")) {
            try {
                a.setCreatedTimestamp(java.time.Instant.ofEpochMilli(Long.parseLong(map.get("createdTimestamp"))));
            } catch (Exception ignored) {}
        }
        if (map.containsKey("marketValue")) {
            try {
                a.setMarketValue(Double.parseDouble(map.get("marketValue")));
            } catch (Exception ignored) {}
        }
        return a;
    }

    private String n(String s) { return s == null ? "" : s; }
}