# Omnixys Cache

Valkey-based caching infrastructure with delayed job scheduling.

## Features

- Valkey service with JSON serialization
- Key definition and type-safe cache operations
- Configuration caching
- Delayed job scheduling with worker/poller
- Rate limiting with Valkey
- Pub/sub messaging
- Stream processing
- Distributed locking
- Cache health checks and diagnostics
- Spring Boot auto-configuration

## Installation

```xml
<dependency>
    <groupId>com.omnixys</groupId>
    <artifactId>cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
@Autowired
private ValkeyService valkeyService;

valkeyService.set("key", "value");
Optional<String> result = valkeyService.get("key", String.class);
```
