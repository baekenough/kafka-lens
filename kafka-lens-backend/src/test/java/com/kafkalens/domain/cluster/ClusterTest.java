package com.kafkalens.domain.cluster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cluster 엔티티 테스트 클래스.
 */
@DisplayName("Cluster")
class ClusterTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("유효한 값으로 Cluster를 생성할 수 있다")
        void shouldCreateClusterWithValidValues() {
            // when
            Cluster cluster = Cluster.builder()
                    .id("test-cluster")
                    .name("Test Cluster")
                    .description("Test description")
                    .environment("development")
                    .bootstrapServers("localhost:9092")
                    .build();

            // then
            assertEquals("test-cluster", cluster.id());
            assertEquals("Test Cluster", cluster.name());
            assertEquals("Test description", cluster.description());
            assertEquals("development", cluster.environment());
            assertEquals(List.of("localhost:9092"), cluster.bootstrapServers());
            assertNotNull(cluster.security());
            assertTrue(cluster.properties().isEmpty());
        }

        @Test
        @DisplayName("여러 부트스트랩 서버를 설정할 수 있다")
        void shouldCreateClusterWithMultipleBootstrapServers() {
            // when
            Cluster cluster = Cluster.builder()
                    .id("test-cluster")
                    .name("Test Cluster")
                    .bootstrapServers("server1:9092", "server2:9092", "server3:9092")
                    .build();

            // then
            assertEquals(3, cluster.bootstrapServers().size());
            assertEquals("server1:9092,server2:9092,server3:9092", cluster.getBootstrapServersAsString());
        }

        @Test
        @DisplayName("id가 null이면 예외가 발생한다")
        void shouldThrowExceptionWhenIdIsNull() {
            // when & then
            assertThrows(NullPointerException.class, () ->
                    Cluster.builder()
                            .name("Test")
                            .bootstrapServers("localhost:9092")
                            .build()
            );
        }

        @Test
        @DisplayName("id가 빈 문자열이면 예외가 발생한다")
        void shouldThrowExceptionWhenIdIsBlank() {
            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    Cluster.builder()
                            .id("  ")
                            .name("Test")
                            .bootstrapServers("localhost:9092")
                            .build()
            );
            assertEquals("Cluster id must not be blank", ex.getMessage());
        }

        @Test
        @DisplayName("bootstrapServers가 비어있으면 예외가 발생한다")
        void shouldThrowExceptionWhenBootstrapServersIsEmpty() {
            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    Cluster.builder()
                            .id("test")
                            .name("Test")
                            .bootstrapServers(Collections.emptyList())
                            .build()
            );
            assertEquals("Bootstrap servers must not be empty", ex.getMessage());
        }

        @Test
        @DisplayName("security가 null이면 PLAINTEXT로 기본 설정된다")
        void shouldDefaultToPlaintextWhenSecurityIsNull() {
            // when
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .build();

            // then
            assertNotNull(cluster.security());
            assertEquals("PLAINTEXT", cluster.security().protocol());
            assertFalse(cluster.isSecure());
        }

        @Test
        @DisplayName("properties가 null이면 빈 맵으로 설정된다")
        void shouldDefaultToEmptyMapWhenPropertiesIsNull() {
            // when
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .properties(null)
                    .build();

            // then
            assertNotNull(cluster.properties());
            assertTrue(cluster.properties().isEmpty());
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("bootstrapServers 리스트는 수정할 수 없다")
        void bootstrapServersShouldBeImmutable() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .build();

            // when & then
            assertThrows(UnsupportedOperationException.class, () ->
                    cluster.bootstrapServers().add("new-server:9092")
            );
        }

        @Test
        @DisplayName("properties 맵은 수정할 수 없다")
        void propertiesShouldBeImmutable() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .properties(Map.of("key", "value"))
                    .build();

            // when & then
            assertThrows(UnsupportedOperationException.class, () ->
                    cluster.properties().put("newKey", "newValue")
            );
        }

        @Test
        @DisplayName("원본 리스트를 수정해도 Cluster에 영향을 주지 않는다")
        void originalListModificationShouldNotAffectCluster() {
            // given
            List<String> mutableServers = new java.util.ArrayList<>();
            mutableServers.add("localhost:9092");

            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers(mutableServers)
                    .build();

            // when
            mutableServers.add("new-server:9092");

            // then
            assertEquals(1, cluster.bootstrapServers().size());
        }

        @Test
        @DisplayName("원본 맵을 수정해도 Cluster에 영향을 주지 않는다")
        void originalMapModificationShouldNotAffectCluster() {
            // given
            Map<String, String> mutableProps = new HashMap<>();
            mutableProps.put("key", "value");

            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .properties(mutableProps)
                    .build();

            // when
            mutableProps.put("newKey", "newValue");

            // then
            assertEquals(1, cluster.properties().size());
            assertFalse(cluster.properties().containsKey("newKey"));
        }
    }

    @Nested
    @DisplayName("환경 및 보안 테스트")
    class EnvironmentAndSecurityTest {

        @Test
        @DisplayName("production 환경을 식별할 수 있다")
        void shouldIdentifyProductionEnvironment() {
            // given
            Cluster prodCluster = Cluster.builder()
                    .id("prod")
                    .name("Production")
                    .environment("production")
                    .bootstrapServers("localhost:9092")
                    .build();

            Cluster devCluster = Cluster.builder()
                    .id("dev")
                    .name("Development")
                    .environment("development")
                    .bootstrapServers("localhost:9092")
                    .build();

            // then
            assertTrue(prodCluster.isProduction());
            assertFalse(devCluster.isProduction());
        }

        @Test
        @DisplayName("PLAINTEXT는 보안 연결이 아니다")
        void plaintextShouldNotBeSecure() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .security(Cluster.SecurityConfig.plaintext())
                    .build();

            // then
            assertFalse(cluster.isSecure());
        }

        @Test
        @DisplayName("SSL은 보안 연결이다")
        void sslShouldBeSecure() {
            // given
            Cluster.SecurityConfig sslConfig = new Cluster.SecurityConfig("SSL", null, null);
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .security(sslConfig)
                    .build();

            // then
            assertTrue(cluster.isSecure());
            assertTrue(cluster.security().usesSsl());
            assertFalse(cluster.security().usesSasl());
        }

        @Test
        @DisplayName("SASL_SSL은 SASL과 SSL을 모두 사용한다")
        void saslSslShouldUseBoth() {
            // given
            Cluster.SaslConfig saslConfig = new Cluster.SaslConfig("SCRAM-SHA-512", "user", "pass");
            Cluster.SecurityConfig securityConfig = new Cluster.SecurityConfig("SASL_SSL", saslConfig, null);

            // then
            assertTrue(securityConfig.isSecure());
            assertTrue(securityConfig.usesSasl());
            assertTrue(securityConfig.usesSsl());
        }
    }

    @Nested
    @DisplayName("유틸리티 메서드 테스트")
    class UtilityMethodTest {

        @Test
        @DisplayName("단일 서버를 문자열로 변환할 수 있다")
        void shouldConvertSingleServerToString() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("localhost:9092")
                    .build();

            // when
            String result = cluster.getBootstrapServersAsString();

            // then
            assertEquals("localhost:9092", result);
        }

        @Test
        @DisplayName("여러 서버를 쉼표로 구분된 문자열로 변환할 수 있다")
        void shouldConvertMultipleServersToString() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("test")
                    .name("Test")
                    .bootstrapServers("server1:9092", "server2:9093", "server3:9094")
                    .build();

            // when
            String result = cluster.getBootstrapServersAsString();

            // then
            assertEquals("server1:9092,server2:9093,server3:9094", result);
        }
    }
}
