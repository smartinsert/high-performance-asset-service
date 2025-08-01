package com.tankit.client;

import com.tankit.asset.proto.AssetRequest;
import com.tankit.asset.proto.AssetResponse;
import com.tankit.asset.proto.AssetServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asset Client Application that requests assets from Asset Service
 */
@SpringBootApplication
public class AssetClientApplication {

    private static final Logger logger = LoggerFactory.getLogger(AssetClientApplication.class);

    @GrpcClient("asset-service")
    private AssetServiceGrpc.AssetServiceStub assetServiceStub;

    @GrpcClient("asset-service")
    private AssetServiceGrpc.AssetServiceBlockingStub assetServiceBlockingStub;

    public static void main(String[] args) {
        SpringApplication.run(AssetClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner runClient() {
        return args -> {
            logger.info("Starting Asset Client...");
            
            // Wait a bit for services to be ready
            Thread.sleep(5000);
            
            performAssetRequests();
        };
    }

    private void performAssetRequests() {
        logger.info("=== Starting Asset Client Performance Test ===");
        
        final int TOTAL_ASSETS = 3000;
        final int BATCH_SIZE = 1000;
        final int NUM_BATCHES = TOTAL_ASSETS / BATCH_SIZE;
        
        // Generate asset IDs to request
        List<String> assetIds = generateAssetIds(TOTAL_ASSETS);
        
        // Performance tracking
        AtomicLong totalProcessingTime = new AtomicLong(0);
        AtomicInteger totalAssetsReceived = new AtomicInteger(0);
        AtomicInteger totalBatchesReceived = new AtomicInteger(0);
        
        CountDownLatch latch = new CountDownLatch(NUM_BATCHES);
        
        long startTime = System.currentTimeMillis();
        
        // Split into batches and make streaming requests
        for (int i = 0; i < NUM_BATCHES; i++) {
            int startIdx = i * BATCH_SIZE;
            int endIdx = Math.min(startIdx + BATCH_SIZE, TOTAL_ASSETS);
            List<String> batch = assetIds.subList(startIdx, endIdx);
            
            logger.info("Requesting batch {} with {} assets", i + 1, batch.size());
            
            AssetRequest request = AssetRequest.newBuilder()
                .addAllAssetIds(batch)
                .setBatchSize(BATCH_SIZE)
                .build();
            
            // Make streaming request
            assetServiceStub.getAssets(request, new StreamObserver<AssetResponse>() {
                @Override
                public void onNext(AssetResponse response) {
                    logger.info("Received response: {} assets found out of {} requested from server {} (processing time: {} ms)",
                               response.getTotalFound(),
                               response.getTotalRequested(),
                               response.getServerInstance(),
                               response.getProcessingTimeMs());
                    
                    totalAssetsReceived.addAndGet(response.getTotalFound());
                    totalProcessingTime.addAndGet(response.getProcessingTimeMs());
                    totalBatchesReceived.incrementAndGet();
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.error("Error in streaming response: {}", t.getMessage());
                    latch.countDown();
                }
                
                @Override
                public void onCompleted() {
                    logger.debug("Batch streaming completed");
                    latch.countDown();
                }
            });
        }
        
        try {
            // Wait for all responses with timeout
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            if (completed) {
                logger.info("=== PERFORMANCE RESULTS ===");
                logger.info("Total time: {} ms", totalTime);
                logger.info("Total assets requested: {}", TOTAL_ASSETS);
                logger.info("Total assets received: {}", totalAssetsReceived.get());
                logger.info("Total batches processed: {}", totalBatchesReceived.get());
                logger.info("Average server processing time: {} ms", 
                           totalBatchesReceived.get() > 0 ? totalProcessingTime.get() / totalBatchesReceived.get() : 0);
                logger.info("Overall throughput: {} assets/second", 
                           totalTime > 0 ? (totalAssetsReceived.get() * 1000.0) / totalTime : 0);
                logger.info("Success rate: {}%", 
                           TOTAL_ASSETS > 0 ? (totalAssetsReceived.get() * 100.0) / TOTAL_ASSETS : 0);
            } else {
                logger.warn("Client requests timed out after 60 seconds");
            }
            
        } catch (InterruptedException e) {
            logger.error("Client interrupted", e);
            Thread.currentThread().interrupt();
        }
        
        // Keep application running for monitoring
        logger.info("Client test completed. Application will continue running for monitoring...");
        
        // Additional health check test
        performHealthCheck();
    }

    /**
     * Generate asset IDs for testing
     */
    private List<String> generateAssetIds(int count) {
        List<String> assetIds = new ArrayList<>();
        
        // Generate IDs that should exist in the system (ASSET_000001 to ASSET_100000)
        for (int i = 1; i <= count; i++) {
            // Spread requests across the asset range for better cache hit testing
            int assetNumber = (i * 33) % 100000 + 1; // Simple distribution
            assetIds.add(String.format("ASSET_%06d", assetNumber));
        }
        
        logger.info("Generated {} asset IDs for testing", assetIds.size());
        return assetIds;
    }

    private void performHealthCheck() {
        try {
            logger.info("Performing health check...");
            
            com.tankit.asset.proto.HealthCheckRequest healthRequest =
                com.tankit.asset.proto.HealthCheckRequest.newBuilder()
                    .setService("asset-service")
                    .build();
            
            com.tankit.asset.proto.HealthCheckResponse healthResponse =
                assetServiceBlockingStub.check(healthRequest);
            
            logger.info("Health check result: {}", healthResponse.getStatus());
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
        }
    }
}