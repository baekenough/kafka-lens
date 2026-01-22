package com.kafkalens.infrastructure.kafka;

import com.kafkalens.common.exception.KafkaConnectionException;
import com.kafkalens.common.exception.KafkaTimeoutException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminClientWrapper 테스트 클래스.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminClientWrapper")
class AdminClientWrapperTest {

    @Mock
    private AdminClientFactory adminClientFactory;

    @Mock
    private AdminClient adminClient;

    private AdminClientWrapper wrapper;

    private static final String CLUSTER_ID = "test-cluster";

    @BeforeEach
    void setUp() {
        wrapper = new AdminClientWrapper(adminClientFactory, 30000);
        when(adminClientFactory.getOrCreate(CLUSTER_ID)).thenReturn(adminClient);
    }

    @Nested
    @DisplayName("토픽 작업 테스트")
    class TopicOperationsTest {

        @Test
        @DisplayName("토픽 목록을 조회할 수 있다")
        void shouldListTopics() throws Exception {
            // given
            Set<String> expectedTopics = Set.of("topic1", "topic2", "topic3");

            ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
            KafkaFuture<Set<String>> future = mock(KafkaFuture.class);

            when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
            when(listTopicsResult.names()).thenReturn(future);
            when(future.get()).thenReturn(expectedTopics);

            // when
            Set<String> topics = wrapper.listTopics(CLUSTER_ID);

            // then
            assertEquals(expectedTopics, topics);
            verify(adminClient).listTopics(any(ListTopicsOptions.class));
        }

        @Test
        @DisplayName("내부 토픽을 포함하여 조회할 수 있다")
        void shouldListTopicsIncludingInternal() throws Exception {
            // given
            Set<String> expectedTopics = Set.of("topic1", "__consumer_offsets");

            ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
            KafkaFuture<Set<String>> future = mock(KafkaFuture.class);

            when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
            when(listTopicsResult.names()).thenReturn(future);
            when(future.get()).thenReturn(expectedTopics);

            // when
            Set<String> topics = wrapper.listTopics(CLUSTER_ID, true);

            // then
            assertEquals(expectedTopics, topics);
        }

        @Test
        @DisplayName("토픽 상세 정보를 조회할 수 있다")
        void shouldDescribeTopics() throws Exception {
            // given
            String topicName = "test-topic";
            TopicDescription description = new TopicDescription(
                    topicName, false, Collections.emptyList()
            );

            DescribeTopicsResult describeResult = mock(DescribeTopicsResult.class);
            KafkaFuture<Map<String, TopicDescription>> future = mock(KafkaFuture.class);

            when(adminClient.describeTopics(anyCollection())).thenReturn(describeResult);
            when(describeResult.allTopicNames()).thenReturn(future);
            when(future.get()).thenReturn(Map.of(topicName, description));

            // when
            Map<String, TopicDescription> result = wrapper.describeTopics(CLUSTER_ID, List.of(topicName));

            // then
            assertEquals(1, result.size());
            assertTrue(result.containsKey(topicName));
        }

        @Test
        @DisplayName("단일 토픽을 조회할 수 있다")
        void shouldDescribeSingleTopic() throws Exception {
            // given
            String topicName = "single-topic";
            TopicDescription description = new TopicDescription(
                    topicName, false, Collections.emptyList()
            );

            DescribeTopicsResult describeResult = mock(DescribeTopicsResult.class);
            KafkaFuture<Map<String, TopicDescription>> future = mock(KafkaFuture.class);

            when(adminClient.describeTopics(anyCollection())).thenReturn(describeResult);
            when(describeResult.allTopicNames()).thenReturn(future);
            when(future.get()).thenReturn(Map.of(topicName, description));

            // when
            TopicDescription result = wrapper.describeTopic(CLUSTER_ID, topicName);

            // then
            assertNotNull(result);
            assertEquals(topicName, result.name());
        }
    }

    @Nested
    @DisplayName("컨슈머 그룹 작업 테스트")
    class ConsumerGroupOperationsTest {

        @Test
        @DisplayName("컨슈머 그룹 목록을 조회할 수 있다")
        void shouldListConsumerGroups() throws Exception {
            // given
            ConsumerGroupListing group1 = new ConsumerGroupListing("group1", false);
            ConsumerGroupListing group2 = new ConsumerGroupListing("group2", false);

            ListConsumerGroupsResult listResult = mock(ListConsumerGroupsResult.class);
            KafkaFuture<Collection<ConsumerGroupListing>> future = mock(KafkaFuture.class);

            when(adminClient.listConsumerGroups()).thenReturn(listResult);
            when(listResult.all()).thenReturn(future);
            when(future.get()).thenReturn(List.of(group1, group2));

            // when
            Collection<ConsumerGroupListing> groups = wrapper.listConsumerGroups(CLUSTER_ID);

            // then
            assertEquals(2, groups.size());
        }

        @Test
        @DisplayName("컨슈머 그룹 상세 정보를 조회할 수 있다")
        void shouldDescribeConsumerGroups() throws Exception {
            // given
            String groupId = "test-group";
            ConsumerGroupDescription description = new ConsumerGroupDescription(
                    groupId,
                    false,
                    Collections.emptyList(),
                    "",
                    ConsumerGroupState.STABLE,
                    new Node(0, "localhost", 9092)
            );

            DescribeConsumerGroupsResult describeResult = mock(DescribeConsumerGroupsResult.class);
            KafkaFuture<Map<String, ConsumerGroupDescription>> future = mock(KafkaFuture.class);

            when(adminClient.describeConsumerGroups(anyCollection())).thenReturn(describeResult);
            when(describeResult.all()).thenReturn(future);
            when(future.get()).thenReturn(Map.of(groupId, description));

            // when
            Map<String, ConsumerGroupDescription> result = wrapper.describeConsumerGroups(CLUSTER_ID, List.of(groupId));

            // then
            assertEquals(1, result.size());
            assertTrue(result.containsKey(groupId));
        }
    }

    @Nested
    @DisplayName("클러스터 정보 조회 테스트")
    class ClusterInfoTest {

        @Test
        @DisplayName("클러스터 정보를 조회할 수 있다")
        void shouldDescribeCluster() throws Exception {
            // given
            String kafkaClusterId = "kafka-cluster-id";
            Node controller = new Node(0, "controller.example.com", 9092);
            Collection<Node> nodes = List.of(
                    controller,
                    new Node(1, "broker1.example.com", 9092),
                    new Node(2, "broker2.example.com", 9092)
            );

            DescribeClusterResult describeResult = mock(DescribeClusterResult.class);
            KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);
            KafkaFuture<Node> controllerFuture = mock(KafkaFuture.class);
            KafkaFuture<Collection<Node>> nodesFuture = mock(KafkaFuture.class);

            when(adminClient.describeCluster()).thenReturn(describeResult);
            when(describeResult.clusterId()).thenReturn(clusterIdFuture);
            when(describeResult.controller()).thenReturn(controllerFuture);
            when(describeResult.nodes()).thenReturn(nodesFuture);
            when(clusterIdFuture.get()).thenReturn(kafkaClusterId);
            when(controllerFuture.get()).thenReturn(controller);
            when(nodesFuture.get()).thenReturn(nodes);

            // when
            AdminClientWrapper.ClusterInfo info = wrapper.describeCluster(CLUSTER_ID);

            // then
            assertEquals(kafkaClusterId, info.clusterId());
            assertEquals(controller, info.controller());
            assertEquals(3, info.brokerCount());
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("타임아웃 발생 시 KafkaTimeoutException을 던진다")
        void shouldThrowKafkaTimeoutExceptionOnTimeout() throws Exception {
            // given
            ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
            KafkaFuture<Set<String>> future = mock(KafkaFuture.class);

            when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
            when(listTopicsResult.names()).thenReturn(future);
            when(future.get()).thenThrow(new ExecutionException(new TimeoutException("Timed out")));

            // when & then
            assertThrows(KafkaTimeoutException.class, () -> wrapper.listTopics(CLUSTER_ID));
        }

        @Test
        @DisplayName("기타 예외 발생 시 KafkaConnectionException을 던진다")
        void shouldThrowKafkaConnectionExceptionOnOtherError() throws Exception {
            // given
            ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
            KafkaFuture<Set<String>> future = mock(KafkaFuture.class);

            when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
            when(listTopicsResult.names()).thenReturn(future);
            when(future.get()).thenThrow(new ExecutionException(new RuntimeException("Connection failed")));

            // when & then
            assertThrows(KafkaConnectionException.class, () -> wrapper.listTopics(CLUSTER_ID));
        }

        @Test
        @DisplayName("InterruptedException 발생 시 스레드 인터럽트 상태가 복원된다")
        void shouldRestoreInterruptStatusOnInterruptedException() throws Exception {
            // given
            ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
            KafkaFuture<Set<String>> future = mock(KafkaFuture.class);

            when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
            when(listTopicsResult.names()).thenReturn(future);
            when(future.get()).thenThrow(new InterruptedException("Interrupted"));

            // when & then
            assertThrows(KafkaConnectionException.class, () -> wrapper.listTopics(CLUSTER_ID));
            assertTrue(Thread.currentThread().isInterrupted());

            // Clean up interrupt status
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("ClusterInfo 레코드 테스트")
    class ClusterInfoRecordTest {

        @Test
        @DisplayName("브로커 수를 계산할 수 있다")
        void shouldCalculateBrokerCount() {
            // given
            Collection<Node> nodes = List.of(
                    new Node(0, "host1", 9092),
                    new Node(1, "host2", 9092),
                    new Node(2, "host3", 9092)
            );

            AdminClientWrapper.ClusterInfo info = new AdminClientWrapper.ClusterInfo(
                    "cluster-id",
                    nodes.iterator().next(),
                    nodes
            );

            // then
            assertEquals(3, info.brokerCount());
        }

        @Test
        @DisplayName("노드가 null이면 브로커 수는 0이다")
        void shouldReturnZeroWhenNodesIsNull() {
            // given
            AdminClientWrapper.ClusterInfo info = new AdminClientWrapper.ClusterInfo(
                    "cluster-id",
                    null,
                    null
            );

            // then
            assertEquals(0, info.brokerCount());
        }
    }
}
