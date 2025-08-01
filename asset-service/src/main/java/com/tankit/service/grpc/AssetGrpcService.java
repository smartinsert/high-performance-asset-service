package com.tankit.service.grpc;

import com.tankit.asset.proto.*;
import com.tankit.service.model.Asset;
import com.tankit.service.repository.AssetRedisRepository;
import com.github.benmanes.caffeine.cache.Cache;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for Asset operations
 */
@GrpcService
public class AssetGrpcService extends AssetServiceGrpc.AssetServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AssetGrpcService.class);

    @Autowired
    private AssetRedisRepository redisRepository;

    @Autowired
    private Cache<String, Asset> assetCache;

    @Autowired
    private ThreadPoolExecutor assetProcessingExecutor;

    @Value("${server.port:9090}")
    private String serverPort;

    @Value("${grpc.client.asset-service-1.address:localhost:9091}")
    private String service1Address;

    @Value("${grpc.client.asset-service-2.address:localhost:9092}")
    private String service2Address;

    @GrpcClient("asset-service-1")
    private AssetServiceGrpc.AssetServiceBlockingStub assetService1Client;

    @GrpcClient("asset-service-2")
    private AssetServiceGrpc.AssetServiceBlockingStub assetService2Client;

    @Override
    public void getAssets(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
        logger.info("Received asset request for {} assets with batch size {}", 
                   request.getAssetIdsCount(), request.getBatchSize());

        long startTime = System.currentTimeMillis();
        List<String> assetIds = request.getAssetIdsList();
        int batchSize = request.getBatchSize() > 0 ? request.getBatchSize() : 1000;

        try {
            // Process assets in batches using thread pool
            List<CompletableFuture<AssetResponse>> futures = new ArrayList<>();

            for (int i = 0; i < assetIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, assetIds.size());
                List<String> batch = assetIds.subList(i, endIndex);

                CompletableFuture<AssetResponse> future = CompletableFuture
                    .supplyAsync(() -> processBatch(batch), assetProcessingExecutor);
                futures.add(future);
            }

            // Stream responses as they become available
            for (CompletableFuture<AssetResponse> future : futures) {
                AssetResponse response = future.get();
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();
            
            long endTime = System.currentTimeMillis();
            logger.info("Completed asset request in {} ms", endTime - startTime);

        } catch (Exception e) {
            logger.error("Error processing asset request", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to process asset request: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getAssetsInternal(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
        logger.debug("Received internal asset request for {} assets", request.getAssetIdsCount());

        try {
            AssetResponse response = processBatch(request.getAssetIdsList());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing internal asset request", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to process internal request")
                .asRuntimeException());
        }
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.debug("Health check requested for service: {}", request.getService());

        try {
            // Check Redis connectivity
            boolean redisHealthy = redisRepository.isHealthy();
            
            // Check cache stats
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = assetCache.stats();
            boolean cacheHealthy = stats != null;

            HealthCheckResponse.ServingStatus status = (redisHealthy && cacheHealthy) 
                ? HealthCheckResponse.ServingStatus.SERVING 
                : HealthCheckResponse.ServingStatus.NOT_SERVING;

            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(status)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Health check failed", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Health check failed")
                .asRuntimeException());
        }
    }

    private AssetResponse processBatch(List<String> assetIds) {
        long startTime = System.currentTimeMillis();
        List<Asset> foundAssets = new ArrayList<>();
        List<String> notFoundInCache = new ArrayList<>();

        // Step 1: Check local Caffeine cache
        for (String assetId : assetIds) {
            Asset cachedAsset = assetCache.getIfPresent(assetId);
            if (cachedAsset != null) {
                foundAssets.add(cachedAsset);
                logger.debug("Asset {} found in local cache", assetId);
            } else {
                notFoundInCache.add(assetId);
            }
        }

        // Step 2: Check other asset service instances if not found in local cache
        if (!notFoundInCache.isEmpty()) {
            List<String> stillNotFound = checkOtherServices(notFoundInCache, foundAssets);
            
            // Step 3: Check Redis for remaining assets
            if (!stillNotFound.isEmpty()) {
                List<Asset> redisAssets = redisRepository.findAssetsByIds(stillNotFound);
                foundAssets.addAll(redisAssets);

                // Cache the assets found in Redis
                for (Asset asset : redisAssets) {
                    assetCache.put(asset.getAssetId(), asset);
                }
            }
        }

        long endTime = System.currentTimeMillis();

        // Convert to protobuf assets
        List<com.tankit.asset.proto.Asset> protoAssets = foundAssets.stream()
            .map(this::convertToProtoAsset)
            .collect(Collectors.toList());

        return AssetResponse.newBuilder()
            .addAllAssets(protoAssets)
            .setTotalFound(foundAssets.size())
            .setTotalRequested(assetIds.size())
            .setServerInstance("port-" + serverPort)
            .setProcessingTimeMs(endTime - startTime)
            .build();
    }

    private List<String> checkOtherServices(List<String> assetIds, List<Asset> foundAssets) {
        List<String> stillNotFound = new ArrayList<>(assetIds);

        try {
            // Check service 1
            if (assetService1Client != null && !stillNotFound.isEmpty()) {
                AssetRequest request = AssetRequest.newBuilder()
                    .addAllAssetIds(stillNotFound)
                    .setBatchSize(stillNotFound.size())
                    .build();

                AssetResponse response = assetService1Client.getAssetsInternal(request);
                List<Asset> service1Assets = response.getAssetsList().stream()
                    .map(this::convertFromProtoAsset)
                    .collect(Collectors.toList());

                foundAssets.addAll(service1Assets);
                
                // Remove found assets from still not found list
                List<String> foundIds = service1Assets.stream()
                    .map(Asset::getAssetId)
                    .collect(Collectors.toList());
                stillNotFound.removeAll(foundIds);

                // Cache assets from other service
                for (Asset asset : service1Assets) {
                    assetCache.put(asset.getAssetId(), asset);
                }

                logger.debug("Found {} assets from service 1", service1Assets.size());
            }

            // Check service 2
            if (assetService2Client != null && !stillNotFound.isEmpty()) {
                AssetRequest request = AssetRequest.newBuilder()
                    .addAllAssetIds(stillNotFound)
                    .setBatchSize(stillNotFound.size())
                    .build();

                AssetResponse response = assetService2Client.getAssetsInternal(request);
                List<Asset> service2Assets = response.getAssetsList().stream()
                    .map(this::convertFromProtoAsset)
                    .collect(Collectors.toList());

                foundAssets.addAll(service2Assets);
                
                // Remove found assets from still not found list
                List<String> foundIds = service2Assets.stream()
                    .map(Asset::getAssetId)
                    .collect(Collectors.toList());
                stillNotFound.removeAll(foundIds);

                // Cache assets from other service
                for (Asset asset : service2Assets) {
                    assetCache.put(asset.getAssetId(), asset);
                }

                logger.debug("Found {} assets from service 2", service2Assets.size());
            }

        } catch (Exception e) {
            logger.warn("Error checking other services: {}", e.getMessage());
        }

        return stillNotFound;
    }

    private com.tankit.asset.proto.Asset convertToProtoAsset(Asset asset) {
        return com.tankit.asset.proto.Asset.newBuilder()
            .setAssetId(asset.getAssetId())
            .setName(asset.getName())
            .setDescription(asset.getDescription())
            .setCusip(asset.getCusip())
            .setBloombergId(asset.getBloombergId())
            .setIsin(asset.getIsin() != null ? asset.getIsin() : "")
            .setSedol(asset.getSedol() != null ? asset.getSedol() : "")
            .setCreatedTimestamp(asset.getCreatedTimestamp().toEpochMilli())
            .setMarketValue(asset.getMarketValue() != null ? asset.getMarketValue() : 0.0)
            .setCurrency(asset.getCurrency())
            .build();
    }

    private Asset convertFromProtoAsset(com.tankit.asset.proto.Asset protoAsset) {
        Asset asset = new Asset();
        asset.setAssetId(protoAsset.getAssetId());
        asset.setName(protoAsset.getName());
        asset.setDescription(protoAsset.getDescription());
        asset.setCusip(protoAsset.getCusip());
        asset.setBloombergId(protoAsset.getBloombergId());
        asset.setIsin(protoAsset.getIsin());
        asset.setSedol(protoAsset.getSedol());
        asset.setCreatedTimestamp(Instant.ofEpochMilli(protoAsset.getCreatedTimestamp()));
        asset.setMarketValue(protoAsset.getMarketValue());
        asset.setCurrency(protoAsset.getCurrency());
        return asset;
    }
}