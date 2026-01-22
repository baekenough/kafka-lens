package com.kafkalens.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkalens.common.GlobalExceptionHandler;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.TopicNotFoundException;
import com.kafkalens.domain.topic.PartitionInfo;
import com.kafkalens.domain.topic.Topic;
import com.kafkalens.domain.topic.TopicDetail;
import com.kafkalens.domain.topic.TopicService;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TopicController 통합 테스트.
 *
 * <p>Spring MVC 테스트 프레임워크를 사용하여 컨트롤러 레이어를 테스트합니다.
 * 서비스 레이어는 Mock으로 대체합니다.</p>
 *
 * <p>테스트 API:</p>
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/topics - 토픽 목록 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/topics/{topicName} - 토픽 상세 조회</li>
 * </ul>
 */
@WebMvcTest(TopicController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    private static final String CLUSTER_ID = "local";
    private static final String TOPIC_NAME = "test-topic";

    private Topic topic1;
    private Topic topic2;
    private TopicDetail topicDetail;
    private List<PartitionInfo> partitions;

    @BeforeEach
    void setUp() {
        topic1 = new Topic(
                "orders",
                3,
                2,
                false
        );

        topic2 = new Topic(
                "users",
                5,
                3,
                false
        );

        partitions = List.of(
                new PartitionInfo(0, 1, List.of(1, 2, 3), List.of(1, 2, 3), 0L, 1000L),
                new PartitionInfo(1, 2, List.of(2, 3, 1), List.of(2, 3, 1), 0L, 800L),
                new PartitionInfo(2, 3, List.of(3, 1, 2), List.of(3, 1, 2), 0L, 1200L)
        );

        Map<String, String> configs = Map.of(
                "cleanup.policy", "delete",
                "retention.ms", "604800000",
                "segment.bytes", "1073741824"
        );

        topicDetail = new TopicDetail(
                TOPIC_NAME,
                3,
                3,
                false,
                partitions,
                configs
        );
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/topics")
    class GetTopics {

        @Test
        @DisplayName("클러스터의 모든 토픽 목록을 반환한다")
        void getTopics_returnsTopicList() throws Exception {
            // given
            List<Topic> topics = List.of(topic1, topic2);
            given(topicService.listTopics(CLUSTER_ID, false)).willReturn(topics);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics", CLUSTER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name", is("orders")))
                    .andExpect(jsonPath("$.data[0].partitionCount", is(3)))
                    .andExpect(jsonPath("$.data[0].replicationFactor", is(2)))
                    .andExpect(jsonPath("$.data[0].internal", is(false)))
                    .andExpect(jsonPath("$.data[1].name", is("users")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(topicService).listTopics(CLUSTER_ID, false);
        }

        @Test
        @DisplayName("토픽이 없으면 빈 목록을 반환한다")
        void getTopics_noTopics_returnsEmptyList() throws Exception {
            // given
            given(topicService.listTopics(CLUSTER_ID, false)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics", CLUSTER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(topicService).listTopics(CLUSTER_ID, false);
        }

        @Test
        @DisplayName("includeInternal=true로 내부 토픽을 포함하여 조회한다")
        void getTopics_includeInternal_returnsInternalTopics() throws Exception {
            // given
            Topic internalTopic = new Topic("__consumer_offsets", 50, 3, true);
            List<Topic> topics = List.of(topic1, internalTopic);
            given(topicService.listTopics(CLUSTER_ID, true)).willReturn(topics);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics", CLUSTER_ID)
                            .param("includeInternal", "true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[?(@.name=='__consumer_offsets')].internal", contains(true)));

            verify(topicService).listTopics(CLUSTER_ID, true);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 404 에러를 반환한다")
        void getTopics_nonExistingCluster_returns404() throws Exception {
            // given
            given(topicService.listTopics("unknown", false))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics", "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown")));

            verify(topicService).listTopics("unknown", false);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/topics/{topicName}")
    class GetTopicByName {

        @Test
        @DisplayName("토픽 상세 정보를 반환한다")
        void getTopic_returnsTopicDetail() throws Exception {
            // given
            given(topicService.getTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(topicDetail);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is(TOPIC_NAME)))
                    .andExpect(jsonPath("$.data.partitionCount", is(3)))
                    .andExpect(jsonPath("$.data.replicationFactor", is(3)))
                    .andExpect(jsonPath("$.data.internal", is(false)))
                    .andExpect(jsonPath("$.data.configs", notNullValue()))
                    .andExpect(jsonPath("$.data.configs['cleanup.policy']", is("delete")))
                    .andExpect(jsonPath("$.data.configs['retention.ms']", is("604800000")));

            verify(topicService).getTopic(CLUSTER_ID, TOPIC_NAME);
        }

        @Test
        @DisplayName("토픽 상세 정보에 파티션 목록이 포함된다")
        void getTopic_containsPartitions() throws Exception {
            // given
            given(topicService.getTopic(CLUSTER_ID, TOPIC_NAME)).willReturn(topicDetail);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.partitions", hasSize(3)))
                    .andExpect(jsonPath("$.data.partitions[0].partition", is(0)))
                    .andExpect(jsonPath("$.data.partitions[0].leader", is(1)))
                    .andExpect(jsonPath("$.data.partitions[0].replicas", hasSize(3)))
                    .andExpect(jsonPath("$.data.partitions[0].isr", hasSize(3)))
                    .andExpect(jsonPath("$.data.partitions[0].beginningOffset", is(0)))
                    .andExpect(jsonPath("$.data.partitions[0].endOffset", is(1000)))
                    .andExpect(jsonPath("$.data.partitions[0].messageCount", is(1000)));

            verify(topicService).getTopic(CLUSTER_ID, TOPIC_NAME);
        }

        @Test
        @DisplayName("존재하지 않는 토픽으로 조회하면 404 에러를 반환한다")
        void getTopic_nonExistingTopic_returns404() throws Exception {
            // given
            given(topicService.getTopic(CLUSTER_ID, "unknown-topic"))
                    .willThrow(new TopicNotFoundException(CLUSTER_ID, "unknown-topic"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", CLUSTER_ID, "unknown-topic")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("TOPIC_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown-topic")));

            verify(topicService).getTopic(CLUSTER_ID, "unknown-topic");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 404 에러를 반환한다")
        void getTopic_nonExistingCluster_returns404() throws Exception {
            // given
            given(topicService.getTopic("unknown", TOPIC_NAME))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", "unknown", TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")));

            verify(topicService).getTopic("unknown", TOPIC_NAME);
        }
    }

    @Nested
    @DisplayName("에러 응답 형식")
    class ErrorResponseFormat {

        @Test
        @DisplayName("에러 응답에는 timestamp가 포함된다")
        void errorResponse_containsTimestamp() throws Exception {
            // given
            given(topicService.getTopic(CLUSTER_ID, "unknown"))
                    .willThrow(new TopicNotFoundException(CLUSTER_ID, "unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", CLUSTER_ID, "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("토픽 에러 응답에는 clusterId와 topicName이 포함된다")
        void topicErrorResponse_containsDetails() throws Exception {
            // given
            given(topicService.getTopic(CLUSTER_ID, "unknown"))
                    .willThrow(new TopicNotFoundException(CLUSTER_ID, "unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}", CLUSTER_ID, "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.error.details.clusterId", is(CLUSTER_ID)))
                    .andExpect(jsonPath("$.error.details.topicName", is("unknown")));
        }
    }
}
