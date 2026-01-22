package com.kafkalens.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkalens.common.GlobalExceptionHandler;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.broker.Broker;
import com.kafkalens.domain.broker.BrokerService;
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
 * BrokerController 통합 테스트.
 *
 * <p>Spring MVC 테스트 프레임워크를 사용하여 컨트롤러 레이어를 테스트합니다.
 * 서비스 레이어는 Mock으로 대체합니다.</p>
 *
 * <p>테스트 API:</p>
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/brokers - 브로커 목록 조회</li>
 * </ul>
 */
@WebMvcTest(BrokerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class BrokerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BrokerService brokerService;

    private static final String CLUSTER_ID = "local";

    @Nested
    @DisplayName("GET /api/v1/clusters/{clusterId}/brokers")
    class GetBrokers {

        @Test
        @DisplayName("클러스터의 모든 브로커 목록을 반환한다")
        void getBrokers_returnsBrokerList() throws Exception {
            // given
            List<Broker> brokers = List.of(
                    new Broker(0, "broker-0", 9092, "rack-a", true),
                    new Broker(1, "broker-1", 9092, "rack-b", false),
                    new Broker(2, "broker-2", 9092, "rack-c", false)
            );
            given(brokerService.listBrokers(CLUSTER_ID)).willReturn(brokers);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", CLUSTER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].id", is(0)))
                    .andExpect(jsonPath("$.data[0].host", is("broker-0")))
                    .andExpect(jsonPath("$.data[0].port", is(9092)))
                    .andExpect(jsonPath("$.data[0].rack", is("rack-a")))
                    .andExpect(jsonPath("$.data[0].controller", is(true)))
                    .andExpect(jsonPath("$.data[1].id", is(1)))
                    .andExpect(jsonPath("$.data[1].controller", is(false)))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(brokerService).listBrokers(CLUSTER_ID);
        }

        @Test
        @DisplayName("브로커가 없으면 빈 목록을 반환한다")
        void getBrokers_noBrokers_returnsEmptyList() throws Exception {
            // given
            given(brokerService.listBrokers(CLUSTER_ID)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", CLUSTER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(brokerService).listBrokers(CLUSTER_ID);
        }

        @Test
        @DisplayName("랙 정보가 없는 브로커는 rack이 null이다")
        void getBrokers_noRack_returnsNullRack() throws Exception {
            // given
            List<Broker> brokers = List.of(
                    new Broker(0, "broker-0", 9092, null, true)
            );
            given(brokerService.listBrokers(CLUSTER_ID)).willReturn(brokers);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", CLUSTER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].rack").doesNotExist());

            verify(brokerService).listBrokers(CLUSTER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 404 에러를 반환한다")
        void getBrokers_nonExistingCluster_returns404() throws Exception {
            // given
            given(brokerService.listBrokers("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown")));

            verify(brokerService).listBrokers("unknown");
        }
    }

    @Nested
    @DisplayName("에러 응답 형식")
    class ErrorResponseFormat {

        @Test
        @DisplayName("에러 응답에는 timestamp가 포함된다")
        void errorResponse_containsTimestamp() throws Exception {
            // given
            given(brokerService.listBrokers("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("클러스터 에러 응답에는 clusterId가 포함된다")
        void clusterErrorResponse_containsDetails() throws Exception {
            // given
            given(brokerService.listBrokers("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/{clusterId}/brokers", "unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.error.details.clusterId", is("unknown")));
        }
    }
}
