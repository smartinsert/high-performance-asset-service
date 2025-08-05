package com.tankit.client;

import com.tankit.asset.proto.AssetRequest;
import com.tankit.asset.proto.AssetResponse;
import com.tankit.asset.proto.AssetServiceGrpc;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    private void performAssetRequests() throws InterruptedException {
        final int numInstances = GRPC_PORTS.size();
        final int NUM_BATCHES = totalAssetCount / batchSize;

        ExecutorService executor = Executors.newFixedThreadPool(numInstances);

        List<String> assetIds = generateAssetIds(totalAssetCount);

        long startTime = System.currentTimeMillis();
        AtomicInteger totalAssetsReceived = new AtomicInteger(0);
        AtomicInteger totalBatchesProcessed = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_BATCHES; i++) {
            final int batchNum = i;
            futures.add(executor.submit(() -> {
                int serverIndex = batchNum % numInstances;
                int port = GRPC_PORTS.get(serverIndex);

                int startIdx = batchNum * batchSize;
                int endIdx = Math.min(startIdx + batchSize, totalAssetCount);
                List<String> batch = assetIds.subList(startIdx, endIdx);

//                logger.info("Requesting batch {} with {} assets on {}:{}", batchNum + 1, batch.size(),  GRPC_HOST, GRPC_PORTS.get(serverIndex));

                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(GRPC_HOST, GRPC_PORTS.get(serverIndex))
                        .usePlaintext()
                        .build();

                try {
                    AssetServiceGrpc.AssetServiceBlockingStub blockingStub =
                            AssetServiceGrpc.newBlockingStub(channel);

                    AssetRequest request = AssetRequest.newBuilder()
                            .addAllAssetIds(batch)
                            .setBatchSize(batchSize)
                            .build();

                    Iterator<AssetResponse> responseIterator = blockingStub.getAssets(request);

                    int assetsFound = 0;
                    while (responseIterator.hasNext()) {
                        AssetResponse resp = responseIterator.next();
                        assetsFound += resp.getTotalFound();
                        totalProcessingTime.addAndGet(resp.getProcessingTimeMs());
                    }

                    logger.info("Batch {} processed with {} assets found.", batchNum + 1, assetsFound);
                    totalAssetsReceived.addAndGet(assetsFound);
                    totalBatchesProcessed.incrementAndGet();

                } catch (Exception ex) {
                    logger.error("Error processing batch {}", batchNum + 1, ex);
                } finally {
                    channel.shutdown();
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                logger.error("Error in batch execution", e);
            }
        }

        executor.shutdown();
        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== PERFORMANCE RESULTS ===");
        logger.info("Total time: {} ms", elapsed);
        logger.info("Total assets requested: {}", totalAssetCount);
        logger.info("Total assets received: {}", totalAssetsReceived.get());
        logger.info("Total batches processed: {}", totalBatchesProcessed.get());
        logger.info("Average batch processing time: {} ms for batch size {}", (elapsed / NUM_BATCHES), batchSize);
        logger.info("Average processing time per asset {}ms", (1 / (totalAssetsReceived.get() * 1000.0 / elapsed)) * 1000);
        logger.info("Overall throughput: {} assets/sec", (totalAssetsReceived.get() * 1000.0 / elapsed));
        logger.info("Success rate: {}%", (totalAssetsReceived.get() * 100.0 / totalAssetCount));
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
