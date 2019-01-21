package EdgeNode.GRPC;

import EdgeNode.StateModel;
import Sensor.Measurement;
import io.grpc.stub.StreamObserver;

public class SensorsGRPCInterfaceImpl extends SensorsGRPCInterfaceGrpc.SensorsGRPCInterfaceImplBase {

    @Override
    public io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> sendMeasurement(io.grpc.stub.StreamObserver<SensorsGRPCInterfaceOuterClass.Null> responseObserver){

        StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> streamObserver = new StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement>() {
            @Override
            public void onNext(SensorsGRPCInterfaceOuterClass.Measurement value) {
                //Aggiunge la misurazione al buffer
                Measurement m = new Measurement(value.getId(), value.getType(), value.getValue(), value.getTimestamp());
                StateModel.getInstance().sensorsMeasurementBuffer.put(m);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        return streamObserver;
    }
}
