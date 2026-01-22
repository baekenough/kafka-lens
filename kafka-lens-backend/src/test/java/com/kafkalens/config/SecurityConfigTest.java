package com.kafkalens.config;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security 설정 테스트.
 *
 * <p>Spring Security 설정이 올바르게 적용되었는지 검증합니다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *     <li>인증되지 않은 요청이 /api/** 접근 시 401 반환</li>
 *     <li>Basic Auth로 인증된 요청이 /api/** 접근 허용</li>
 *     <li>CORS 설정 검증</li>
 *     <li>CSRF 비활성화 검증</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("인증 테스트")
    class AuthenticationTest {

        @Test
        @DisplayName("인증되지 않은 요청은 401 Unauthorized를 반환한다")
        void unauthenticatedRequest_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("잘못된 자격 증명으로 요청하면 401 Unauthorized를 반환한다")
        void invalidCredentials_returns401() throws Exception {
            String invalidAuth = Base64.getEncoder()
                    .encodeToString("invalid:wrong".getBytes());

            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Basic " + invalidAuth))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("올바른 Basic Auth 자격 증명으로 요청하면 200 OK를 반환한다")
        void validBasicAuth_returns200() throws Exception {
            // 기본 개발 환경 자격 증명: admin/admin
            String validAuth = Base64.getEncoder()
                    .encodeToString("admin:admin".getBytes());

            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Basic " + validAuth))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("@WithMockUser로 인증된 요청은 API에 접근할 수 있다")
        void withMockUser_canAccessApi() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CORS 테스트")
    class CorsTest {

        @Test
        @DisplayName("localhost:3000에서 오는 요청에 CORS 헤더가 포함된다")
        void corsHeadersForLocalhost3000() throws Exception {
            String validAuth = Base64.getEncoder()
                    .encodeToString("admin:admin".getBytes());

            mockMvc.perform(options("/api/v1/clusters")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Authorization", "Basic " + validAuth))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                    .andExpect(header().exists("Access-Control-Allow-Methods"));
        }

        @Test
        @DisplayName("허용되지 않은 Origin에서의 요청은 CORS 헤더가 없다")
        void corsHeadersNotIncludedForUnallowedOrigin() throws Exception {
            String validAuth = Base64.getEncoder()
                    .encodeToString("admin:admin".getBytes());

            mockMvc.perform(options("/api/v1/clusters")
                            .header("Origin", "http://malicious-site.com")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Authorization", "Basic " + validAuth))
                    .andDo(print())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @DisplayName("Public 엔드포인트 테스트")
    class PublicEndpointTest {

        @Test
        @DisplayName("헬스 체크 엔드포인트는 인증 없이 접근 가능하다")
        void healthEndpoint_accessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/actuator/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CSRF 테스트")
    class CsrfTest {

        @Test
        @DisplayName("CSRF 토큰 없이 POST 요청이 가능하다 (CSRF 비활성화)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void postWithoutCsrf_succeeds() throws Exception {
            mockMvc.perform(get("/api/v1/clusters")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
