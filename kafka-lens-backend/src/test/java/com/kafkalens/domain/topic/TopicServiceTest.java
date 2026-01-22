package com.kafkalens.domain.topic;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.TopicNotFoundException;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * TopicService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, TopicService의 핵심 비즈니스 로직을 검증합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *   <li>listTopics: 토픽 목록 조회</li>
 *   <li>getTopic: 토픽 상세 조회</li>
 *   <li>getTopicPartitions: 토픽 파티션 조회</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private AdminClientWrapper adminClientWrapper;

    @Mock
    private ClusterService clusterService;

    private TopicService topicService;

    private static final String CLUSTER_ID = "local";
    private static final String TOPIC_NAME = "test-topic";
    private static final String INTERNAL_TOPIC = "__consumer_offsets";

    @BeforeEach
    void setUp() {
        topicService = new TopicService(adminClientWrapper, clusterService);
    }

    @Nested
    @DisplayName("listTopics()")
    class ListTopics {

        @Test
        @DisplayName("클러스터의 모든 토픽 목록을 반환한다")
        void testListTopics_returnsAllTopics() {
            // given
            Set<String> topicNames = Set.of("topic-1", "topic-2", "topic-3");
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.listTopics(CLUSTER_ID, false)).willReturn(topicNames);

            Map<String, TopicDescription> descriptions = createMockTopicDescriptions(topicNames);
            given(adminClientWrapper.describeTopics(CLUSTER_ID, topicNames)).willReturn(descriptions);

            // when
            List<Topic> result = topicService.listTopics(CLUSTER_ID, false);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Topic::name)
                    .containsExactlyInAnyOrder("topic-1", "topic-2", "topic-3");
            verify(clusterService).existsById(CLUSTER_ID);
            verify(adminClientWrapper).listTopics(CLUSTER_ID, false);
        }

        @Test
        @DisplayName("내부 토픽을 포함하여 조회할 수 있다")
        void testListTopics_includeInternalTopics() {
            // given
            Set<String> topicNames = Set.of("topic-1", INTERNAL_TOPIC);
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.listTopics(CLUSTER_ID, true)).willReturn(topicNames);

            Map<String, TopicDescription> descriptions = createMockTopicDescriptions(topicNames, true);
            given(adminClientWrapper.describeTopics(CLUSTER_ID, topicNames)).willReturn(descriptions);

            // when
            List<Topic> result = topicService.listTopics(CLUSTER_ID, true);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(t -> t.name().equals(INTERNAL_TOPIC) && t.isInternal());
            verify(adminClientWrapper).listTopics(CLUSTER_ID, true);
        }

        @Test
        @DisplayName("토픽이 없으면 빈 목록을 반환한다")
        void testListTopics_noTopics_returnsEmptyList() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.listTopics(CLUSTER_ID, false)).willReturn(Set.of());

            // when
            List<Topic> result = topicService.listTopics(CLUSTER_ID, false);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 예외를 발생시킨다")
        void testListTopics_nonExistingCluster_throwsException() {
            // given
            given(clusterService.existsById("unknown")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> topicService.listTopics("unknown", false))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("토픽 목록에 파티션 수와 복제 팩터가 포함된다")
        void testListTopics_containsPartitionCountAndReplicationFactor() {
            // given
            Set<String> topicNames = Set.of(TOPIC_NAME);
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.listTopics(CLUSTER_ID, false)).willReturn(topicNames);

            Map<String, TopicDescription> descriptions = createMockTopicDescriptions(topicNames, 3, 2);
            given(adminClientWrapper.describeTopics(CLUSTER_ID, topicNames)).willReturn(descriptions);

            // when
            List<Topic> result = topicService.listTopics(CLUSTER_ID, false);

            // then
            assertThat(result).hasSize(1);
            Topic topic = result.get(0);
            assertThat(topic.partitionCount()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getTopic()")
    class GetTopic {

        @Test
        @DisplayName("토픽 상세 정보를 반환한다")
        void testGetTopic_returnsTopicDetail() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            TopicDescription description = createMockTopicDescription(TOPIC_NAME, 3, 2, false);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(description);

            Map<String, Map<String, String>> configs = Map.of(
                    TOPIC_NAME, Map.of(
                            "cleanup.policy", "delete",
                            "retention.ms", "604800000"
                    )
            );
            given(adminClientWrapper.describeTopicConfigs(CLUSTER_ID, Set.of(TOPIC_NAME))).willReturn(configs);

            // 파티션 오프셋 정보
            List<TopicPartition> partitions = List.of(
                    new TopicPartition(TOPIC_NAME, 0),
                    new TopicPartition(TOPIC_NAME, 1),
                    new TopicPartition(TOPIC_NAME, 2)
            );
            Map<TopicPartition, Long> beginningOffsets = Map.of(
                    partitions.get(0), 0L,
                    partitions.get(1), 0L,
                    partitions.get(2), 0L
            );
            Map<TopicPartition, Long> endOffsets = Map.of(
                    partitions.get(0), 100L,
                    partitions.get(1), 150L,
                    partitions.get(2), 200L
            );
            given(adminClientWrapper.getBeginningOffsets(CLUSTER_ID, partitions)).willReturn(beginningOffsets);
            given(adminClientWrapper.getEndOffsets(CLUSTER_ID, partitions)).willReturn(endOffsets);

            // when
            TopicDetail result = topicService.getTopic(CLUSTER_ID, TOPIC_NAME);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(TOPIC_NAME);
            assertThat(result.partitionCount()).isEqualTo(3);
            assertThat(result.replicationFactor()).isEqualTo(2);
            assertThat(result.isInternal()).isFalse();
            assertThat(result.configs()).containsEntry("cleanup.policy", "delete");
            verify(adminClientWrapper).describeTopic(CLUSTER_ID, TOPIC_NAME);
        }

        @Test
        @DisplayName("토픽 상세 정보에 파티션 정보가 포함된다")
        void testGetTopic_containsPartitionInfo() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            TopicDescription description = createMockTopicDescription(TOPIC_NAME, 2, 3, false);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(description);

            Map<String, Map<String, String>> configs = Map.of(TOPIC_NAME, Map.of());
            given(adminClientWrapper.describeTopicConfigs(CLUSTER_ID, Set.of(TOPIC_NAME))).willReturn(configs);

            List<TopicPartition> partitions = List.of(
                    new TopicPartition(TOPIC_NAME, 0),
                    new TopicPartition(TOPIC_NAME, 1)
            );
            Map<TopicPartition, Long> beginningOffsets = Map.of(
                    partitions.get(0), 0L,
                    partitions.get(1), 10L
            );
            Map<TopicPartition, Long> endOffsets = Map.of(
                    partitions.get(0), 100L,
                    partitions.get(1), 200L
            );
            given(adminClientWrapper.getBeginningOffsets(CLUSTER_ID, partitions)).willReturn(beginningOffsets);
            given(adminClientWrapper.getEndOffsets(CLUSTER_ID, partitions)).willReturn(endOffsets);

            // when
            TopicDetail result = topicService.getTopic(CLUSTER_ID, TOPIC_NAME);

            // then
            assertThat(result.partitions()).hasSize(2);

            PartitionInfo partition0 = result.partitions().stream()
                    .filter(p -> p.partition() == 0)
                    .findFirst()
                    .orElseThrow();
            assertThat(partition0.beginningOffset()).isEqualTo(0L);
            assertThat(partition0.endOffset()).isEqualTo(100L);
            assertThat(partition0.messageCount()).isEqualTo(100L);

            PartitionInfo partition1 = result.partitions().stream()
                    .filter(p -> p.partition() == 1)
                    .findFirst()
                    .orElseThrow();
            assertThat(partition1.beginningOffset()).isEqualTo(10L);
            assertThat(partition1.endOffset()).isEqualTo(200L);
            assertThat(partition1.messageCount()).isEqualTo(190L);
        }

        @Test
        @DisplayName("존재하지 않는 토픽으로 조회하면 예외를 발생시킨다")
        void testGetTopic_nonExistingTopic_throwsException() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, "unknown-topic")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> topicService.getTopic(CLUSTER_ID, "unknown-topic"))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("unknown-topic");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 예외를 발생시킨다")
        void testGetTopic_nonExistingCluster_throwsException() {
            // given
            given(clusterService.existsById("unknown")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> topicService.getTopic("unknown", TOPIC_NAME))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("getTopicPartitions()")
    class GetTopicPartitions {

        @Test
        @DisplayName("토픽의 파티션 목록을 반환한다")
        void testGetTopicPartitions_returnsPartitionList() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            TopicDescription description = createMockTopicDescription(TOPIC_NAME, 3, 2, false);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(description);

            List<TopicPartition> partitions = List.of(
                    new TopicPartition(TOPIC_NAME, 0),
                    new TopicPartition(TOPIC_NAME, 1),
                    new TopicPartition(TOPIC_NAME, 2)
            );
            Map<TopicPartition, Long> beginningOffsets = Map.of(
                    partitions.get(0), 0L,
                    partitions.get(1), 0L,
                    partitions.get(2), 0L
            );
            Map<TopicPartition, Long> endOffsets = Map.of(
                    partitions.get(0), 50L,
                    partitions.get(1), 75L,
                    partitions.get(2), 100L
            );
            given(adminClientWrapper.getBeginningOffsets(CLUSTER_ID, partitions)).willReturn(beginningOffsets);
            given(adminClientWrapper.getEndOffsets(CLUSTER_ID, partitions)).willReturn(endOffsets);

            // when
            List<PartitionInfo> result = topicService.getTopicPartitions(CLUSTER_ID, TOPIC_NAME);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(PartitionInfo::partition)
                    .containsExactlyInAnyOrder(0, 1, 2);
        }

        @Test
        @DisplayName("파티션 정보에 리더, 레플리카, ISR이 포함된다")
        void testGetTopicPartitions_containsLeaderReplicaIsr() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            TopicDescription description = createMockTopicDescription(TOPIC_NAME, 1, 3, false);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(description);

            List<TopicPartition> partitions = List.of(new TopicPartition(TOPIC_NAME, 0));
            given(adminClientWrapper.getBeginningOffsets(CLUSTER_ID, partitions)).willReturn(Map.of(partitions.get(0), 0L));
            given(adminClientWrapper.getEndOffsets(CLUSTER_ID, partitions)).willReturn(Map.of(partitions.get(0), 100L));

            // when
            List<PartitionInfo> result = topicService.getTopicPartitions(CLUSTER_ID, TOPIC_NAME);

            // then
            assertThat(result).hasSize(1);
            PartitionInfo partition = result.get(0);
            assertThat(partition.leader()).isEqualTo(0); // leader broker id
            assertThat(partition.replicas()).containsExactly(0, 1, 2);
            assertThat(partition.isr()).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("존재하지 않는 토픽으로 조회하면 예외를 발생시킨다")
        void testGetTopicPartitions_nonExistingTopic_throwsException() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);
            given(adminClientWrapper.describeTopic(CLUSTER_ID, "unknown-topic")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> topicService.getTopicPartitions(CLUSTER_ID, "unknown-topic"))
                    .isInstanceOf(TopicNotFoundException.class);
        }
    }

    // === Helper Methods ===

    private Map<String, TopicDescription> createMockTopicDescriptions(Set<String> topicNames) {
        return createMockTopicDescriptions(topicNames, false);
    }

    private Map<String, TopicDescription> createMockTopicDescriptions(Set<String> topicNames, boolean markInternal) {
        return createMockTopicDescriptions(topicNames, 1, 1, markInternal);
    }

    private Map<String, TopicDescription> createMockTopicDescriptions(Set<String> topicNames, int partitions, int replicas) {
        return createMockTopicDescriptions(topicNames, partitions, replicas, false);
    }

    private Map<String, TopicDescription> createMockTopicDescriptions(
            Set<String> topicNames, int partitions, int replicas, boolean markInternal) {
        Map<String, TopicDescription> descriptions = new HashMap<>();
        for (String topicName : topicNames) {
            boolean isInternal = markInternal && topicName.startsWith("__");
            descriptions.put(topicName, createMockTopicDescription(topicName, partitions, replicas, isInternal));
        }
        return descriptions;
    }

    private TopicDescription createMockTopicDescription(String topicName, int partitionCount, int replicaCount, boolean isInternal) {
        List<TopicPartitionInfo> partitions = new ArrayList<>();

        for (int i = 0; i < partitionCount; i++) {
            List<Node> replicas = new ArrayList<>();
            for (int r = 0; r < replicaCount; r++) {
                replicas.add(new Node(r, "broker-" + r, 9092));
            }
            Node leader = replicas.get(0);
            List<Node> isr = new ArrayList<>(replicas);

            partitions.add(new TopicPartitionInfo(i, leader, replicas, isr));
        }

        return new TopicDescription(topicName, isInternal, partitions);
    }
}
