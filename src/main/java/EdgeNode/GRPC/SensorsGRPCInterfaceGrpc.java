package EdgeNode.GRPC;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.7.0)",
    comments = "Source: SensorsGRPCInterface.proto")
public final class SensorsGRPCInterfaceGrpc {

  private SensorsGRPCInterfaceGrpc() {}

  public static final String SERVICE_NAME = "SensorsGRPCInterface";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<SensorsGRPCInterfaceOuterClass.Measurement,
      SensorsGRPCInterfaceOuterClass.Null> METHOD_SEND_MEASUREMENT =
      io.grpc.MethodDescriptor.<SensorsGRPCInterfaceOuterClass.Measurement, SensorsGRPCInterfaceOuterClass.Null>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
          .setFullMethodName(generateFullMethodName(
              "SensorsGRPCInterface", "SendMeasurement"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              SensorsGRPCInterfaceOuterClass.Measurement.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              SensorsGRPCInterfaceOuterClass.Null.getDefaultInstance()))
          .setSchemaDescriptor(new SensorsGRPCInterfaceMethodDescriptorSupplier("SendMeasurement"))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SensorsGRPCInterfaceStub newStub(io.grpc.Channel channel) {
    return new SensorsGRPCInterfaceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SensorsGRPCInterfaceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new SensorsGRPCInterfaceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SensorsGRPCInterfaceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new SensorsGRPCInterfaceFutureStub(channel);
  }

  /**
   */
  public static abstract class SensorsGRPCInterfaceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> sendMeasurement(
        io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Null> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_SEND_MEASUREMENT, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SEND_MEASUREMENT,
            asyncClientStreamingCall(
              new MethodHandlers<
                SensorsGRPCInterfaceOuterClass.Measurement,
                SensorsGRPCInterfaceOuterClass.Null>(
                  this, METHODID_SEND_MEASUREMENT)))
          .build();
    }
  }

  /**
   */
  public static final class SensorsGRPCInterfaceStub extends io.grpc.stub.AbstractStub<SensorsGRPCInterfaceStub> {
    private SensorsGRPCInterfaceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SensorsGRPCInterfaceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SensorsGRPCInterfaceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SensorsGRPCInterfaceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> sendMeasurement(
        io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Null> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(METHOD_SEND_MEASUREMENT, getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class SensorsGRPCInterfaceBlockingStub extends io.grpc.stub.AbstractStub<SensorsGRPCInterfaceBlockingStub> {
    private SensorsGRPCInterfaceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SensorsGRPCInterfaceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SensorsGRPCInterfaceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SensorsGRPCInterfaceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class SensorsGRPCInterfaceFutureStub extends io.grpc.stub.AbstractStub<SensorsGRPCInterfaceFutureStub> {
    private SensorsGRPCInterfaceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SensorsGRPCInterfaceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SensorsGRPCInterfaceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SensorsGRPCInterfaceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SEND_MEASUREMENT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SensorsGRPCInterfaceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SensorsGRPCInterfaceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_MEASUREMENT:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sendMeasurement(
              (io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Null>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class SensorsGRPCInterfaceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SensorsGRPCInterfaceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return SensorsGRPCInterfaceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SensorsGRPCInterface");
    }
  }

  private static final class SensorsGRPCInterfaceFileDescriptorSupplier
      extends SensorsGRPCInterfaceBaseDescriptorSupplier {
    SensorsGRPCInterfaceFileDescriptorSupplier() {}
  }

  private static final class SensorsGRPCInterfaceMethodDescriptorSupplier
      extends SensorsGRPCInterfaceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SensorsGRPCInterfaceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SensorsGRPCInterfaceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SensorsGRPCInterfaceFileDescriptorSupplier())
              .addMethod(METHOD_SEND_MEASUREMENT)
              .build();
        }
      }
    }
    return result;
  }
}
