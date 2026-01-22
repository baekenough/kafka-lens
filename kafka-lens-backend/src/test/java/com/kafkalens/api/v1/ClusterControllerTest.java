package com.kafkalens.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkalens.api.v1.dto.ConnectionTestResult;
import com.kafkalens.common.GlobalExceptionHandler;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ClusterController 통합 테스트.
 *
 * <p>Spring MVC 테스트 프레임워크를 사용하여 컨트롤러 레이어를 테스트합니다.
 * 서비스 레이어는 Mock으로 대체합니다.</p>
 */
@WebMvcTest(ClusterController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ClusterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClusterService clusterService;

    private Cluster localCluster;
    private Cluster prodCluster;

    @BeforeEach
    void setUp() {
        localCluster = Cluster.builder()
                .id("local")
                .name("Local Development")
                .description("Local Kafka cluster for development")
                .environment("development")
                .bootstrapServers("localhost:9092")
                .build();

        prodCluster = Cluster.builder()
                .id("production")
                .name("Production Cluster")
                .description("Production Kafka cluster")
                .environment("production")
                .bootstrapServers("kafka-prod-1:9092", "kafka-prod-2:9092")
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/clusters")
    class GetClusters {

        @Test
        @DisplayName("모든 클러스터 목록을 반환한다")
        void getAllClusters_returnsClusterList() throws Exception {
            // given
            List<Cluster> clusters = List.of(localCluster, prodCluster);
            given(clusterService.findAll()).willReturn(clusters);

            // when & then
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].id", is("local")))
                    .andExpect(jsonPath("$.data[0].name", is("Local Development")))
                    .andExpect(jsonPath("$.data[1].id", is("production")))
                    .andExpect(jsonPath("$.data[1].name", is("Production Cluster")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(clusterService).findAll();
        }

        @Test
        @DisplayName("클러스터가 없으면 빈 목록을 반환한다")
        void getAllClusters_noClusters_returnsEmptyList() throws Exception {
            // given
            given(clusterService.findAll()).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(clusterService).findAll();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clusters/{id}")
    class GetClusterById {

        @Test
        @DisplayName("존재하는 클러스터 ID로 조회하면 클러스터 정보를 반환한다")
        void getClusterById_existingCluster_returnsCluster() throws Exception {
            // given
            given(clusterService.findById("local")).willReturn(localCluster);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/local")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is("local")))
                    .andExpect(jsonPath("$.data.name", is("Local Development")))
                    .andExpect(jsonPath("$.data.description", is("Local Kafka cluster for development")))
                    .andExpect(jsonPath("$.data.environment", is("development")))
                    .andExpect(jsonPath("$.data.bootstrapServers", hasSize(1)))
                    .andExpect(jsonPath("$.data.bootstrapServers[0]", is("localhost:9092")));

            verify(clusterService).findById("local");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 404 에러를 반환한다")
        void getClusterById_nonExistingCluster_returns404() throws Exception {
            // given
            given(clusterService.findById("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("unknown")));

            verify(clusterService).findById("unknown");
        }

        @Test
        @DisplayName("Production 클러스터 정보에는 다중 부트스트랩 서버가 포함된다")
        void getClusterById_productionCluster_returnsMultipleBootstrapServers() throws Exception {
            // given
            given(clusterService.findById("production")).willReturn(prodCluster);

            // when & then
            mockMvc.perform(get("/api/v1/clusters/production")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bootstrapServers", hasSize(2)))
                    .andExpect(jsonPath("$.data.bootstrapServers[0]", is("kafka-prod-1:9092")))
                    .andExpect(jsonPath("$.data.bootstrapServers[1]", is("kafka-prod-2:9092")));

            verify(clusterService).findById("production");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/clusters/{id}/test")
    class TestClusterConnection {

        @Test
        @DisplayName("연결 테스트 성공 시 성공 결과를 반환한다")
        void testConnection_success_returnsSuccessResult() throws Exception {
            // given
            ConnectionTestResult testResult = ConnectionTestResult.success(
                    "local", "Local Development", 150L
            );
            given(clusterService.testConnection("local")).willReturn(testResult);

            // when & then
            mockMvc.perform(post("/api/v1/clusters/local/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.success", is(true)))
                    .andExpect(jsonPath("$.data.clusterId", is("local")))
                    .andExpect(jsonPath("$.data.clusterName", is("Local Development")))
                    .andExpect(jsonPath("$.data.responseTimeMs", is(150)))
                    .andExpect(jsonPath("$.data.errorMessage").doesNotExist());

            verify(clusterService).testConnection("local");
        }

        @Test
        @DisplayName("연결 테스트 실패 시 실패 결과를 반환한다")
        void testConnection_failure_returnsFailureResult() throws Exception {
            // given
            ConnectionTestResult testResult = ConnectionTestResult.failure(
                    "local", "Local Development", "Connection refused"
            );
            given(clusterService.testConnection("local")).willReturn(testResult);

            // when & then
            mockMvc.perform(post("/api/v1/clusters/local/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.success", is(false)))
                    .andExpect(jsonPath("$.data.clusterId", is("local")))
                    .andExpect(jsonPath("$.data.errorMessage", is("Connection refused")));

            verify(clusterService).testConnection("local");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터의 연결 테스트 시 404 에러를 반환한다")
        void testConnection_nonExistingCluster_returns404() throws Exception {
            // given
            given(clusterService.testConnection("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(post("/api/v1/clusters/unknown/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CLUSTER_NOT_FOUND")));

            verify(clusterService).testConnection("unknown");
        }
    }

    @Nested
    @DisplayName("에러 응답 형식")
    class ErrorResponseFormat {

        @Test
        @DisplayName("에러 응답에는 timestamp가 포함된다")
        void errorResponse_containsTimestamp() throws Exception {
            // given
            given(clusterService.findById("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("에러 응답에는 details가 포함될 수 있다")
        void errorResponse_mayContainDetails() throws Exception {
            // given
            given(clusterService.findById("unknown"))
                    .willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            mockMvc.perform(get("/api/v1/clusters/unknown")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(jsonPath("$.error.details.clusterId", is("unknown")));
        }
    }
}
