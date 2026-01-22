package com.kafkalens.domain.topic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Kafka 토픽 상세 정보.
 *
 * <p>토픽의 전체 정보를 포함합니다.
 * 기본 정보, 파티션 상세 정보, 토픽 설정을 포함합니다.</p>
 *
 * @param name              토픽 이름
 * @param partitionCount    파티션 수
 * @param replicationFactor 복제 팩터
 * @param isInternal        내부 토픽 여부
 * @param partitions        파티션 상세 정보 목록
 * @param configs           토픽 설정 (cleanup.policy, retention.ms 등)
 */
public record TopicDetail(
        String name,
        int partitionCount,
        int replicationFactor,
        @JsonProperty("internal") boolean isInternal,
        List<PartitionInfo> partitions,
        Map<String, String> configs
) {
    /**
     * TopicDetail 생성자.
     *
     * @param name              토픽 이름 (필수)
     * @param partitionCount    파티션 수 (1 이상)
     * @param replicationFactor 복제 팩터 (1 이상)
     * @param isInternal        내부 토픽 여부
     * @param partitions        파티션 목록
     * @param configs           설정 맵
     */
    public TopicDetail {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be null or blank");
        }
        if (partitionCount < 1) {
            throw new IllegalArgumentException("Partition count must be at least 1");
        }
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("Replication factor must be at least 1");
        }
        if (partitions == null) {
            partitions = List.of();
        }
        if (configs == null) {
            configs = Map.of();
        }
    }

    /**
     * 전체 메시지 수를 계산합니다.
     *
     * @return 모든 파티션의 메시지 수 합계
     */
    public long totalMessageCount() {
        return partitions.stream()
                .mapToLong(PartitionInfo::messageCount)
                .sum();
    }

    /**
     * 언더-레플리케이션 파티션 수를 계산합니다.
     *
     * @return 언더-레플리케이션 상태인 파티션 수
     */
    public long underReplicatedPartitionCount() {
        return partitions.stream()
                .filter(PartitionInfo::isUnderReplicated)
                .count();
    }

    /**
     * Topic(간략 정보)으로 변환합니다.
     *
     * @return Topic 인스턴스
     */
    public Topic toTopic() {
        return new Topic(name, partitionCount, replicationFactor, isInternal);
    }

    /**
     * Builder를 사용하여 TopicDetail을 생성합니다.
     *
     * @return TopicDetailBuilder 인스턴스
     */
    public static TopicDetailBuilder builder() {
        return new TopicDetailBuilder();
    }

    /**
     * TopicDetail Builder 클래스.
     */
    public static class TopicDetailBuilder {
        private String name;
        private int partitionCount = 1;
        private int replicationFactor = 1;
        private boolean isInternal = false;
        private List<PartitionInfo> partitions = List.of();
        private Map<String, String> configs = Map.of();

        public TopicDetailBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TopicDetailBuilder partitionCount(int partitionCount) {
            this.partitionCount = partitionCount;
            return this;
        }

        public TopicDetailBuilder replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public TopicDetailBuilder isInternal(boolean isInternal) {
            this.isInternal = isInternal;
            return this;
        }

        public TopicDetailBuilder partitions(List<PartitionInfo> partitions) {
            this.partitions = partitions;
            return this;
        }

        public TopicDetailBuilder configs(Map<String, String> configs) {
            this.configs = configs;
            return this;
        }

        public TopicDetail build() {
            return new TopicDetail(name, partitionCount, replicationFactor, isInternal, partitions, configs);
        }
    }
}
