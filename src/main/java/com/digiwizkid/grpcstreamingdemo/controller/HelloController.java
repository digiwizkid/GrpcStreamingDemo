package com.digiwizkid.grpcstreamingdemo.controller;

import com.digiwizkid.grpcstreamingdemo.proto.GrpcStreamingServiceGrpc;
import com.digiwizkid.grpcstreamingdemo.proto.Message;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
public class HelloController {

    @GrpcClient("hello-service")
    private GrpcStreamingServiceGrpc.GrpcStreamingServiceBlockingStub blockingStub;

    @GrpcClient("hello-service")
    private GrpcStreamingServiceGrpc.GrpcStreamingServiceStub asyncStub;

    // 1. Unary
    @GetMapping("/unary")
    public String unary(@RequestParam(defaultValue = "World") String name) {
        Message reply = blockingStub.unary(Message.newBuilder().setText(name).build());
        return reply.getText();
    }

    // 2. Server streaming - returns SSE stream
    @GetMapping(value = "/server-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter serverStream(@RequestParam(defaultValue = "Hello") String msg) {
        SseEmitter emitter = new SseEmitter(30_000L);

        asyncStub.serverStream(Message.newBuilder().setText(msg).build(), new StreamObserver<>() {
            @Override
            public void onNext(Message reply) {
                try {
                    emitter.send(SseEmitter.event().data(reply.getText()));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                emitter.completeWithError(t);
            }

            @Override
            public void onCompleted() {
                emitter.complete();
            }
        });

        return emitter;
    }

    // 3. Client streaming
    @GetMapping("/client-stream")
    public String clientStream(@RequestParam(defaultValue = "msg") String prefix) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String[] result = {null};

        StreamObserver<Message> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Message reply) {
                result[0] = reply.getText();
            }

            @Override
            public void onError(Throwable t) {
                result[0] = "Error: " + t.getMessage();
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<Message> requestObserver = asyncStub.clientStream(responseObserver);

        for (int i = 1; i <= 5; i++) {
            requestObserver.onNext(Message.newBuilder().setText(prefix + " " + i).build());
            Thread.sleep(300);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
        return result[0];
    }

    // 4. Bidirectional streaming - returns SSE stream
    @GetMapping(value = "/bidi-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter bidiStream(@RequestParam(defaultValue = "ping") String msg) {
        SseEmitter emitter = new SseEmitter(30_000L);

        StreamObserver<Message> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Message reply) {
                try {
                    emitter.send(SseEmitter.event().data(reply.getText()));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                emitter.completeWithError(t);
            }

            @Override
            public void onCompleted() {
                emitter.complete();
            }
        };

        StreamObserver<Message> requestObserver = asyncStub.bidiStream(responseObserver);

        new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    requestObserver.onNext(Message.newBuilder().setText(msg + " " + i).build());
                    Thread.sleep(500);
                }
                requestObserver.onCompleted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                requestObserver.onError(e);
            }
        }).start();

        return emitter;
    }
}
