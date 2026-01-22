package com.kafkalens.domain.consumer;

import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 컨슈머 그룹 서비스.
 *
 * <p>컨슈머 그룹 관련 비즈니스 로직을 처리합니다.
 * 컨슈머 그룹 목록 조회, 상세 조회, 멤버 조회 등의 기능을 제공합니다.</p>
 */
@Service
public class ConsumerService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    private final ClusterService clusterService;
    private final AdminClientWrapper adminClientWrapper;

    /**
     * ConsumerService 생성자.
     *
     * @param clusterService     클러스터 서비스
     * @param adminClientWrapper AdminClient 래퍼
     */
    public ConsumerService(ClusterService clusterService, AdminClientWrapper adminClientWrapper) {
        this.clusterService = clusterService;
        this.adminClientWrapper = adminClientWrapper;
    }

    /**
     * 클러스터의 모든 컨슈머 그룹 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 컨슈머 그룹 목록
     */
    public List<ConsumerGroup> listGroups(String clusterId) {
        log.debug("Listing consumer groups for cluster: {}", clusterId);

        // 클러스터 존재 확인
        clusterService.findById(clusterId);

        // 컨슈머 그룹 목록 조회
        Collection<ConsumerGroupListing> listings = adminClientWrapper.listConsumerGroups(clusterId);

        if (listings.isEmpty()) {
            log.debug("No consumer groups found for cluster: {}", clusterId);
            return List.of();
        }

        // 그룹 ID 목록 추출
        Set<String> groupIds = listings.stream()
                .map(ConsumerGroupListing::groupId)
                .collect(Collectors.toSet());

        // 그룹 상세 정보 조회
        Map<String, ConsumerGroupDescription> descriptions =
                adminClientWrapper.describeConsumerGroups(clusterId, groupIds);

        // 도메인 모델로 변환
        List<ConsumerGroup> groups = descriptions.values().stream()
                .map(this::toConsumerGroup)
                .sorted(Comparator.comparing(ConsumerGroup::groupId))
                .collect(Collectors.toList());

        log.info("Found {} consumer groups for cluster: {}", groups.size(), clusterId);
        return groups;
    }

    /**
     * 특정 컨슈머 그룹의 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   그룹 ID
     * @return 컨슈머 그룹 상세 정보
     */
    public ConsumerGroup getGroup(String clusterId, String groupId) {
        log.debug("Getting consumer group {} for cluster: {}", groupId, clusterId);

        // 클러스터 존재 확인
        clusterService.findById(clusterId);

        // 그룹 상세 정보 조회
        Map<String, ConsumerGroupDescription> descriptions =
                adminClientWrapper.describeConsumerGroups(clusterId, Collections.singleton(groupId));

        ConsumerGroupDescription description = descriptions.get(groupId);
        if (description == null) {
            log.warn("Consumer group not found: {} in cluster: {}", groupId, clusterId);
            // TODO: ConsumerGroupNotFoundException 추가 가능
            throw new IllegalArgumentException("Consumer group not found: " + groupId);
        }

        return toConsumerGroup(description);
    }

    /**
     * 컨슈머 그룹의 멤버 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   그룹 ID
     * @return 멤버 목록
     */
    public List<ConsumerMember> getMembers(String clusterId, String groupId) {
        log.debug("Getting members for consumer group {} in cluster: {}", groupId, clusterId);

        // 그룹 정보 조회
        ConsumerGroup group = getGroup(clusterId, groupId);

        return group.members();
    }

    // === Private Helper Methods ===

    /**
     * Kafka ConsumerGroupDescription을 도메인 모델로 변환합니다.
     *
     * @param description Kafka ConsumerGroupDescription
     * @return ConsumerGroup 도메인 모델
     */
    private ConsumerGroup toConsumerGroup(ConsumerGroupDescription description) {
        List<ConsumerMember> members = description.members().stream()
                .map(this::toConsumerMember)
                .collect(Collectors.toList());

        int coordinatorId = description.coordinator() != null
                ? description.coordinator().id()
                : -1;

        return ConsumerGroup.builder()
                .groupId(description.groupId())
                .state(formatState(description.state()))
                .coordinator(coordinatorId)
                .memberCount(members.size())
                .members(members)
                .build();
    }

    /**
     * Kafka MemberDescription을 도메인 모델로 변환합니다.
     *
     * @param member Kafka MemberDescription
     * @return ConsumerMember 도메인 모델
     */
    private ConsumerMember toConsumerMember(MemberDescription member) {
        List<TopicPartitionAssignment> assignments = new ArrayList<>();

        if (member.assignment() != null && member.assignment().topicPartitions() != null) {
            for (TopicPartition tp : member.assignment().topicPartitions()) {
                assignments.add(new TopicPartitionAssignment(tp.topic(), tp.partition()));
            }
        }

        // Sort assignments by topic name and partition
        assignments.sort(Comparator
                .comparing(TopicPartitionAssignment::topic)
                .thenComparing(TopicPartitionAssignment::partition));

        return ConsumerMember.builder()
                .memberId(member.consumerId())
                .clientId(member.clientId())
                .host(member.host())
                .assignments(assignments)
                .build();
    }

    /**
     * ConsumerGroupState를 문자열로 변환합니다.
     *
     * @param state Kafka ConsumerGroupState
     * @return 상태 문자열
     */
    private String formatState(org.apache.kafka.common.ConsumerGroupState state) {
        if (state == null) {
            return "Unknown";
        }

        return switch (state) {
            case STABLE -> "Stable";
            case PREPARING_REBALANCE -> "PreparingRebalance";
            case COMPLETING_REBALANCE -> "CompletingRebalance";
            case EMPTY -> "Empty";
            case DEAD -> "Dead";
            default -> "Unknown";
        };
    }
}
