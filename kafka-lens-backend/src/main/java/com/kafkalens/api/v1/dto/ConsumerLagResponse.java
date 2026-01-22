package com.kafkalens.api.v1.dto;

import com.kafkalens.domain.consumer.ConsumerLag;
import com.kafkalens.domain.consumer.ConsumerLagSummary;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 컨슈머 Lag 응답 DTO.
 *
 * <p>API 응답용 컨슈머 그룹 Lag 정보입니다.</p>
 *
 * @param groupId       컨슈머 그룹 ID
 * @param totalLag      총 Lag
 * @param partitionCount 파티션 수
 * @param warningCount  warning 상태 파티션 수
 * @param criticalCount critical 상태 파티션 수
 * @param topics        토픽별 Lag 요약 목록
 */
public record ConsumerLagResponse(
        String groupId,
        long totalLag,
        int partitionCount,
        long warningCount,
        long criticalCount,
        List<TopicLagResponse> topics
) {
    /**
     * ConsumerLagSummary에서 응답 DTO를 생성합니다.
     *
     * @param summary ConsumerLagSummary
     * @return ConsumerLagResponse
     */
    public static ConsumerLagResponse from(ConsumerLagSummary summary) {
        List<TopicLagResponse> topics = summary.getTopicSummaries().stream()
                .map(TopicLagResponse::from)
                .collect(Collectors.toList());

        return new ConsumerLagResponse(
                summary.groupId(),
                summary.totalLag(),
                summary.partitionCount(),
                summary.warningCount(),
                summary.criticalCount(),
                topics
        );
    }

    /**
     * 토픽별 Lag 응답 DTO.
     *
     * @param topic          토픽 이름
     * @param totalLag       토픽 총 Lag
     * @param partitionCount 파티션 수
     * @param partitions     파티션별 Lag 목록
     */
    public record TopicLagResponse(
            String topic,
            long totalLag,
            int partitionCount,
            List<PartitionLagResponse> partitions
    ) {
        /**
         * TopicLagSummary에서 응답 DTO를 생성합니다.
         *
         * @param summary TopicLagSummary
         * @return TopicLagResponse
         */
        public static TopicLagResponse from(ConsumerLagSummary.TopicLagSummary summary) {
            List<PartitionLagResponse> partitions = summary.partitions().stream()
                    .map(PartitionLagResponse::from)
                    .collect(Collectors.toList());

            return new TopicLagResponse(
                    summary.topic(),
                    summary.totalLag(),
                    summary.partitionCount(),
                    partitions
            );
        }
    }

    /**
     * 파티션별 Lag 응답 DTO.
     *
     * @param partition     파티션 번호
     * @param currentOffset 현재 커밋된 오프셋
     * @param endOffset     로그 끝 오프셋
     * @param lag           Lag
     * @param status        상태 (normal, warning, critical)
     */
    public record PartitionLagResponse(
            int partition,
            long currentOffset,
            long endOffset,
            long lag,
            String status
    ) {
        /**
         * ConsumerLag에서 응답 DTO를 생성합니다.
         *
         * @param lag ConsumerLag
         * @return PartitionLagResponse
         */
        public static PartitionLagResponse from(ConsumerLag lag) {
            return new PartitionLagResponse(
                    lag.partition(),
                    lag.currentOffset(),
                    lag.endOffset(),
                    lag.lag(),
                    lag.getStatus()
            );
        }
    }
}
