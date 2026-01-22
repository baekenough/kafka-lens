package com.kafkalens.infrastructure.kafka;

import com.kafkalens.common.exception.KafkaConnectionException;
import com.kafkalens.common.exception.KafkaTimeoutException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Kafka AdminClient 래퍼.
 *
 * <p>AdminClient 작업을 간편하게 수행할 수 있는 고수준 API를 제공합니다.
 * 타임아웃 처리와 예외 변환을 담당합니다.</p>
 */
@Component
public class AdminClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(AdminClientWrapper.class);

    private final AdminClientFactory adminClientFactory;
    private final Duration defaultTimeout;

    public AdminClientWrapper(
            AdminClientFactory adminClientFactory,
            @Value("${kafka.admin.default-api-timeout-ms:60000}") int defaultTimeoutMs
    ) {
        this.adminClientFactory = adminClientFactory;
        this.defaultTimeout = Duration.ofMillis(defaultTimeoutMs);
    }

    // === Topic Operations ===

    /**
     * 클러스터의 모든 토픽 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 토픽 이름 목록
     */
    public Set<String> listTopics(String clusterId) {
        return listTopics(clusterId, false);
    }

    /**
     * 클러스터의 토픽 목록을 조회합니다.
     *
     * @param clusterId       클러스터 ID
     * @param includeInternal 내부 토픽 포함 여부
     * @return 토픽 이름 목록
     */
    public Set<String> listTopics(String clusterId, boolean includeInternal) {
        log.debug("Listing topics for cluster: {} (includeInternal: {})", clusterId, includeInternal);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        ListTopicsOptions options = new ListTopicsOptions()
                .listInternal(includeInternal)
                .timeoutMs((int) defaultTimeout.toMillis());

        try {
            return client.listTopics(options).names().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "listTopics", e);
        }
    }

    /**
     * 토픽 상세 정보를 조회합니다.
     *
     * @param clusterId  클러스터 ID
     * @param topicNames 토픽 이름 목록
     * @return 토픽 이름 -> TopicDescription 맵
     */
    public Map<String, TopicDescription> describeTopics(String clusterId, Collection<String> topicNames) {
        log.debug("Describing topics for cluster {}: {}", clusterId, topicNames);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        try {
            return client.describeTopics(topicNames).allTopicNames().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "describeTopics", e);
        }
    }

    /**
     * 단일 토픽 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @return TopicDescription
     */
    public TopicDescription describeTopic(String clusterId, String topicName) {
        Map<String, TopicDescription> descriptions = describeTopics(clusterId, Collections.singleton(topicName));
        return descriptions.get(topicName);
    }

    /**
     * 토픽 설정을 조회합니다.
     *
     * @param clusterId  클러스터 ID
     * @param topicNames 토픽 이름 목록
     * @return 토픽 이름 -> 설정 맵
     */
    public Map<String, Map<String, String>> describeTopicConfigs(String clusterId, Collection<String> topicNames) {
        log.debug("Describing topic configs for cluster {}: {}", clusterId, topicNames);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        List<ConfigResource> resources = topicNames.stream()
                .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                .collect(Collectors.toList());

        try {
            Map<ConfigResource, Config> configs = client.describeConfigs(resources).all().get();

            Map<String, Map<String, String>> result = new HashMap<>();
            for (Map.Entry<ConfigResource, Config> entry : configs.entrySet()) {
                Map<String, String> configMap = entry.getValue().entries().stream()
                        .collect(Collectors.toMap(
                                ConfigEntry::name,
                                ConfigEntry::value,
                                (v1, v2) -> v2
                        ));
                result.put(entry.getKey().name(), configMap);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "describeTopicConfigs", e);
        }
    }

    // === Consumer Group Operations ===

    /**
     * 모든 컨슈머 그룹 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 컨슈머 그룹 목록
     */
    public Collection<ConsumerGroupListing> listConsumerGroups(String clusterId) {
        log.debug("Listing consumer groups for cluster: {}", clusterId);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        try {
            return client.listConsumerGroups().all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "listConsumerGroups", e);
        }
    }

    /**
     * 컨슈머 그룹 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupIds  그룹 ID 목록
     * @return 그룹 ID -> ConsumerGroupDescription 맵
     */
    public Map<String, ConsumerGroupDescription> describeConsumerGroups(String clusterId, Collection<String> groupIds) {
        log.debug("Describing consumer groups for cluster {}: {}", clusterId, groupIds);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        try {
            return client.describeConsumerGroups(groupIds).all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "describeConsumerGroups", e);
        }
    }

    /**
     * 컨슈머 그룹의 오프셋을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   그룹 ID
     * @return TopicPartition -> OffsetAndMetadata 맵
     */
    public Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsets(String clusterId, String groupId) {
        log.debug("Listing consumer group offsets for cluster {}, group: {}", clusterId, groupId);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        try {
            return client.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "listConsumerGroupOffsets", e);
        }
    }

    // === Broker/Cluster Operations ===

    /**
     * 클러스터 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return DescribeClusterResult
     */
    public ClusterInfo describeCluster(String clusterId) {
        log.debug("Describing cluster: {}", clusterId);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        try {
            DescribeClusterResult result = client.describeCluster();
            String kafkaClusterId = result.clusterId().get();
            Node controller = result.controller().get();
            Collection<Node> nodes = result.nodes().get();

            return new ClusterInfo(kafkaClusterId, controller, nodes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "describeCluster", e);
        }
    }

    /**
     * 브로커 설정을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param brokerId  브로커 ID
     * @return 설정 맵
     */
    public Map<String, String> describeBrokerConfig(String clusterId, int brokerId) {
        log.debug("Describing broker config for cluster {}, broker: {}", clusterId, brokerId);

        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        ConfigResource resource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(brokerId));

        try {
            Config config = client.describeConfigs(Collections.singleton(resource)).all().get().get(resource);

            return config.entries().stream()
                    .collect(Collectors.toMap(
                            ConfigEntry::name,
                            ConfigEntry::value,
                            (v1, v2) -> v2
                    ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "describeBrokerConfig", e);
        }
    }

    // === Partition Operations ===

    /**
     * 토픽 파티션의 시작 오프셋을 조회합니다.
     *
     * @param clusterId       클러스터 ID
     * @param topicPartitions 토픽 파티션 목록
     * @return TopicPartition -> 시작 오프셋 맵
     */
    public Map<TopicPartition, Long> getBeginningOffsets(String clusterId, Collection<TopicPartition> topicPartitions) {
        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        Map<TopicPartition, OffsetSpec> offsetSpecs = topicPartitions.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.earliest()));

        try {
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> result =
                    client.listOffsets(offsetSpecs).all().get();

            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().offset()
                    ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "getBeginningOffsets", e);
        }
    }

    /**
     * 토픽 파티션의 끝 오프셋을 조회합니다.
     *
     * @param clusterId       클러스터 ID
     * @param topicPartitions 토픽 파티션 목록
     * @return TopicPartition -> 끝 오프셋 맵
     */
    public Map<TopicPartition, Long> getEndOffsets(String clusterId, Collection<TopicPartition> topicPartitions) {
        AdminClient client = adminClientFactory.getOrCreate(clusterId);

        Map<TopicPartition, OffsetSpec> offsetSpecs = topicPartitions.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

        try {
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> result =
                    client.listOffsets(offsetSpecs).all().get();

            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().offset()
                    ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(clusterId, "Operation interrupted");
        } catch (ExecutionException e) {
            throw handleExecutionException(clusterId, "getEndOffsets", e);
        }
    }

    // === Helper Classes ===

    /**
     * 클러스터 정보 레코드.
     */
    public record ClusterInfo(
            String clusterId,
            Node controller,
            Collection<Node> nodes
    ) {
        /**
         * 브로커 수를 반환합니다.
         */
        public int brokerCount() {
            return nodes != null ? nodes.size() : 0;
        }
    }

    // === Private Methods ===

    /**
     * ExecutionException을 적절한 예외로 변환합니다.
     */
    private RuntimeException handleExecutionException(String clusterId, String operation, ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof TimeoutException) {
            log.warn("Kafka operation timed out: {} on cluster {}", operation, clusterId);
            return new KafkaTimeoutException(clusterId, operation, cause);
        }

        log.error("Kafka operation failed: {} on cluster {}", operation, clusterId, cause);
        return new KafkaConnectionException(clusterId, cause);
    }
}
