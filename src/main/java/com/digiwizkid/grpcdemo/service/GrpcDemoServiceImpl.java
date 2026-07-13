package com.digiwizkid.grpcdemo.service;

import com.digiwizkid.grpcdemo.proto.GrpcDemoServiceGrpc;
import com.digiwizkid.grpcdemo.proto.Message;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GrpcDemoServiceImpl extends GrpcDemoServiceGrpc.GrpcDemoServiceImplBase {

    // 1. Unary - receive one message, send one reply
    @Override
    public void unary(Message request, StreamObserver<Message> responseObserver) {
        responseObserver.onNext(Message.newBuilder()
                .setText("Hello, " + request.getText() + "!")
                .build());
        responseObserver.onCompleted();
    }

    // 2. Server streaming - receive one message, send a stream of replies
    @Override
    public void serverStream(Message request, StreamObserver<Message> responseObserver) {
        for (int i = 1; i <= 5; i++) {
            responseObserver.onNext(Message.newBuilder()
                    .setText("[" + i + "/5] " + request.getText())
                    .build());
            sleep(500);
        }
        responseObserver.onCompleted();
    }

    // 3. Client streaming - receive a stream of messages, send one reply
    @Override
    public StreamObserver<Message> clientStream(StreamObserver<Message> responseObserver) {
        return new StreamObserver<>() {
            private int count = 0;

            @Override
            public void onNext(Message msg) {
                count++;
                System.out.println("Client stream received: " + msg.getText());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Message.newBuilder()
                        .setText("Server received " + count + " messages")
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    // 4. Bidirectional streaming - receive and send messages simultaneously
    @Override
    public StreamObserver<Message> bidiStream(StreamObserver<Message> responseObserver) {
        return new StreamObserver<>() {
            private int count = 0;

            @Override
            public void onNext(Message msg) {
                count++;
                responseObserver.onNext(Message.newBuilder()
                        .setText("[" + count + "] Echo: " + msg.getText())
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Message.newBuilder()
                        .setText("Done - " + count + " messages echoed")
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
