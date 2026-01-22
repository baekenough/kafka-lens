package com.kafkalens.domain.consumer;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ConsumerService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, ConsumerService의 핵심 비즈니스 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConsumerServiceTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private AdminClientWrapper adminClientWrapper;

    private ConsumerService consumerService;

    private Cluster testCluster;

    @BeforeEach
    void setUp() {
        consumerService = new ConsumerService(clusterService, adminClientWrapper);

        testCluster = Cluster.builder()
                .id("local")
                .name("Local Development")
                .description("Local Kafka cluster for development")
                .environment("development")
                .bootstrapServers("localhost:9092")
                .build();
    }

    @Nested
    @DisplayName("listGroups()")
    class ListGroups {

        @Test
        @DisplayName("클러스터의 모든 컨슈머 그룹 목록을 반환한다")
        void testListGroups_returnsAllGroups() {
            // given
            String clusterId = "local";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupListing group1 = createConsumerGroupListing("order-service-group", false);
            ConsumerGroupListing group2 = createConsumerGroupListing("payment-service-group", false);
            Collection<ConsumerGroupListing> listings = List.of(group1, group2);

            given(adminClientWrapper.listConsumerGroups(clusterId)).willReturn(listings);

            // Mock describeConsumerGroups to return descriptions
            ConsumerGroupDescription desc1 = createConsumerGroupDescription(
                    "order-service-group", ConsumerGroupState.STABLE, 2);
            ConsumerGroupDescription desc2 = createConsumerGroupDescription(
                    "payment-service-group", ConsumerGroupState.STABLE, 3);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(
                    "order-service-group", desc1,
                    "payment-service-group", desc2
            );
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            List<ConsumerGroup> result = consumerService.listGroups(clusterId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ConsumerGroup::groupId)
                    .containsExactlyInAnyOrder("order-service-group", "payment-service-group");
            verify(clusterService).findById(clusterId);
            verify(adminClientWrapper).listConsumerGroups(clusterId);
        }

        @Test
        @DisplayName("컨슈머 그룹이 없으면 빈 목록을 반환한다")
        void testListGroups_noGroups_returnsEmptyList() {
            // given
            String clusterId = "local";
            given(clusterService.findById(clusterId)).willReturn(testCluster);
            given(adminClientWrapper.listConsumerGroups(clusterId)).willReturn(List.of());

            // when
            List<ConsumerGroup> result = consumerService.listGroups(clusterId);

            // then
            assertThat(result).isEmpty();
            verify(clusterService).findById(clusterId);
            verify(adminClientWrapper).listConsumerGroups(clusterId);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 예외를 발생시킨다")
        void testListGroups_nonExistingCluster_throwsException() {
            // given
            String clusterId = "unknown";
            given(clusterService.findById(clusterId)).willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            assertThatThrownBy(() -> consumerService.listGroups(clusterId))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
            verify(clusterService).findById(clusterId);
        }
    }

    @Nested
    @DisplayName("getGroup()")
    class GetGroup {

        @Test
        @DisplayName("특정 컨슈머 그룹 상세 정보를 반환한다")
        void testGetGroup_returnsGroupDetail() {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupDescription description = createConsumerGroupDescription(
                    groupId, ConsumerGroupState.STABLE, 2);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(groupId, description);
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            ConsumerGroup result = consumerService.getGroup(clusterId, groupId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.groupId()).isEqualTo(groupId);
            assertThat(result.state()).isEqualTo("Stable");
            assertThat(result.memberCount()).isEqualTo(2);
            verify(clusterService).findById(clusterId);
            verify(adminClientWrapper).describeConsumerGroups(eq(clusterId), anyCollection());
        }

        @Test
        @DisplayName("그룹에 coordinator 정보가 포함된다")
        void testGetGroup_includesCoordinator() {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupDescription description = createConsumerGroupDescription(
                    groupId, ConsumerGroupState.STABLE, 1);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(groupId, description);
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            ConsumerGroup result = consumerService.getGroup(clusterId, groupId);

            // then
            assertThat(result.coordinator()).isNotNull();
            assertThat(result.coordinator()).isEqualTo(0);  // Mock coordinator node ID
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 예외를 발생시킨다")
        void testGetGroup_nonExistingCluster_throwsException() {
            // given
            String clusterId = "unknown";
            String groupId = "order-service-group";
            given(clusterService.findById(clusterId)).willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            assertThatThrownBy(() -> consumerService.getGroup(clusterId, groupId))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
            verify(clusterService).findById(clusterId);
        }
    }

    @Nested
    @DisplayName("getMembers()")
    class GetMembers {

        @Test
        @DisplayName("컨슈머 그룹의 멤버 목록을 반환한다")
        void testGetMembers_returnsMembersList() {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupDescription description = createConsumerGroupDescription(
                    groupId, ConsumerGroupState.STABLE, 2);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(groupId, description);
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            List<ConsumerMember> result = consumerService.getMembers(clusterId, groupId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ConsumerMember::clientId)
                    .containsExactlyInAnyOrder("client-0", "client-1");
            verify(clusterService).findById(clusterId);
        }

        @Test
        @DisplayName("멤버 정보에는 할당된 파티션이 포함된다")
        void testGetMembers_includesAssignments() {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupDescription description = createConsumerGroupDescription(
                    groupId, ConsumerGroupState.STABLE, 1);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(groupId, description);
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            List<ConsumerMember> result = consumerService.getMembers(clusterId, groupId);

            // then
            assertThat(result).hasSize(1);
            ConsumerMember member = result.get(0);
            assertThat(member.assignments()).isNotNull();
            assertThat(member.assignments()).isNotEmpty();
        }

        @Test
        @DisplayName("Empty 상태의 그룹은 빈 멤버 목록을 반환한다")
        void testGetMembers_emptyGroup_returnsEmptyList() {
            // given
            String clusterId = "local";
            String groupId = "empty-group";
            given(clusterService.findById(clusterId)).willReturn(testCluster);

            ConsumerGroupDescription description = createConsumerGroupDescription(
                    groupId, ConsumerGroupState.EMPTY, 0);
            Map<String, ConsumerGroupDescription> descriptions = Map.of(groupId, description);
            given(adminClientWrapper.describeConsumerGroups(eq(clusterId), anyCollection()))
                    .willReturn(descriptions);

            // when
            List<ConsumerMember> result = consumerService.getMembers(clusterId, groupId);

            // then
            assertThat(result).isEmpty();
        }
    }

    // === Helper Methods ===

    /**
     * ConsumerGroupListing Mock 생성
     */
    private ConsumerGroupListing createConsumerGroupListing(String groupId, boolean isSimple) {
        return new ConsumerGroupListing(groupId, isSimple);
    }

    /**
     * ConsumerGroupDescription Mock 생성
     */
    private ConsumerGroupDescription createConsumerGroupDescription(
            String groupId, ConsumerGroupState state, int memberCount) {

        Node coordinator = new Node(0, "localhost", 9092);

        List<MemberDescription> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            Set<TopicPartition> partitions = Set.of(
                    new TopicPartition("test-topic", i)
            );
            MemberAssignment assignment = new MemberAssignment(partitions);

            MemberDescription member = new MemberDescription(
                    "member-" + i,
                    Optional.of("instance-" + i),
                    "client-" + i,
                    "/127.0.0.1",
                    assignment
            );
            members.add(member);
        }

        return new ConsumerGroupDescription(
                groupId,
                true,  // isSimpleConsumerGroup
                members,
                "range",     // partition assignor
                state,
                coordinator
        );
    }
}
