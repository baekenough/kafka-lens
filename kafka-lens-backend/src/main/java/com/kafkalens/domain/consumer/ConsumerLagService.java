package com.kafkalens.domain.consumer;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.ClusterRepository;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 컨슈머 Lag 서비스.
 *
 * <p>컨슈머 그룹의 Lag를 계산하고 요약 정보를 제공합니다.
 * Lag = endOffset - currentOffset 으로 계산됩니다.</p>
 */
@Service
public class ConsumerLagService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerLagService.class);

    private final ClusterRepository clusterRepository;
    private final AdminClientWrapper adminClientWrapper;

    /**
     * ConsumerLagService 생성자.
     *
     * @param clusterRepository  클러스터 저장소
     * @param adminClientWrapper AdminClient 래퍼
     */
    public ConsumerLagService(
            ClusterRepository clusterRepository,
            AdminClientWrapper adminClientWrapper
    ) {
        this.clusterRepository = clusterRepository;
        this.adminClientWrapper = adminClientWrapper;
    }

    /**
     * Lag를 계산합니다.
     *
     * @param currentOffset 현재 커밋된 오프셋
     * @param endOffset     로그 끝 오프셋
     * @return Lag (endOffset - currentOffset, 최소 0)
     */
    public long calculateLag(long currentOffset, long endOffset) {
        return Math.max(0L, endOffset - currentOffset);
    }

    /**
     * 컨슈머 그룹의 Lag 요약을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   컨슈머 그룹 ID
     * @return ConsumerLagSummary
     * @throws ClusterNotFoundException 클러스터가 존재하지 않는 경우
     */
    public ConsumerLagSummary getLagSummary(String clusterId, String groupId) {
        log.debug("Getting lag summary for cluster: {}, group: {}", clusterId, groupId);

        // 클러스터 존재 확인
        if (!clusterRepository.existsById(clusterId)) {
            throw new ClusterNotFoundException(clusterId);
        }

        // 컨슈머 그룹 오프셋 조회
        Map<TopicPartition, OffsetAndMetadata> consumerOffsets =
                adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId);

        // 오프셋이 없으면 빈 요약 반환
        if (consumerOffsets == null || consumerOffsets.isEmpty()) {
            log.debug("No offsets found for consumer group: {}", groupId);
            return ConsumerLagSummary.empty(groupId);
        }

        // 토픽 파티션 목록
        Set<TopicPartition> topicPartitions = consumerOffsets.keySet();

        // End 오프셋 조회
        Map<TopicPartition, Long> endOffsets =
                adminClientWrapper.getEndOffsets(clusterId, topicPartitions);

        // 파티션별 Lag 계산
        List<ConsumerLag> partitionLags = new ArrayList<>();

        for (TopicPartition tp : topicPartitions) {
            OffsetAndMetadata offsetAndMetadata = consumerOffsets.get(tp);
            Long endOffset = endOffsets.get(tp);

            if (offsetAndMetadata != null && endOffset != null) {
                long currentOffset = offsetAndMetadata.offset();
                long lag = calculateLag(currentOffset, endOffset);

                ConsumerLag consumerLag = ConsumerLag.of(
                        groupId,
                        tp.topic(),
                        tp.partition(),
                        currentOffset,
                        endOffset
                );

                partitionLags.add(consumerLag);

                log.trace("Partition {}-{}: currentOffset={}, endOffset={}, lag={}",
                        tp.topic(), tp.partition(), currentOffset, endOffset, lag);
            }
        }

        // 정렬: 토픽 이름, 파티션 번호 순
        partitionLags.sort((a, b) -> {
            int topicCompare = a.topic().compareTo(b.topic());
            if (topicCompare != 0) {
                return topicCompare;
            }
            return Integer.compare(a.partition(), b.partition());
        });

        ConsumerLagSummary summary = ConsumerLagSummary.of(groupId, partitionLags);

        log.info("Lag summary for group {}: totalLag={}, partitions={}, warnings={}",
                groupId, summary.totalLag(), summary.partitionCount(), summary.warningCount());

        return summary;
    }
}
