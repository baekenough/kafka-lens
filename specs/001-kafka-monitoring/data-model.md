# Data Model: Kafka Monitoring Dashboard

**Branch**: `001-kafka-monitoring` | **Date**: 2026-01-22

## Overview

이 문서는 kafka-lens의 핵심 데이터 모델을 정의한다.
별도의 데이터베이스 없이 Kafka AdminClient API 응답과 설정 파일을 기반으로 한다.

## Entities

### Cluster (설정 파일 기반)

클러스터 정보는 `clusters.yml`에서 관리된다.

```yaml
# clusters.yml 예시
clusters:
  - id: dev-cluster
    name: "개발 클러스터"
    bootstrapServers: "localhost:9092"
    properties:
      security.protocol: PLAINTEXT

  - id: prod-cluster
    name: "운영 클러스터"
    bootstrapServers: "kafka-prod-1:9092,kafka-prod-2:9092"
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: "org.apache.kafka.common.security.plain.PlainLoginModule required username='${KAFKA_USER}' password='${KAFKA_PASSWORD}';"
```

```java
public record Cluster(
    String id,
    String name,
    String bootstrapServers,
    Map<String, String> properties
) {}
```

### Broker (AdminClient API)

Kafka 브로커 정보. `describeCluster()` API로 조회.

```java
public record Broker(
    int id,
    String host,
    int port,
    String rack,           // nullable
    boolean isController
) {}
```

### Topic (AdminClient API)

토픽 정보. `listTopics()`, `describeTopics()` API로 조회.

```java
public record Topic(
    String name,
    int partitionCount,
    int replicationFactor,
    boolean isInternal
) {}

public record TopicDetail(
    String name,
    List<PartitionInfo> partitions,
    Map<String, String> configs
) {}

public record PartitionInfo(
    int partition,
    int leader,
    List<Integer> replicas,
    List<Integer> isr,       // In-Sync Replicas
    long beginningOffset,
    long endOffset
) {}
```

### ConsumerGroup (AdminClient API)

컨슈머 그룹 정보. `listConsumerGroups()`, `describeConsumerGroups()` API로 조회.

```java
public record ConsumerGroup(
    String groupId,
    String state,           // Empty, Dead, Stable, PreparingRebalance, CompletingRebalance
    String coordinator,     // 코디네이터 브로커
    int memberCount,
    List<ConsumerMember> members
) {}

public record ConsumerMember(
    String memberId,
    String clientId,
    String host,
    List<TopicPartition> assignments
) {}

public record TopicPartition(
    String topic,
    int partition
) {}
```

### ConsumerLag (AdminClient API)

컨슈머 랙 정보. `listConsumerGroupOffsets()`, `listOffsets()` API로 계산.

```java
public record ConsumerLag(
    String groupId,
    String topic,
    int partition,
    long currentOffset,     // 컨슈머가 마지막으로 커밋한 오프셋
    long endOffset,         // 파티션의 최신 오프셋
    long lag                // endOffset - currentOffset
) {}

public record ConsumerLagSummary(
    String groupId,
    long totalLag,
    List<ConsumerLag> partitionLags
) {}
```

### KafkaMessage (Consumer API)

메시지 조회 결과. `KafkaConsumer.poll()` API로 조회.

```java
public record KafkaMessage(
    String topic,
    int partition,
    long offset,
    long timestamp,
    String timestampType,   // CreateTime, LogAppendTime
    String key,             // Base64 또는 문자열
    String value,           // Base64 또는 문자열
    Map<String, String> headers
) {}
```

## API Response DTOs

### Common Response Wrapper

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, null), Instant.now());
    }
}

public record ApiError(
    String code,
    String message,
    Map<String, Object> details
) {}
```

### List Response

```java
public record PagedResponse<T>(
    List<T> items,
    int totalCount,
    int page,
    int pageSize
) {}
```

## Error Codes

| Code | Description |
|------|-------------|
| `CLUSTER_NOT_FOUND` | 클러스터 ID가 존재하지 않음 |
| `CLUSTER_CONNECTION_FAILED` | 클러스터 연결 실패 |
| `CLUSTER_TIMEOUT` | 클러스터 연결 타임아웃 |
| `TOPIC_NOT_FOUND` | 토픽이 존재하지 않음 |
| `CONSUMER_GROUP_NOT_FOUND` | 컨슈머 그룹이 존재하지 않음 |
| `INVALID_OFFSET` | 유효하지 않은 오프셋 |
| `MESSAGE_LIMIT_EXCEEDED` | 메시지 조회 제한 초과 (1000건) |
| `UNAUTHORIZED` | 인증 필요 |
| `FORBIDDEN` | 권한 없음 |

## Relationships

```
Cluster (1) ──────── (N) Topic
    │                    │
    │                    └── (N) Partition
    │
    ├── (N) Broker
    │
    └── (N) ConsumerGroup
                │
                ├── (N) ConsumerMember
                │
                └── (N) ConsumerLag
```

## Notes

- 모든 데이터는 Kafka AdminClient/Consumer API를 통해 실시간 조회
- 클러스터 설정만 `clusters.yml` 파일에 저장
- 별도의 데이터베이스 불필요 (stateless)
- 메시지 값은 바이너리인 경우 Base64 인코딩
