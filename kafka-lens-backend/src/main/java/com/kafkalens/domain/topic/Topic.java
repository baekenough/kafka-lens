package com.kafkalens.domain.topic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka 토픽 기본 정보.
 *
 * <p>토픽 목록 조회 시 반환되는 간략한 토픽 정보입니다.
 * 파티션 상세 정보나 설정값은 포함하지 않습니다.</p>
 *
 * @param name              토픽 이름
 * @param partitionCount    파티션 수
 * @param replicationFactor 복제 팩터
 * @param isInternal        내부 토픽 여부 (__consumer_offsets 등)
 */
public record Topic(
        String name,
        int partitionCount,
        int replicationFactor,
        @JsonProperty("internal") boolean isInternal
) {
    /**
     * Topic 생성자.
     *
     * @param name              토픽 이름 (필수)
     * @param partitionCount    파티션 수 (1 이상)
     * @param replicationFactor 복제 팩터 (1 이상)
     * @param isInternal        내부 토픽 여부
     * @throws IllegalArgumentException 유효하지 않은 값이 전달된 경우
     */
    public Topic {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be null or blank");
        }
        if (partitionCount < 1) {
            throw new IllegalArgumentException("Partition count must be at least 1");
        }
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("Replication factor must be at least 1");
        }
    }

    /**
     * Builder를 사용하여 Topic을 생성합니다.
     *
     * @return TopicBuilder 인스턴스
     */
    public static TopicBuilder builder() {
        return new TopicBuilder();
    }

    /**
     * Topic Builder 클래스.
     */
    public static class TopicBuilder {
        private String name;
        private int partitionCount = 1;
        private int replicationFactor = 1;
        private boolean isInternal = false;

        public TopicBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TopicBuilder partitionCount(int partitionCount) {
            this.partitionCount = partitionCount;
            return this;
        }

        public TopicBuilder replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public TopicBuilder isInternal(boolean isInternal) {
            this.isInternal = isInternal;
            return this;
        }

        public Topic build() {
            return new Topic(name, partitionCount, replicationFactor, isInternal);
        }
    }
}
