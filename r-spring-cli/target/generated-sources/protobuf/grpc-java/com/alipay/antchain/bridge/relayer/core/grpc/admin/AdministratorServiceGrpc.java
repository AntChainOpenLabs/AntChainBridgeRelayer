package com.alipay.antchain.bridge.relayer.core.grpc.admin;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The greeting service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.2)",
    comments = "Source: admingrpc.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AdministratorServiceGrpc {

  private AdministratorServiceGrpc() {}

  public static final String SERVICE_NAME = "acb.relayer.admin.AdministratorService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest,
      com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> getAdminRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "adminRequest",
      requestType = com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest.class,
      responseType = com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest,
      com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> getAdminRequestMethod() {
    io.grpc.MethodDescriptor<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest, com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> getAdminRequestMethod;
    if ((getAdminRequestMethod = AdministratorServiceGrpc.getAdminRequestMethod) == null) {
      synchronized (AdministratorServiceGrpc.class) {
        if ((getAdminRequestMethod = AdministratorServiceGrpc.getAdminRequestMethod) == null) {
          AdministratorServiceGrpc.getAdminRequestMethod = getAdminRequestMethod =
              io.grpc.MethodDescriptor.<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest, com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "adminRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AdministratorServiceMethodDescriptorSupplier("adminRequest"))
              .build();
        }
      }
    }
    return getAdminRequestMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AdministratorServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceStub>() {
        @java.lang.Override
        public AdministratorServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AdministratorServiceStub(channel, callOptions);
        }
      };
    return AdministratorServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AdministratorServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceBlockingStub>() {
        @java.lang.Override
        public AdministratorServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AdministratorServiceBlockingStub(channel, callOptions);
        }
      };
    return AdministratorServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AdministratorServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AdministratorServiceFutureStub>() {
        @java.lang.Override
        public AdministratorServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AdministratorServiceFutureStub(channel, callOptions);
        }
      };
    return AdministratorServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static abstract class AdministratorServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * 管控请求
     * </pre>
     */
    public void adminRequest(com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest request,
        io.grpc.stub.StreamObserver<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAdminRequestMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getAdminRequestMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest,
                com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse>(
                  this, METHODID_ADMIN_REQUEST)))
          .build();
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class AdministratorServiceStub extends io.grpc.stub.AbstractAsyncStub<AdministratorServiceStub> {
    private AdministratorServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AdministratorServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AdministratorServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 管控请求
     * </pre>
     */
    public void adminRequest(com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest request,
        io.grpc.stub.StreamObserver<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAdminRequestMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class AdministratorServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<AdministratorServiceBlockingStub> {
    private AdministratorServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AdministratorServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AdministratorServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 管控请求
     * </pre>
     */
    public com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse adminRequest(com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAdminRequestMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class AdministratorServiceFutureStub extends io.grpc.stub.AbstractFutureStub<AdministratorServiceFutureStub> {
    private AdministratorServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AdministratorServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AdministratorServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 管控请求
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse> adminRequest(
        com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAdminRequestMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADMIN_REQUEST = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AdministratorServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AdministratorServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ADMIN_REQUEST:
          serviceImpl.adminRequest((com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest) request,
              (io.grpc.stub.StreamObserver<com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse>) responseObserver);
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

  private static abstract class AdministratorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AdministratorServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminGrpcServerOuter.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AdministratorService");
    }
  }

  private static final class AdministratorServiceFileDescriptorSupplier
      extends AdministratorServiceBaseDescriptorSupplier {
    AdministratorServiceFileDescriptorSupplier() {}
  }

  private static final class AdministratorServiceMethodDescriptorSupplier
      extends AdministratorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AdministratorServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (AdministratorServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AdministratorServiceFileDescriptorSupplier())
              .addMethod(getAdminRequestMethod())
              .build();
        }
      }
    }
    return result;
  }
}
