# GrpcStreamingDemo

A minimal Spring Boot demo of all four gRPC communication patterns.

## What It Demonstrates

| # | Pattern | RPC | What Happens |
|---|---------|-----|--------------|
| 1 | **Unary** | `unary` | Single request, single response |
| 2 | **Server Streaming** | `serverStream` | Single request, server streams 5 replies |
| 3 | **Client Streaming** | `clientStream` | Client streams 5 messages, server replies once |
| 4 | **Bidirectional** | `bidiStream` | Both sides stream simultaneously |

## Proto Definition

```protobuf
service GrpcStreamingService {
    rpc unary        (Message) returns (Message) {}
    rpc serverStream (Message) returns (stream Message) {}
    rpc clientStream (stream Message) returns (Message) {}
    rpc bidiStream   (stream Message) returns (stream Message) {}
}

message Message {
    string text = 1;
}
```

## Project Structure

```
src/main/proto/hello.proto                              # Service definition
src/main/java/com/digiwizkid/grpcstreamingdemo/
  GrpcStreamingDemoApplication.java                     # Spring Boot main
  service/GrpcStreamingDemoServiceImpl.java             # gRPC server (all 4 patterns)
  controller/HelloController.java                       # REST endpoints (gRPC client)
src/main/resources/application.yml                      # Config
```

## Run

```bash
./gradlew bootRun
```

## Test

### 1. Unary

```
GET http://localhost:8080/unary?name=Alice
```
```
Hello, Alice!
```

### 2. Server Streaming

```
GET http://localhost:8080/server-stream?msg=Hello
```
```
[1/5] Hello
[2/5] Hello
[3/5] Hello
[4/5] Hello
[5/5] Hello
```

### 3. Client Streaming

```
GET http://localhost:8080/client-stream?prefix=msg
```
```
Server received 5 messages
```

### 4. Bidirectional Streaming

```
GET http://localhost:8080/bidi-stream?msg=ping
```
```
[1] Echo: ping 1
[2] Echo: ping 2
[3] Echo: ping 3
[4] Echo: ping 4
[5] Echo: ping 5
Done - 5 messages echoed
```

## Using Postman

Postman v10.0+ supports gRPC natively.

### Setup

1. Create a new request → select **gRPC** protocol
2. Enter URL: `localhost:9090`
3. Postman auto-discovers services via **Server Reflection** (enabled by default)

### Unary

1. Select method: `hello.GrpcStreamingService/unary`
2. Body tab → enter:
   ```json
   { "text": "Bob" }
   ```
3. Click **Invoke** → response:
   ```json
   { "text": "Hello, Bob!" }
   ```

### Server Streaming

1. Select method: `hello.GrpcStreamingService/serverStream`
2. Body tab → enter:
   ```json
   { "text": "Hi" }
   ```
3. Click **Invoke** → messages appear one by one:
   ```
   [1/5] Hi
   [2/5] Hi
   ...
   ```

### Client Streaming

1. Select method: `hello.GrpcStreamingService/clientStream`
2. Click **+ Message** to add each message, then **Send Message** for each:
   ```
   { "text": "a" }
   { "text": "b" }
   { "text": "c" }
   ```
3. Click **Invoke** → server responds with:
   ```json
   { "text": "Server received 3 messages" }
   ```

### Bidirectional Streaming

1. Select method: `hello.GrpcStreamingService/bidiStream`
2. Add and send messages one at a time:
   ```
   { "text": "ping 1" }
   { "text": "ping 2" }
   ```
3. Click **Invoke** → replies appear as you send:
   ```
   [1] Echo: ping 1
   [2] Echo: ping 2
   ...
   ```

## Config

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | 8080 | REST API |
| `grpc.server.port` | 9090 | gRPC server |
| `grpc.client.hello-service.address` | `static://localhost:9090` | Client target |
