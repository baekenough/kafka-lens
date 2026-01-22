package com.kafkalens.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkalens.common.GlobalExceptionHandler;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.consumer.ConsumerGroup;
import com.kafkalens.domain.consumer.ConsumerMember;
import com.kafkalens.domain.consumer.ConsumerLag;
import com.kafkalens.domain.consumer.ConsumerLagService;
import com.kafkalens.domain.consumer.ConsumerLagSummary;
import com.kafkalens.domain.consumer.ConsumerService;
import com.kafkalens.domain.consumer.TopicPartitionAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ConsumerController 통합 테스트.
 *
 * <p>Spring MVC 테스트 프레임워크를 사용하여 컨트롤러 레이어를 테스트합니다.
 * 서비스 레이어는 Mock으로 대체합니다.</p>
 */
@WebMvcTest(ConsumerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsumerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsumerService consumerService;

    @MockBean
    private ConsumerLagService consumerLagService;

    private ConsumerGroup orderServiceGroup;
    private ConsumerGroup paymentServiceGroup;

    @BeforeEach
    void setUp() {
        // Order service consumer group
        orderServiceGroup = ConsumerGroup.builder()
                .groupId("order-service-group")
                .state("Stable")
                .coordinator(0)
                .memberCount(2)
                .members(List.of(
                        createMember("member-1", "order-service-1", "/10.0.0.1"),
                        createMember("member-2", "order-service-2", "/10.0.0.2")
                ))
                .build();

        // Payment service consumer group
        paymentServiceGroup = ConsumerGroup.builder()
                .groupId("payment-service-group")
                .state("Stable")
                .coordinator(1)
                .memberCount(3)
                .members(List.of(
                        createMember("member-1", "payment-service-1", "/10.0.1.1"),
                        createMember("member-2", "payment-service-2", "/10.0.1.2"),
                        createMember("member-3", "payment-service-3", "/10.0.1.3")
                ))
                .build();
    }

    private ConsumerMember createMember(String memberId, String clientId, String host) {
        return ConsumerMember.builder()
                .memberId(memberId)
                .clientId(clientId)
                .host(host)
                .assignments(List.of(
                        new TopicPartitionAssignment("test-topic", 0),
                        new TopicPartitionAssignment("test-topic", 1)
                ))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/consumer-groups")
    class GetConsumerGroups {

        @Test
        @DisplayName("클러스터의 모든 컨슈머 그룹 목록을 반환한다")
        void getAllConsumerGroups_returnsGroupList() throws Exception {
            // given
            String clusterId = "local";
            List<ConsumerGroup> groups = List.of(orderServiceGroup, paymentServiceGroup);
            given(consumerService.listGroups(clusterId)).willReturn(groups);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].groupId", is("order-service-group")))
                    .andExpect(jsonPath("$.data[0].state", is("Stable")))
                    .andExpect(jsonPath("$.data[0].memberCount", is(2)))
                    .andExpect(jsonPath("$.data[1].groupId", is("payment-service-group")))
                    .andExpect(jsonPath("$.data[1].state", is("Stable")))
                    .andExpect(jsonPath("$.data[1].memberCount", is(3)))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(consumerService).listGroups(clusterId);
        }

        @Test
        @DisplayName("컨슈머 그룹이 없으면 빈 목록을 반환한다")
        void getAllConsumerGroups_noGroups_returnsEmptyList() throws Exception {
            // given
            String clusterId = "local";
            given(consumerService.listGroups(clusterId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(consumerService).listGroups(clusterId);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 404 에러를 반환한다")
        void getAllConsumerGroups_nonExistingCluster_returns404() throws Exception {
            // given
            String clusterId = "unknown";
            given(consumerService.listGroups(clusterId))
                    .willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown")));

            verify(consumerService).listGroups(clusterId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/consumer-groups/{groupId}")
    class GetConsumerGroupById {

        @Test
        @DisplayName("특정 컨슈머 그룹 상세 정보를 반환한다")
        void getConsumerGroupById_existingGroup_returnsGroup() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(consumerService.getGroup(clusterId, groupId)).willReturn(orderServiceGroup);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.groupId", is("order-service-group")))
                    .andExpect(jsonPath("$.data.state", is("Stable")))
                    .andExpect(jsonPath("$.data.coordinator", is(0)))
                    .andExpect(jsonPath("$.data.memberCount", is(2)))
                    .andExpect(jsonPath("$.data.members", hasSize(2)))
                    .andExpect(jsonPath("$.data.members[0].memberId", is("member-1")))
                    .andExpect(jsonPath("$.data.members[0].clientId", is("order-service-1")))
                    .andExpect(jsonPath("$.data.members[0].host", is("/10.0.0.1")))
                    .andExpect(jsonPath("$.data.members[0].assignments", hasSize(2)));

            verify(consumerService).getGroup(clusterId, groupId);
        }

        @Test
        @DisplayName("멤버의 파티션 할당 정보가 올바르게 포함된다")
        void getConsumerGroupById_includesMemberAssignments() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";
            given(consumerService.getGroup(clusterId, groupId)).willReturn(orderServiceGroup);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.members[0].assignments[0].topic", is("test-topic")))
                    .andExpect(jsonPath("$.data.members[0].assignments[0].partition", is(0)))
                    .andExpect(jsonPath("$.data.members[0].assignments[1].topic", is("test-topic")))
                    .andExpect(jsonPath("$.data.members[0].assignments[1].partition", is(1)));

            verify(consumerService).getGroup(clusterId, groupId);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 404 에러를 반환한다")
        void getConsumerGroupById_nonExistingCluster_returns404() throws Exception {
            // given
            String clusterId = "unknown";
            String groupId = "order-service-group";
            given(consumerService.getGroup(clusterId, groupId))
                    .willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")));

            verify(consumerService).getGroup(clusterId, groupId);
        }
    }

    @Nested
    @DisplayName("컨슈머 그룹 상태별 테스트")
    class ConsumerGroupStates {

        @Test
        @DisplayName("PreparingRebalance 상태의 그룹을 반환한다")
        void getConsumerGroup_preparingRebalance() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "rebalancing-group";
            ConsumerGroup rebalancingGroup = ConsumerGroup.builder()
                    .groupId(groupId)
                    .state("PreparingRebalance")
                    .coordinator(0)
                    .memberCount(2)
                    .members(List.of())
                    .build();

            given(consumerService.getGroup(clusterId, groupId)).willReturn(rebalancingGroup);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state", is("PreparingRebalance")));

            verify(consumerService).getGroup(clusterId, groupId);
        }

        @Test
        @DisplayName("Empty 상태의 그룹을 반환한다")
        void getConsumerGroup_empty() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "empty-group";
            ConsumerGroup emptyGroup = ConsumerGroup.builder()
                    .groupId(groupId)
                    .state("Empty")
                    .coordinator(0)
                    .memberCount(0)
                    .members(List.of())
                    .build();

            given(consumerService.getGroup(clusterId, groupId)).willReturn(emptyGroup);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state", is("Empty")))
                    .andExpect(jsonPath("$.data.memberCount", is(0)))
                    .andExpect(jsonPath("$.data.members", hasSize(0)));

            verify(consumerService).getGroup(clusterId, groupId);
        }

        @Test
        @DisplayName("Dead 상태의 그룹을 반환한다")
        void getConsumerGroup_dead() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "dead-group";
            ConsumerGroup deadGroup = ConsumerGroup.builder()
                    .groupId(groupId)
                    .state("Dead")
                    .coordinator(0)
                    .memberCount(0)
                    .members(List.of())
                    .build();

            given(consumerService.getGroup(clusterId, groupId)).willReturn(deadGroup);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}", clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state", is("Dead")));

            verify(consumerService).getGroup(clusterId, groupId);
        }
    }

    @Nested
    @DisplayName("응답 형식 검증")
    class ResponseFormat {

        @Test
        @DisplayName("응답에 timestamp가 포함된다")
        void response_containsTimestamp() throws Exception {
            // given
            String clusterId = "local";
            given(consumerService.listGroups(clusterId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("성공 응답의 success 필드는 true이다")
        void successResponse_hasSuccessTrue() throws Exception {
            // given
            String clusterId = "local";
            given(consumerService.listGroups(clusterId)).willReturn(List.of(orderServiceGroup));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        @DisplayName("에러 응답의 success 필드는 false이다")
        void errorResponse_hasSuccessFalse() throws Exception {
            // given
            String clusterId = "unknown";
            given(consumerService.listGroups(clusterId))
                    .willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups", clusterId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag")
    class GetConsumerGroupLag {

        @Test
        @DisplayName("컨슈머 그룹의 Lag 정보를 반환한다")
        void getLag_returnsLagInfo() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";

            List<ConsumerLag> partitionLags = List.of(
                    ConsumerLag.of(groupId, "topic-a", 0, 100L, 150L),
                    ConsumerLag.of(groupId, "topic-a", 1, 200L, 300L)
            );
            ConsumerLagSummary summary = ConsumerLagSummary.of(groupId, partitionLags);

            given(consumerLagService.getLagSummary(clusterId, groupId)).willReturn(summary);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag",
                            clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.groupId", is(groupId)))
                    .andExpect(jsonPath("$.data.totalLag", is(150)))  // 50 + 100
                    .andExpect(jsonPath("$.data.partitionCount", is(2)))
                    .andExpect(jsonPath("$.data.topics", hasSize(1)))
                    .andExpect(jsonPath("$.data.topics[0].topic", is("topic-a")))
                    .andExpect(jsonPath("$.data.topics[0].partitions", hasSize(2)));

            verify(consumerLagService).getLagSummary(clusterId, groupId);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터의 Lag 조회 시 404를 반환한다")
        void getLag_clusterNotFound_returns404() throws Exception {
            // given
            String clusterId = "unknown";
            String groupId = "order-service-group";

            given(consumerLagService.getLagSummary(clusterId, groupId))
                    .willThrow(new ClusterNotFoundException(clusterId));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag",
                            clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")));

            verify(consumerLagService).getLagSummary(clusterId, groupId);
        }

        @Test
        @DisplayName("빈 컨슈머 그룹의 Lag 정보를 반환한다")
        void getLag_emptyGroup_returnsEmptySummary() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "empty-group";

            ConsumerLagSummary summary = ConsumerLagSummary.empty(groupId);

            given(consumerLagService.getLagSummary(clusterId, groupId)).willReturn(summary);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag",
                            clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.groupId", is(groupId)))
                    .andExpect(jsonPath("$.data.totalLag", is(0)))
                    .andExpect(jsonPath("$.data.partitionCount", is(0)))
                    .andExpect(jsonPath("$.data.topics", hasSize(0)));

            verify(consumerLagService).getLagSummary(clusterId, groupId);
        }

        @Test
        @DisplayName("warning 상태의 Lag 정보를 올바르게 표시한다")
        void getLag_warningLag_showsWarningStatus() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";

            List<ConsumerLag> partitionLags = List.of(
                    ConsumerLag.of(groupId, "topic-a", 0, 0L, 1500L)  // lag = 1500, warning
            );
            ConsumerLagSummary summary = ConsumerLagSummary.of(groupId, partitionLags);

            given(consumerLagService.getLagSummary(clusterId, groupId)).willReturn(summary);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag",
                            clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.warningCount", is(1)))
                    .andExpect(jsonPath("$.data.topics[0].partitions[0].lag", is(1500)))
                    .andExpect(jsonPath("$.data.topics[0].partitions[0].status", is("warning")));

            verify(consumerLagService).getLagSummary(clusterId, groupId);
        }

        @Test
        @DisplayName("여러 토픽의 Lag 정보를 모두 반환한다")
        void getLag_multipleTopics_returnsAll() throws Exception {
            // given
            String clusterId = "local";
            String groupId = "order-service-group";

            List<ConsumerLag> partitionLags = List.of(
                    ConsumerLag.of(groupId, "topic-a", 0, 100L, 200L),  // lag = 100
                    ConsumerLag.of(groupId, "topic-b", 0, 50L, 150L)    // lag = 100
            );
            ConsumerLagSummary summary = ConsumerLagSummary.of(groupId, partitionLags);

            given(consumerLagService.getLagSummary(clusterId, groupId)).willReturn(summary);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag",
                            clusterId, groupId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalLag", is(200)))  // 100 + 100
                    .andExpect(jsonPath("$.data.topics", hasSize(2)));

            verify(consumerLagService).getLagSummary(clusterId, groupId);
        }
    }
}
