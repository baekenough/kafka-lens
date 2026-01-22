package com.kafkalens.domain.consumer;

/**
 * 토픽 파티션 할당 정보.
 *
 * <p>컨슈머 멤버에게 할당된 토픽 파티션 정보를 나타냅니다.</p>
 *
 * @param topic     토픽 이름
 * @param partition 파티션 번호
 */
public record TopicPartitionAssignment(
        String topic,
        int partition
) {
    /**
     * TopicPartitionAssignment 생성자.
     *
     * @param topic     토픽 이름 (null 불가)
     * @param partition 파티션 번호 (0 이상)
     */
    public TopicPartitionAssignment {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be null or blank");
        }
        if (partition < 0) {
            throw new IllegalArgumentException("Partition number cannot be negative");
        }
    }
}
