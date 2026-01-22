package com.kafkalens.domain.consumer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 컨슈머 그룹 Lag 요약.
 *
 * <p>컨슈머 그룹의 전체 Lag 정보를 요약합니다.
 * 총 Lag와 파티션별 Lag 목록을 포함합니다.</p>
 *
 * @param groupId       컨슈머 그룹 ID
 * @param totalLag      총 Lag (모든 파티션의 합)
 * @param partitionLags 파티션별 Lag 목록
 */
public record ConsumerLagSummary(
        String groupId,
        long totalLag,
        List<ConsumerLag> partitionLags
) {
    /**
     * ConsumerLagSummary 생성자.
     * partitionLags가 null인 경우 빈 목록으로 초기화합니다.
     */
    public ConsumerLagSummary {
        if (partitionLags == null) {
            partitionLags = List.of();
        } else {
            // 불변 리스트로 복사
            partitionLags = List.copyOf(partitionLags);
        }
    }

    /**
     * 파티션 Lag 목록으로부터 요약을 생성합니다.
     *
     * @param groupId       컨슈머 그룹 ID
     * @param partitionLags 파티션별 Lag 목록
     * @return ConsumerLagSummary 인스턴스
     */
    public static ConsumerLagSummary of(String groupId, List<ConsumerLag> partitionLags) {
        long totalLag = partitionLags.stream()
                .mapToLong(ConsumerLag::lag)
                .sum();
        return new ConsumerLagSummary(groupId, totalLag, partitionLags);
    }

    /**
     * 빈 요약을 생성합니다.
     *
     * @param groupId 컨슈머 그룹 ID
     * @return 빈 ConsumerLagSummary 인스턴스
     */
    public static ConsumerLagSummary empty(String groupId) {
        return new ConsumerLagSummary(groupId, 0L, List.of());
    }

    /**
     * 파티션 수를 반환합니다.
     *
     * @return 파티션 수
     */
    public int partitionCount() {
        return partitionLags.size();
    }

    /**
     * warning 상태인 파티션 수를 반환합니다.
     *
     * @return warning 상태 파티션 수
     */
    public long warningCount() {
        return partitionLags.stream()
                .filter(ConsumerLag::isWarning)
                .count();
    }

    /**
     * critical 상태인 파티션 수를 반환합니다.
     *
     * @return critical 상태 파티션 수
     */
    public long criticalCount() {
        return partitionLags.stream()
                .filter(ConsumerLag::isCritical)
                .count();
    }

    /**
     * 전체 Lag가 warning 임계값 이상인지 확인합니다.
     *
     * @return 총 Lag >= 1000 이면 true
     */
    public boolean isWarning() {
        return totalLag >= ConsumerLag.WARNING_THRESHOLD;
    }

    /**
     * 토픽별 Lag 요약을 반환합니다.
     *
     * @return 토픽별 Lag 요약 목록
     */
    public List<TopicLagSummary> getTopicSummaries() {
        Map<String, List<ConsumerLag>> byTopic = partitionLags.stream()
                .collect(Collectors.groupingBy(ConsumerLag::topic));

        return byTopic.entrySet().stream()
                .map(entry -> {
                    String topic = entry.getKey();
                    List<ConsumerLag> lags = entry.getValue();
                    long topicTotalLag = lags.stream()
                            .mapToLong(ConsumerLag::lag)
                            .sum();
                    return new TopicLagSummary(topic, topicTotalLag, lags);
                })
                .sorted(Comparator.comparing(TopicLagSummary::topic))
                .collect(Collectors.toList());
    }

    /**
     * 토픽별 Lag 요약 레코드.
     *
     * @param topic     토픽 이름
     * @param totalLag  토픽의 총 Lag
     * @param partitions 파티션별 Lag 목록
     */
    public record TopicLagSummary(
            String topic,
            long totalLag,
            List<ConsumerLag> partitions
    ) {
        /**
         * 파티션 수를 반환합니다.
         */
        public int partitionCount() {
            return partitions != null ? partitions.size() : 0;
        }
    }
}
