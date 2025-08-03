package com.tankit.service.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.tankit.asset.proto.AssetRequest;
import com.tankit.asset.proto.AssetResponse;
import com.tankit.asset.proto.AssetServiceGrpc;
import com.tankit.service.model.Asset;
import com.tankit.service.repository.AssetRedisRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class AssetGrpcService extends AssetServiceGrpc.AssetServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AssetGrpcService.class);

    @Autowired
    private AssetRedisRepository redisRepository;

    @Autowired
    private Cache<String, Asset> assetCache;

    @Value("${server.port:9090}")
    private String serverPort;

    @Override
    public void getAssets(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
        logger.info("Received asset request for {} assets", request.getAssetIdsCount());

        long startTime = System.currentTimeMillis();
        List<Asset> foundAssets = new ArrayList<>();
        int totalRequested = request.getAssetIdsCount();

        for (String assetId : request.getAssetIdsList()) {
            Asset asset = assetCache.getIfPresent(assetId);

            logger.info("Found {} in caffeine", assetId);

            if (asset == null) {
                asset = redisRepository.findAssetById(assetId);
                logger.info("Asset {} not found in local cache, Querying redis", assetId);
                if (asset != null) {
                    logger.info("Received from Redis {}", asset.getAssetId());
                    assetCache.put(assetId, asset);
                }
            }

            if (asset != null) {
                foundAssets.add(asset);

                AssetResponse response = AssetResponse.newBuilder()
                        .addAssets(convertToProtoAsset(asset))
                        .setTotalFound(1)
                        .setTotalRequested(1)
                        .setServerInstance("port-" + serverPort)
                        .setProcessingTimeMs(System.currentTimeMillis() - startTime)
                        .build();

                responseObserver.onNext(response);
            }
        }

        // If no asset was found, send a single empty response!
        if (foundAssets.isEmpty()) {
            responseObserver.onNext(
                    AssetResponse.newBuilder()
                            .setTotalFound(0)
                            .setTotalRequested(totalRequested)
                            .setServerInstance("port-" + serverPort)
                            .setProcessingTimeMs(System.currentTimeMillis() - startTime)
                            .build()
            );
        }

        responseObserver.onCompleted();
        logger.info("Completed asset request for {} assets. Found: {}", totalRequested, foundAssets.size());
    }

    @Override
    public void getAssetsInternal(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
        getAssets(request, responseObserver); // Direct call
    }

    private com.tankit.asset.proto.Asset convertToProtoAsset(Asset asset) {
        return com.tankit.asset.proto.Asset.newBuilder()
                .setAssetId(asset.getAssetId())
                .setName(asset.getName() == null ? "" : asset.getName())
                .setDescription(asset.getDescription() == null ? "" : asset.getDescription())
                .setCusip(asset.getCusip() == null ? "" : asset.getCusip())
                .setBloombergId(asset.getBloombergId() == null ? "" : asset.getBloombergId())
                .setIsin(asset.getIsin() == null ? "" : asset.getIsin())
                .setSedol(asset.getSedol() == null ? "" : asset.getSedol())
                .setCreatedTimestamp(asset.getCreatedTimestamp() != null ? asset.getCreatedTimestamp().toEpochMilli() : 0)
                .setMarketValue(asset.getMarketValue() != null ? asset.getMarketValue() : 0.0)
                .setCurrency(asset.getCurrency() == null ? "" : asset.getCurrency())
                .build();
    }
}
