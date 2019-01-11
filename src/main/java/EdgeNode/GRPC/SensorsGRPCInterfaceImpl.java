package EdgeNode.GRPC;

import EdgeNode.EdgeNode;
import Sensor.Measurement;
import io.grpc.stub.StreamObserver;

public class SensorsGRPCInterfaceImpl extends SensorsGRPCInterfaceGrpc.SensorsGRPCInterfaceImplBase {

    private EdgeNode parent;

    public SensorsGRPCInterfaceImpl(EdgeNode parent){
        this.parent = parent;
    }

    @Override
    public io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> sendMeasurement(io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Null> responseObserver){

        StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> streamObserver = new StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement>() {
            @Override
            public void onNext(SensorsGRPCInterfaceOuterClass.Measurement value) {
                //Aggiunge la misurazione al buffer del parent
                //DEBUG
                System.out.print(".");
                Measurement m = new Measurement(value.getId(), value.getType(), value.getValue(), value.getTimestamp());
                parent.getSensorsMeasurementBuffer().put(m);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                System.out.println("DEBUG: a sensor changed targetNode");
            }
        };

        return streamObserver;
    }
}
