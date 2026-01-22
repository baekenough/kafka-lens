package com.kafkalens.domain.consumer;

/**
 * 컨슈머 Lag 정보.
 *
 * <p>특정 파티션에 대한 컨슈머 그룹의 Lag 정보를 표현합니다.
 * Lag = endOffset - currentOffset 으로 계산됩니다.</p>
 *
 * @param groupId       컨슈머 그룹 ID
 * @param topic         토픽 이름
 * @param partition     파티션 번호
 * @param currentOffset 현재 커밋된 오프셋
 * @param endOffset     로그 끝 오프셋
 * @param lag           Lag (endOffset - currentOffset)
 */
public record ConsumerLag(
        String groupId,
        String topic,
        int partition,
        long currentOffset,
        long endOffset,
        long lag
) {
    /**
     * Lag 임계값 (1000 이상이면 warning)
     */
    public static final long WARNING_THRESHOLD = 1000L;

    /**
     * ConsumerLag 생성자.
     * lag가 음수가 되지 않도록 보장합니다.
     */
    public ConsumerLag {
        if (lag < 0) {
            lag = 0L;
        }
    }

    /**
     * 파티션별 Lag 정보를 생성합니다.
     *
     * @param groupId       컨슈머 그룹 ID
     * @param topic         토픽 이름
     * @param partition     파티션 번호
     * @param currentOffset 현재 커밋된 오프셋
     * @param endOffset     로그 끝 오프셋
     * @return ConsumerLag 인스턴스
     */
    public static ConsumerLag of(
            String groupId,
            String topic,
            int partition,
            long currentOffset,
            long endOffset
    ) {
        long calculatedLag = Math.max(0L, endOffset - currentOffset);
        return new ConsumerLag(groupId, topic, partition, currentOffset, endOffset, calculatedLag);
    }

    /**
     * Lag가 warning 임계값 이상인지 확인합니다.
     *
     * @return Lag >= 1000 이면 true
     */
    public boolean isWarning() {
        return lag >= WARNING_THRESHOLD;
    }

    /**
     * Lag가 critical 임계값 이상인지 확인합니다.
     * (10000 이상이면 critical)
     *
     * @return Lag >= 10000 이면 true
     */
    public boolean isCritical() {
        return lag >= WARNING_THRESHOLD * 10;
    }

    /**
     * Lag 상태를 반환합니다.
     *
     * @return "critical", "warning", 또는 "normal"
     */
    public String getStatus() {
        if (isCritical()) {
            return "critical";
        } else if (isWarning()) {
            return "warning";
        }
        return "normal";
    }
}
