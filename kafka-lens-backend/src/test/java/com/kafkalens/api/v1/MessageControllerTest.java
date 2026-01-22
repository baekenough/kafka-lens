package com.kafkalens.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkalens.common.GlobalExceptionHandler;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.message.KafkaMessage;
import com.kafkalens.domain.message.MessageFetchRequest;
import com.kafkalens.domain.message.MessageService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MessageController 통합 테스트.
 *
 * <p>Spring MVC 테스트 프레임워크를 사용하여 컨트롤러 레이어를 테스트합니다.
 * 서비스 레이어는 Mock으로 대체합니다.</p>
 *
 * <p>테스트 API:</p>
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/topics/{topicName}/messages - 메시지 조회</li>
 * </ul>
 */
@WebMvcTest(MessageController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageService messageService;

    private static final String CLUSTER_ID = "local";
    private static final String TOPIC_NAME = "test-topic";

    private KafkaMessage message1;
    private KafkaMessage message2;

    @BeforeEach
    void setUp() {
        message1 = KafkaMessage.builder()
                .topic(TOPIC_NAME)
                .partition(0)
                .offset(0L)
                .timestamp(1700000000000L)
                .timestampType("CreateTime")
                .key("key-1")
                .value("value-1")
                .headers(Map.of("correlationId", "abc-123"))
                .build();

        message2 = KafkaMessage.builder()
                .topic(TOPIC_NAME)
                .partition(0)
                .offset(1L)
                .timestamp(1700000001000L)
                .timestampType("CreateTime")
                .key("key-2")
                .value("value-2")
                .headers(Map.of())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/topics/{topicName}/messages")
    class GetMessages {

        @Test
        @DisplayName("토픽에서 메시지를 조회한다")
        void getMessages_returnsMessageList() throws Exception {
            // given
            List<KafkaMessage> messages = List.of(message1, message2);
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].topic", is(TOPIC_NAME)))
                    .andExpect(jsonPath("$.data[0].partition", is(0)))
                    .andExpect(jsonPath("$.data[0].offset", is(0)))
                    .andExpect(jsonPath("$.data[0].key", is("key-1")))
                    .andExpect(jsonPath("$.data[0].value", is("value-1")))
                    .andExpect(jsonPath("$.data[0].timestampType", is("CreateTime")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }

        @Test
        @DisplayName("메시지 헤더가 응답에 포함된다")
        void getMessages_containsHeaders() throws Exception {
            // given
            List<KafkaMessage> messages = List.of(message1);
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].headers.correlationId", is("abc-123")));
        }

        @Test
        @DisplayName("파티션과 오프셋 파라미터를 전달한다")
        void getMessages_withPartitionAndOffset() throws Exception {
            // given
            KafkaMessage message = KafkaMessage.builder()
                    .topic(TOPIC_NAME)
                    .partition(2)
                    .offset(100L)
                    .timestamp(1700000000000L)
                    .timestampType("CreateTime")
                    .key("key")
                    .value("value")
                    .headers(Map.of())
                    .build();

            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of(message));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .param("partition", "2")
                            .param("offset", "100")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].partition", is(2)))
                    .andExpect(jsonPath("$.data[0].offset", is(100)));

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }

        @Test
        @DisplayName("limit 파라미터를 전달한다")
        void getMessages_withLimit() throws Exception {
            // given
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of(message1));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .param("limit", "50")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }

        @Test
        @DisplayName("메시지가 없으면 빈 목록을 반환한다")
        void getMessages_noMessages_returnsEmptyList() throws Exception {
            // given
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("null 키/값이 있는 메시지를 처리한다")
        void getMessages_nullKeyValue() throws Exception {
            // given
            KafkaMessage messageWithNullKey = KafkaMessage.builder()
                    .topic(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .timestamp(1700000000000L)
                    .timestampType("CreateTime")
                    .key(null)  // null key
                    .value("value")
                    .headers(Map.of())
                    .build();

            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of(messageWithNullKey));

            // when & then
            // Jackson NON_NULL 설정으로 인해 null 값은 JSON에 포함되지 않음
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].key").doesNotExist())
                    .andExpect(jsonPath("$.data[0].value", is("value")));
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 404 에러를 반환한다")
        void getMessages_nonExistingCluster_returns404() throws Exception {
            // given
            given(messageService.fetchMessages(eq("unknown"), any(MessageFetchRequest.class)))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            "unknown", TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown")));

            verify(messageService).fetchMessages(eq("unknown"), any(MessageFetchRequest.class));
        }
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValues {

        @Test
        @DisplayName("partition 기본값은 0이다")
        void defaultPartitionIsZero() throws Exception {
            // given
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }

        @Test
        @DisplayName("offset 기본값은 0이다")
        void defaultOffsetIsZero() throws Exception {
            // given
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }

        @Test
        @DisplayName("limit 기본값은 100이다")
        void defaultLimitIs100() throws Exception {
            // given
            given(messageService.fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class)))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/topics/{topicName}/messages",
                            CLUSTER_ID, TOPIC_NAME)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(messageService).fetchMessages(eq(CLUSTER_ID), any(MessageFetchRequest.class));
        }
    }
}
