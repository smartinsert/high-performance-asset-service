package com.tankit.asset.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Asset service definition
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.53.0)",
    comments = "Source: asset-service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AssetServiceGrpc {

  private AssetServiceGrpc() {}

  public static final String SERVICE_NAME = "com.tankit.asset.AssetService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest,
      com.tankit.asset.proto.AssetResponse> getGetAssetsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAssets",
      requestType = com.tankit.asset.proto.AssetRequest.class,
      responseType = com.tankit.asset.proto.AssetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest,
      com.tankit.asset.proto.AssetResponse> getGetAssetsMethod() {
    io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest, com.tankit.asset.proto.AssetResponse> getGetAssetsMethod;
    if ((getGetAssetsMethod = AssetServiceGrpc.getGetAssetsMethod) == null) {
      synchronized (AssetServiceGrpc.class) {
        if ((getGetAssetsMethod = AssetServiceGrpc.getGetAssetsMethod) == null) {
          AssetServiceGrpc.getGetAssetsMethod = getGetAssetsMethod =
              io.grpc.MethodDescriptor.<com.tankit.asset.proto.AssetRequest, com.tankit.asset.proto.AssetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAssets"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.AssetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.AssetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AssetServiceMethodDescriptorSupplier("GetAssets"))
              .build();
        }
      }
    }
    return getGetAssetsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tankit.asset.proto.HealthCheckRequest,
      com.tankit.asset.proto.HealthCheckResponse> getCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Check",
      requestType = com.tankit.asset.proto.HealthCheckRequest.class,
      responseType = com.tankit.asset.proto.HealthCheckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tankit.asset.proto.HealthCheckRequest,
      com.tankit.asset.proto.HealthCheckResponse> getCheckMethod() {
    io.grpc.MethodDescriptor<com.tankit.asset.proto.HealthCheckRequest, com.tankit.asset.proto.HealthCheckResponse> getCheckMethod;
    if ((getCheckMethod = AssetServiceGrpc.getCheckMethod) == null) {
      synchronized (AssetServiceGrpc.class) {
        if ((getCheckMethod = AssetServiceGrpc.getCheckMethod) == null) {
          AssetServiceGrpc.getCheckMethod = getCheckMethod =
              io.grpc.MethodDescriptor.<com.tankit.asset.proto.HealthCheckRequest, com.tankit.asset.proto.HealthCheckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Check"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.HealthCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.HealthCheckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AssetServiceMethodDescriptorSupplier("Check"))
              .build();
        }
      }
    }
    return getCheckMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest,
      com.tankit.asset.proto.AssetResponse> getGetAssetsInternalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAssetsInternal",
      requestType = com.tankit.asset.proto.AssetRequest.class,
      responseType = com.tankit.asset.proto.AssetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest,
      com.tankit.asset.proto.AssetResponse> getGetAssetsInternalMethod() {
    io.grpc.MethodDescriptor<com.tankit.asset.proto.AssetRequest, com.tankit.asset.proto.AssetResponse> getGetAssetsInternalMethod;
    if ((getGetAssetsInternalMethod = AssetServiceGrpc.getGetAssetsInternalMethod) == null) {
      synchronized (AssetServiceGrpc.class) {
        if ((getGetAssetsInternalMethod = AssetServiceGrpc.getGetAssetsInternalMethod) == null) {
          AssetServiceGrpc.getGetAssetsInternalMethod = getGetAssetsInternalMethod =
              io.grpc.MethodDescriptor.<com.tankit.asset.proto.AssetRequest, com.tankit.asset.proto.AssetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAssetsInternal"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.AssetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tankit.asset.proto.AssetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AssetServiceMethodDescriptorSupplier("GetAssetsInternal"))
              .build();
        }
      }
    }
    return getGetAssetsInternalMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AssetServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AssetServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AssetServiceStub>() {
        @java.lang.Override
        public AssetServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AssetServiceStub(channel, callOptions);
        }
      };
    return AssetServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AssetServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AssetServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AssetServiceBlockingStub>() {
        @java.lang.Override
        public AssetServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AssetServiceBlockingStub(channel, callOptions);
        }
      };
    return AssetServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AssetServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AssetServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AssetServiceFutureStub>() {
        @java.lang.Override
        public AssetServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AssetServiceFutureStub(channel, callOptions);
        }
      };
    return AssetServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Asset service definition
   * </pre>
   */
  public static abstract class AssetServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Get assets by IDs with streaming response
     * </pre>
     */
    public void getAssets(com.tankit.asset.proto.AssetRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAssetsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Health check
     * </pre>
     */
    public void check(com.tankit.asset.proto.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckMethod(), responseObserver);
    }

    /**
     * <pre>
     * Asset inter-service communication
     * </pre>
     */
    public void getAssetsInternal(com.tankit.asset.proto.AssetRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAssetsInternalMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetAssetsMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                com.tankit.asset.proto.AssetRequest,
                com.tankit.asset.proto.AssetResponse>(
                  this, METHODID_GET_ASSETS)))
          .addMethod(
            getCheckMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tankit.asset.proto.HealthCheckRequest,
                com.tankit.asset.proto.HealthCheckResponse>(
                  this, METHODID_CHECK)))
          .addMethod(
            getGetAssetsInternalMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tankit.asset.proto.AssetRequest,
                com.tankit.asset.proto.AssetResponse>(
                  this, METHODID_GET_ASSETS_INTERNAL)))
          .build();
    }
  }

  /**
   * <pre>
   * Asset service definition
   * </pre>
   */
  public static final class AssetServiceStub extends io.grpc.stub.AbstractAsyncStub<AssetServiceStub> {
    private AssetServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AssetServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AssetServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get assets by IDs with streaming response
     * </pre>
     */
    public void getAssets(com.tankit.asset.proto.AssetRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetAssetsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Health check
     * </pre>
     */
    public void check(com.tankit.asset.proto.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Asset inter-service communication
     * </pre>
     */
    public void getAssetsInternal(com.tankit.asset.proto.AssetRequest request,
        io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAssetsInternalMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Asset service definition
   * </pre>
   */
  public static final class AssetServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<AssetServiceBlockingStub> {
    private AssetServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AssetServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AssetServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get assets by IDs with streaming response
     * </pre>
     */
    public java.util.Iterator<com.tankit.asset.proto.AssetResponse> getAssets(
        com.tankit.asset.proto.AssetRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetAssetsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Health check
     * </pre>
     */
    public com.tankit.asset.proto.HealthCheckResponse check(com.tankit.asset.proto.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Asset inter-service communication
     * </pre>
     */
    public com.tankit.asset.proto.AssetResponse getAssetsInternal(com.tankit.asset.proto.AssetRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAssetsInternalMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Asset service definition
   * </pre>
   */
  public static final class AssetServiceFutureStub extends io.grpc.stub.AbstractFutureStub<AssetServiceFutureStub> {
    private AssetServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AssetServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AssetServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Health check
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tankit.asset.proto.HealthCheckResponse> check(
        com.tankit.asset.proto.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Asset inter-service communication
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tankit.asset.proto.AssetResponse> getAssetsInternal(
        com.tankit.asset.proto.AssetRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAssetsInternalMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ASSETS = 0;
  private static final int METHODID_CHECK = 1;
  private static final int METHODID_GET_ASSETS_INTERNAL = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AssetServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AssetServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_ASSETS:
          serviceImpl.getAssets((com.tankit.asset.proto.AssetRequest) request,
              (io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse>) responseObserver);
          break;
        case METHODID_CHECK:
          serviceImpl.check((com.tankit.asset.proto.HealthCheckRequest) request,
              (io.grpc.stub.StreamObserver<com.tankit.asset.proto.HealthCheckResponse>) responseObserver);
          break;
        case METHODID_GET_ASSETS_INTERNAL:
          serviceImpl.getAssetsInternal((com.tankit.asset.proto.AssetRequest) request,
              (io.grpc.stub.StreamObserver<com.tankit.asset.proto.AssetResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class AssetServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AssetServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.tankit.asset.proto.AssetServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AssetService");
    }
  }

  private static final class AssetServiceFileDescriptorSupplier
      extends AssetServiceBaseDescriptorSupplier {
    AssetServiceFileDescriptorSupplier() {}
  }

  private static final class AssetServiceMethodDescriptorSupplier
      extends AssetServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AssetServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AssetServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AssetServiceFileDescriptorSupplier())
              .addMethod(getGetAssetsMethod())
              .addMethod(getCheckMethod())
              .addMethod(getGetAssetsInternalMethod())
              .build();
        }
      }
    }
    return result;
  }
}
