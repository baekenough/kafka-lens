package com.kafkalens.domain.consumer;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.ClusterRepository;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ConsumerLagService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, Consumer Lag 계산 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConsumerLagServiceTest {

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private AdminClientWrapper adminClientWrapper;

    private ConsumerLagService consumerLagService;

    private Cluster testCluster;

    @BeforeEach
    void setUp() {
        consumerLagService = new ConsumerLagService(clusterRepository, adminClientWrapper);

        testCluster = Cluster.builder()
                .id("test-cluster")
                .name("Test Cluster")
                .description("Test Kafka cluster")
                .environment("development")
                .bootstrapServers("localhost:9092")
                .build();
    }

    @Nested
    @DisplayName("calculateLag()")
    class CalculateLag {

        @Test
        @DisplayName("Lag를 올바르게 계산한다 (endOffset - currentOffset)")
        void testCalculateLag_calculatesCorrectly() {
            // given
            long currentOffset = 100L;
            long endOffset = 150L;

            // when
            long lag = consumerLagService.calculateLag(currentOffset, endOffset);

            // then
            assertThat(lag).isEqualTo(50L);
        }

        @Test
        @DisplayName("currentOffset이 endOffset보다 크면 0을 반환한다")
        void testCalculateLag_currentOffsetGreaterThanEndOffset_returnsZero() {
            // given
            long currentOffset = 200L;
            long endOffset = 150L;

            // when
            long lag = consumerLagService.calculateLag(currentOffset, endOffset);

            // then
            assertThat(lag).isEqualTo(0L);
        }

        @Test
        @DisplayName("currentOffset과 endOffset이 같으면 0을 반환한다")
        void testCalculateLag_equalOffsets_returnsZero() {
            // given
            long currentOffset = 100L;
            long endOffset = 100L;

            // when
            long lag = consumerLagService.calculateLag(currentOffset, endOffset);

            // then
            assertThat(lag).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getLagSummary()")
    class GetLagSummary {

        @Test
        @DisplayName("컨슈머 그룹의 Lag 요약을 반환한다")
        void testGetLagSummary_returnsLagSummary() {
            // given
            String clusterId = "test-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(true);

            // 컨슈머 그룹 오프셋 설정
            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = new HashMap<>();
            TopicPartition tp0 = new TopicPartition("test-topic", 0);
            TopicPartition tp1 = new TopicPartition("test-topic", 1);
            consumerOffsets.put(tp0, new OffsetAndMetadata(100L));
            consumerOffsets.put(tp1, new OffsetAndMetadata(200L));

            given(adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId))
                    .willReturn(consumerOffsets);

            // end 오프셋 설정
            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            endOffsets.put(tp0, 150L);  // lag = 50
            endOffsets.put(tp1, 300L);  // lag = 100

            given(adminClientWrapper.getEndOffsets(eq(clusterId), any()))
                    .willReturn(endOffsets);

            // when
            ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);

            // then
            assertThat(summary).isNotNull();
            assertThat(summary.groupId()).isEqualTo(groupId);
            assertThat(summary.totalLag()).isEqualTo(150L);  // 50 + 100
            assertThat(summary.partitionLags()).hasSize(2);

            verify(clusterRepository).existsById(clusterId);
            verify(adminClientWrapper).listConsumerGroupOffsets(clusterId, groupId);
            verify(adminClientWrapper).getEndOffsets(eq(clusterId), any());
        }

        @Test
        @DisplayName("존재하지 않는 클러스터에서 조회하면 예외를 발생시킨다")
        void testGetLagSummary_nonExistingCluster_throwsException() {
            // given
            String clusterId = "unknown-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> consumerLagService.getLagSummary(clusterId, groupId))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining(clusterId);

            verify(clusterRepository).existsById(clusterId);
        }

        @Test
        @DisplayName("파티션별 Lag 정보가 올바르게 포함된다")
        void testGetLagSummary_containsPartitionLags() {
            // given
            String clusterId = "test-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(true);

            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = new HashMap<>();
            TopicPartition tp0 = new TopicPartition("test-topic", 0);
            consumerOffsets.put(tp0, new OffsetAndMetadata(100L));

            given(adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId))
                    .willReturn(consumerOffsets);

            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            endOffsets.put(tp0, 200L);

            given(adminClientWrapper.getEndOffsets(eq(clusterId), any()))
                    .willReturn(endOffsets);

            // when
            ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);

            // then
            assertThat(summary.partitionLags()).hasSize(1);
            ConsumerLag lag = summary.partitionLags().get(0);
            assertThat(lag.groupId()).isEqualTo(groupId);
            assertThat(lag.topic()).isEqualTo("test-topic");
            assertThat(lag.partition()).isEqualTo(0);
            assertThat(lag.currentOffset()).isEqualTo(100L);
            assertThat(lag.endOffset()).isEqualTo(200L);
            assertThat(lag.lag()).isEqualTo(100L);
        }

        @Test
        @DisplayName("빈 오프셋 목록에서는 빈 요약을 반환한다")
        void testGetLagSummary_emptyOffsets_returnsEmptySummary() {
            // given
            String clusterId = "test-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(true);
            given(adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId))
                    .willReturn(new HashMap<>());

            // when
            ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);

            // then
            assertThat(summary).isNotNull();
            assertThat(summary.groupId()).isEqualTo(groupId);
            assertThat(summary.totalLag()).isEqualTo(0L);
            assertThat(summary.partitionLags()).isEmpty();
        }

        @Test
        @DisplayName("여러 토픽의 Lag를 모두 포함한다")
        void testGetLagSummary_multipleTopics_includesAll() {
            // given
            String clusterId = "test-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(true);

            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = new HashMap<>();
            TopicPartition tp1 = new TopicPartition("topic-a", 0);
            TopicPartition tp2 = new TopicPartition("topic-b", 0);
            consumerOffsets.put(tp1, new OffsetAndMetadata(50L));
            consumerOffsets.put(tp2, new OffsetAndMetadata(100L));

            given(adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId))
                    .willReturn(consumerOffsets);

            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            endOffsets.put(tp1, 100L);  // lag = 50
            endOffsets.put(tp2, 200L);  // lag = 100

            given(adminClientWrapper.getEndOffsets(eq(clusterId), any()))
                    .willReturn(endOffsets);

            // when
            ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);

            // then
            assertThat(summary.totalLag()).isEqualTo(150L);
            assertThat(summary.partitionLags()).hasSize(2);

            // topic-a와 topic-b 모두 포함되어 있는지 확인
            assertThat(summary.partitionLags())
                    .extracting(ConsumerLag::topic)
                    .containsExactlyInAnyOrder("topic-a", "topic-b");
        }

        @Test
        @DisplayName("Lag가 1000 이상이면 warning 상태로 표시된다")
        void testGetLagSummary_highLag_indicatesWarning() {
            // given
            String clusterId = "test-cluster";
            String groupId = "test-group";

            given(clusterRepository.existsById(clusterId)).willReturn(true);

            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = new HashMap<>();
            TopicPartition tp0 = new TopicPartition("test-topic", 0);
            consumerOffsets.put(tp0, new OffsetAndMetadata(0L));

            given(adminClientWrapper.listConsumerGroupOffsets(clusterId, groupId))
                    .willReturn(consumerOffsets);

            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            endOffsets.put(tp0, 1500L);  // lag = 1500 (>= 1000, warning)

            given(adminClientWrapper.getEndOffsets(eq(clusterId), any()))
                    .willReturn(endOffsets);

            // when
            ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);

            // then
            assertThat(summary.partitionLags()).hasSize(1);
            ConsumerLag lag = summary.partitionLags().get(0);
            assertThat(lag.lag()).isEqualTo(1500L);
            assertThat(lag.isWarning()).isTrue();
        }
    }
}
