package com.kafkalens.domain.topic;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.TopicNotFoundException;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 토픽 서비스.
 *
 * <p>토픽 관련 비즈니스 로직을 처리합니다.
 * 토픽 목록 조회, 상세 조회, 파티션 정보 조회 등의 기능을 제공합니다.</p>
 */
@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private final AdminClientWrapper adminClientWrapper;
    private final ClusterService clusterService;

    /**
     * TopicService 생성자.
     *
     * @param adminClientWrapper Kafka AdminClient 래퍼
     * @param clusterService     클러스터 서비스
     */
    public TopicService(AdminClientWrapper adminClientWrapper, ClusterService clusterService) {
        this.adminClientWrapper = adminClientWrapper;
        this.clusterService = clusterService;
    }

    /**
     * 클러스터의 토픽 목록을 조회합니다.
     *
     * @param clusterId       클러스터 ID
     * @param includeInternal 내부 토픽 포함 여부
     * @return 토픽 목록
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     */
    public List<Topic> listTopics(String clusterId, boolean includeInternal) {
        log.debug("Listing topics for cluster: {} (includeInternal: {})", clusterId, includeInternal);

        validateClusterExists(clusterId);

        Set<String> topicNames = adminClientWrapper.listTopics(clusterId, includeInternal);

        if (topicNames.isEmpty()) {
            return List.of();
        }

        Map<String, TopicDescription> descriptions = adminClientWrapper.describeTopics(clusterId, topicNames);

        return descriptions.values().stream()
                .map(this::toTopic)
                .sorted(Comparator.comparing(Topic::name))
                .collect(Collectors.toList());
    }

    /**
     * 토픽 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @return 토픽 상세 정보
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     * @throws TopicNotFoundException   토픽을 찾을 수 없는 경우
     */
    public TopicDetail getTopic(String clusterId, String topicName) {
        log.debug("Getting topic detail for cluster: {}, topic: {}", clusterId, topicName);

        validateClusterExists(clusterId);

        TopicDescription description = adminClientWrapper.describeTopic(clusterId, topicName);

        if (description == null) {
            throw new TopicNotFoundException(clusterId, topicName);
        }

        // 토픽 설정 조회
        Map<String, Map<String, String>> configs = adminClientWrapper.describeTopicConfigs(
                clusterId, Set.of(topicName));
        Map<String, String> topicConfigs = configs.getOrDefault(topicName, Map.of());

        // 파티션 정보 조회
        List<PartitionInfo> partitions = getPartitionInfoList(clusterId, topicName, description);

        return TopicDetail.builder()
                .name(description.name())
                .partitionCount(description.partitions().size())
                .replicationFactor(getReplicationFactor(description))
                .isInternal(description.isInternal())
                .partitions(partitions)
                .configs(topicConfigs)
                .build();
    }

    /**
     * 토픽의 파티션 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @return 파티션 목록
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     * @throws TopicNotFoundException   토픽을 찾을 수 없는 경우
     */
    public List<PartitionInfo> getTopicPartitions(String clusterId, String topicName) {
        log.debug("Getting partitions for cluster: {}, topic: {}", clusterId, topicName);

        validateClusterExists(clusterId);

        TopicDescription description = adminClientWrapper.describeTopic(clusterId, topicName);

        if (description == null) {
            throw new TopicNotFoundException(clusterId, topicName);
        }

        return getPartitionInfoList(clusterId, topicName, description);
    }

    // === Private Helper Methods ===

    /**
     * 클러스터 존재 여부를 검증합니다.
     */
    private void validateClusterExists(String clusterId) {
        if (!clusterService.existsById(clusterId)) {
            throw new ClusterNotFoundException(clusterId);
        }
    }

    /**
     * TopicDescription을 Topic으로 변환합니다.
     */
    private Topic toTopic(TopicDescription description) {
        return Topic.builder()
                .name(description.name())
                .partitionCount(description.partitions().size())
                .replicationFactor(getReplicationFactor(description))
                .isInternal(description.isInternal())
                .build();
    }

    /**
     * 복제 팩터를 계산합니다.
     */
    private int getReplicationFactor(TopicDescription description) {
        if (description.partitions().isEmpty()) {
            return 0;
        }
        return description.partitions().get(0).replicas().size();
    }

    /**
     * 파티션 정보 목록을 조회합니다.
     */
    private List<PartitionInfo> getPartitionInfoList(String clusterId, String topicName, TopicDescription description) {
        List<TopicPartition> topicPartitions = description.partitions().stream()
                .map(p -> new TopicPartition(topicName, p.partition()))
                .collect(Collectors.toList());

        // 오프셋 정보 조회
        Map<TopicPartition, Long> beginningOffsets = adminClientWrapper.getBeginningOffsets(clusterId, topicPartitions);
        Map<TopicPartition, Long> endOffsets = adminClientWrapper.getEndOffsets(clusterId, topicPartitions);

        return description.partitions().stream()
                .map(partitionInfo -> {
                    TopicPartition tp = new TopicPartition(topicName, partitionInfo.partition());
                    long beginningOffset = beginningOffsets.getOrDefault(tp, 0L);
                    long endOffset = endOffsets.getOrDefault(tp, 0L);

                    return toPartitionInfo(partitionInfo, beginningOffset, endOffset);
                })
                .sorted(Comparator.comparingInt(PartitionInfo::partition))
                .collect(Collectors.toList());
    }

    /**
     * TopicPartitionInfo를 PartitionInfo로 변환합니다.
     */
    private PartitionInfo toPartitionInfo(TopicPartitionInfo partitionInfo, long beginningOffset, long endOffset) {
        return PartitionInfo.builder()
                .partition(partitionInfo.partition())
                .leader(partitionInfo.leader() != null ? partitionInfo.leader().id() : -1)
                .replicas(partitionInfo.replicas().stream()
                        .map(Node::id)
                        .collect(Collectors.toList()))
                .isr(partitionInfo.isr().stream()
                        .map(Node::id)
                        .collect(Collectors.toList()))
                .beginningOffset(beginningOffset)
                .endOffset(endOffset)
                .build();
    }
}
