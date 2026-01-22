package com.kafkalens.api.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API 인증 통합 테스트.
 *
 * <p>실제 API 엔드포인트에 대한 인증 동작을 검증합니다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *     <li>클러스터 API 인증</li>
 *     <li>토픽 API 인증</li>
 *     <li>컨슈머 API 인증</li>
 *     <li>인증 실패 응답 형식</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String getBasicAuthHeader(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    @Nested
    @DisplayName("Cluster API 인증 테스트")
    class ClusterApiAuthTest {

        @Test
        @DisplayName("인증 없이 GET /api/v1/clusters 접근 시 401 반환")
        void getClusters_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Basic Auth로 GET /api/v1/clusters 접근 시 200 반환")
        void getClusters_withBasicAuth_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", getBasicAuthHeader("admin", "admin")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        @DisplayName("인증 없이 GET /api/v1/clusters/{id} 접근 시 401 반환")
        void getClusterById_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters/local")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Mock 인증으로 GET /api/v1/clusters/{id} 접근 가능")
        void getClusterById_withMockUser_accessAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/clusters/local")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("인증 없이 POST /api/v1/clusters/{id}/test 접근 시 401 반환")
        void testConnection_withoutAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/clusters/local/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Topic API 인증 테스트")
    class TopicApiAuthTest {

        @Test
        @DisplayName("인증 없이 GET /api/v1/clusters/{id}/topics 접근 시 401 반환")
        void getTopics_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters/local/topics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Mock 인증으로 Topic API 접근 가능")
        void getTopics_withMockUser_accessAllowed() throws Exception {
            int status = mockMvc.perform(get("/api/v1/clusters/local/topics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andReturn()
                    .getResponse()
                    .getStatus();
            // 인증이 통과되었으므로 401이 아닌 응답을 받음
            // (Kafka 연결 오류로 503/504가 올 수 있음)
            org.junit.jupiter.api.Assertions.assertNotEquals(401, status,
                    "인증이 실패하면 안됩니다 (401 Unauthorized)");
        }
    }

    @Nested
    @DisplayName("Consumer API 인증 테스트")
    class ConsumerApiAuthTest {

        @Test
        @DisplayName("인증 없이 GET /api/v1/clusters/{id}/consumer-groups 접근 시 401 반환")
        void getConsumerGroups_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters/local/consumer-groups")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Mock 인증으로 Consumer API 접근 가능")
        void getConsumerGroups_withMockUser_accessAllowed() throws Exception {
            int status = mockMvc.perform(get("/api/v1/clusters/local/consumer-groups")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andReturn()
                    .getResponse()
                    .getStatus();
            // 인증이 통과되었으므로 401이 아닌 응답을 받음
            // (Kafka 연결 오류로 503/504가 올 수 있음)
            org.junit.jupiter.api.Assertions.assertNotEquals(401, status,
                    "인증이 실패하면 안됩니다 (401 Unauthorized)");
        }
    }

    @Nested
    @DisplayName("인증 실패 응답 형식 테스트")
    class AuthFailureResponseTest {

        @Test
        @DisplayName("인증 실패 시 WWW-Authenticate 헤더가 포함된다")
        void authFailure_includesWwwAuthenticateHeader() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 인증 실패 시 401 반환")
        void wrongPassword_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", getBasicAuthHeader("admin", "wrongpassword")))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 인증 실패 시 401 반환")
        void unknownUser_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", getBasicAuthHeader("unknown", "password")))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("다중 사용자 테스트")
    class MultipleUsersTest {

        @Test
        @DisplayName("admin 사용자로 API 접근 가능")
        void adminUser_canAccessApi() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", getBasicAuthHeader("admin", "admin")))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
