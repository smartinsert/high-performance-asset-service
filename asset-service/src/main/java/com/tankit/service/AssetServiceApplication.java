package com.tankit.service;

import com.tankit.service.data.AssetDataGenerator;
import com.tankit.service.model.Asset;
import com.tankit.service.repository.AssetRedisRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Boot application for Asset Service
 */
@SpringBootApplication
@EnableAsync
public class AssetServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(AssetServiceApplication.class);

    @Value("${app.initialize-data:true}")
    private boolean initializeData;

    @Value("${app.asset-count:100000}")
    private int totalAssetCount;

    @Value("${app.cache-populate-count:30000}")
    private int cachePopulateCount;

    @Autowired
    private AssetDataGenerator dataGenerator;

    @Autowired
    private AssetRedisRepository redisRepository;

    @Autowired
    private Cache<String, Asset> assetCache;

    public static void main(String[] args) {
        // JVM arguments for profiling and memory optimization
        System.setProperty("java.awt.headless", "true");

        SpringApplication app = new SpringApplication(AssetServiceApplication.class);
        app.run(args);
    }

    /**
     * Initialize data if required
     */
    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            if (initializeData) {
                initializeAssetData();
                populateCache();
            }
        };
    }

    private void initializeAssetData() {
        logger.info("Checking if Redis needs to be initialized...");

        long existingCount = redisRepository.getTotalAssetCount();
        logger.info("Found {} existing assets in Redis", existingCount);

        if (existingCount < totalAssetCount) {
            logger.info("Generating {} assets...", totalAssetCount);
            long startTime = System.currentTimeMillis();

            List<Asset> assets = dataGenerator.generateAssets(totalAssetCount);

            logger.info("Saving {} assets to Redis...", assets.size());
            redisRepository.saveAssets(assets);

            long endTime = System.currentTimeMillis();
            logger.info("Completed data initialization in {} ms", endTime - startTime);
        } else {
            logger.info("Redis already contains {} assets, skipping initialization", existingCount);
        }
    }

    private void populateCache() {
        logger.info("Populating cache with {} assets...", cachePopulateCount);

        try {
            Set<String> allAssetIds = redisRepository.getAllAssetIds();
            List<String> assetIdsList = allAssetIds.stream()
                    .limit(cachePopulateCount)
                    .collect(Collectors.toList());

            // Randomly select assets to populate cache for better distribution
            Random random = new Random();
            for (int i = 0; i < Math.min(cachePopulateCount, assetIdsList.size()); i++) {
                int randomIndex = random.nextInt(assetIdsList.size());
                String assetId = assetIdsList.get(randomIndex);

                Asset asset = redisRepository.findAssetById(assetId);
                if (asset != null) {
                    assetCache.put(assetId, asset);
                }

                // Remove to avoid duplicates
                assetIdsList.remove(randomIndex);
            }

            logger.info("Cache populated with {} assets. Cache size: {}",
                    cachePopulateCount, assetCache.estimatedSize());

        } catch (Exception e) {
            logger.error("Error populating cache", e);
        }
    }
}