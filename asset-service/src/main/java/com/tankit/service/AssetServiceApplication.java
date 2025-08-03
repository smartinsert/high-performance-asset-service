package com.tankit.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.tankit.service.data.AssetDataGenerator;
import com.tankit.service.model.Asset;
import com.tankit.service.repository.AssetRedisRepository;
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

    @Value("${app.instance-index}")
    private int instanceIdx;

    @Value("${app.total-instances}")
    private int totalInstances;

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
                populateCacheForInstance(instanceIdx, totalInstances, cachePopulateCount);
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

    /**
     * Populate local Caffeine cache with unique subset of assets,
     * partitioned by instanceIndex to avoid overlap across multiple service instances.
     *
     * @param instanceIndex zero-based index of current instance (0,1,2,...)
     * @param totalInstances total number of service instances (e.g., 3)
     * @param cachePopulateCount number of assets to cache per instance (e.g., 300)
     */
    public void populateCacheForInstance(int instanceIndex, int totalInstances, int cachePopulateCount) {
        logger.info("Populating cache partition for instance {} with {} assets...", instanceIndex, cachePopulateCount);

        try {
            Set<String> allAssetIds = redisRepository.getAllAssetIds();
            List<String> sortedAssetIds = allAssetIds.stream()
                    .sorted()
                    .collect(Collectors.toList());

            int totalAssets = sortedAssetIds.size();

            if (cachePopulateCount * totalInstances > totalAssets) {
                throw new IllegalStateException(String.format(
                        "Not enough assets (%d) in Redis to partition %d assets each for %d instances",
                        totalAssets, cachePopulateCount, totalInstances));
            }

            int fromIndex = instanceIndex * cachePopulateCount;
            int toIndex = Math.min(fromIndex + cachePopulateCount, totalAssets);

            List<String> partitionAssetIds = sortedAssetIds.subList(fromIndex, toIndex);

            for (String assetId : partitionAssetIds) {
                Asset asset = redisRepository.findAssetById(assetId);
                if (asset != null) {
                    assetCache.put(assetId, asset);
                }
            }

            logger.info("Instance {} cache populated with {} assets. Cache size: {}",
                    instanceIndex, partitionAssetIds.size(), assetCache.estimatedSize());

        } catch (Exception e) {
            logger.error("Error populating cache for instance " + instanceIndex, e);
        }
    }

}