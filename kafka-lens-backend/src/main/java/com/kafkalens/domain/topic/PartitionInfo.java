package com.kafkalens.domain.topic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Kafka 파티션 정보.
 *
 * <p>토픽 파티션의 상세 정보를 담고 있습니다.
 * 리더/레플리카 정보, 오프셋 정보를 포함합니다.</p>
 *
 * @param partition       파티션 번호 (0부터 시작)
 * @param leader          리더 브로커 ID
 * @param replicas        레플리카 브로커 ID 목록
 * @param isr             In-Sync Replicas 브로커 ID 목록
 * @param beginningOffset 시작 오프셋 (가장 오래된 메시지)
 * @param endOffset       종료 오프셋 (다음에 쓸 오프셋)
 */
public record PartitionInfo(
        int partition,
        int leader,
        List<Integer> replicas,
        List<Integer> isr,
        long beginningOffset,
        long endOffset
) {
    /**
     * PartitionInfo 생성자.
     *
     * @param partition       파티션 번호 (0 이상)
     * @param leader          리더 브로커 ID
     * @param replicas        레플리카 목록 (null 허용 안 함)
     * @param isr             ISR 목록 (null 허용 안 함)
     * @param beginningOffset 시작 오프셋 (0 이상)
     * @param endOffset       종료 오프셋 (0 이상)
     */
    public PartitionInfo {
        if (partition < 0) {
            throw new IllegalArgumentException("Partition number must be non-negative");
        }
        if (replicas == null) {
            replicas = List.of();
        }
        if (isr == null) {
            isr = List.of();
        }
        if (beginningOffset < 0) {
            throw new IllegalArgumentException("Beginning offset must be non-negative");
        }
        if (endOffset < 0) {
            throw new IllegalArgumentException("End offset must be non-negative");
        }
    }

    /**
     * 파티션에 저장된 메시지 수를 계산합니다.
     *
     * @return 메시지 수 (endOffset - beginningOffset)
     */
    @JsonProperty("messageCount")
    public long messageCount() {
        return endOffset - beginningOffset;
    }

    /**
     * 파티션이 언더-레플리케이션 상태인지 확인합니다.
     *
     * <p>ISR 크기가 레플리카 수보다 적으면 언더-레플리케이션 상태입니다.</p>
     *
     * @return 언더-레플리케이션 상태이면 true
     */
    public boolean isUnderReplicated() {
        return isr.size() < replicas.size();
    }

    /**
     * Builder를 사용하여 PartitionInfo를 생성합니다.
     *
     * @return PartitionInfoBuilder 인스턴스
     */
    public static PartitionInfoBuilder builder() {
        return new PartitionInfoBuilder();
    }

    /**
     * PartitionInfo Builder 클래스.
     */
    public static class PartitionInfoBuilder {
        private int partition;
        private int leader;
        private List<Integer> replicas = List.of();
        private List<Integer> isr = List.of();
        private long beginningOffset;
        private long endOffset;

        public PartitionInfoBuilder partition(int partition) {
            this.partition = partition;
            return this;
        }

        public PartitionInfoBuilder leader(int leader) {
            this.leader = leader;
            return this;
        }

        public PartitionInfoBuilder replicas(List<Integer> replicas) {
            this.replicas = replicas;
            return this;
        }

        public PartitionInfoBuilder isr(List<Integer> isr) {
            this.isr = isr;
            return this;
        }

        public PartitionInfoBuilder beginningOffset(long beginningOffset) {
            this.beginningOffset = beginningOffset;
            return this;
        }

        public PartitionInfoBuilder endOffset(long endOffset) {
            this.endOffset = endOffset;
            return this;
        }

        public PartitionInfo build() {
            return new PartitionInfo(partition, leader, replicas, isr, beginningOffset, endOffset);
        }
    }
}
