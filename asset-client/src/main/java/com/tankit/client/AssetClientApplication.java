package com.tankit.client;

import com.tankit.asset.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
public class AssetClientApplication {

    private static final Logger logger = LoggerFactory.getLogger(AssetClientApplication.class);

    // List just the gRPC ports separately for clarity
    private static final List<Integer> GRPC_PORTS = Arrays.asList(9090, 9091, 9092);
    private static final String GRPC_HOST = "localhost";
    @Value("${app.asset-count:100000}")
    private int totalAssetCount;

    @Value("${app.batch-size:1000}")
    private int batchSize;

    public static void main(String[] args) {
        SpringApplication.run(AssetClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner runClient() {
        return args -> {
            logger.info("Starting Asset Client using round-robin channel creation...");

            // Wait a bit for services to be ready
            Thread.sleep(3000);

            performAssetRequests();

            logger.info("Client test completed.");
        };
    }

    private void performAssetRequests() {
        final int NUM_BATCHES = totalAssetCount / batchSize;

        List<String> assetIds = generateAssetIds(totalAssetCount);

        long startTime = System.currentTimeMillis();
        int totalAssetsReceived = 0;
        int totalBatchesProcessed = 0;
        long totalProcessingTime = 0;

        for (int i = 0; i < NUM_BATCHES; i++) {
            int port = GRPC_PORTS.get(i % GRPC_PORTS.size());
            int startIdx = i * batchSize;
            int endIdx = Math.min(startIdx + batchSize, totalAssetCount);
            List<String> batch = assetIds.subList(startIdx, endIdx);

            logger.info("Requesting batch {} with {} assets on {}:{}", i + 1, batch.size(), GRPC_HOST, port);

            // Each batch opens and closes its own channel
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(GRPC_HOST, port)
                    .usePlaintext()
                    .build();

            try {
                AssetServiceGrpc.AssetServiceBlockingStub blockingStub =
                        AssetServiceGrpc.newBlockingStub(channel);

                AssetRequest request = AssetRequest.newBuilder()
                        .addAllAssetIds(batch)
                        .setBatchSize(batchSize)
                        .build();

                int assetsFoundInBatch = 0;
                long batchStart = System.currentTimeMillis();

                // Streaming response (blocking)
                Iterator<AssetResponse> iterator = blockingStub.getAssets(request);

                while (iterator.hasNext()) {
                    AssetResponse response = iterator.next();

                    int found = response.getTotalFound();
                    assetsFoundInBatch += found;

                    logger.info("Batch {} streaming response: {} assets found ({} requested), server: {}, server time: {}ms",
                            i + 1, found, response.getTotalRequested(), response.getServerInstance(), response.getProcessingTimeMs());
                }
                logger.info("Batch {} complete. Total found: {}", i + 1, assetsFoundInBatch);
                totalAssetsReceived += assetsFoundInBatch;
                totalBatchesProcessed++;
            } catch (Exception e) {
                logger.error("Some error occurred: ", e);
            } finally {
                channel.shutdown();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        logger.info("=== PERFORMANCE RESULTS ===");
        logger.info("Total time: {} ms", elapsed);
        logger.info("Total assets requested: {}", totalAssetCount);
        logger.info("Total assets received: {}", totalAssetsReceived);
        logger.info("Total batches processed: {}", totalBatchesProcessed);
        logger.info("Average batch processing time: {} ms for batch size {}",
                totalBatchesProcessed > 0 ? elapsed / totalBatchesProcessed : 0, batchSize);
        logger.info("Overall throughput: {} assets/sec",
                elapsed > 0 ? (totalAssetsReceived * 1000.0) / elapsed : 0);
        logger.info("Success rate: {}%",
                totalAssetsReceived * 100.0 / totalAssetCount);
    }

    private List<String> generateAssetIds(int count) {
        List<String> assetIds = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            int assetNumber = (i * 33) % totalAssetCount + 1;
            assetIds.add(String.format("ASSET_%06d", assetNumber));
        }
        logger.info("Generated {} asset IDs for testing", assetIds.size());
        return assetIds;
    }
}
