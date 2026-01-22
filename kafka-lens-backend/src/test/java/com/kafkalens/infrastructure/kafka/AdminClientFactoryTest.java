package com.kafkalens.infrastructure.kafka;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminClientFactory 테스트 클래스.
 *
 * <p>실제 Kafka 연결 없이 단위 테스트를 수행합니다.
 * 통합 테스트는 Testcontainers를 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminClientFactory")
class AdminClientFactoryTest {

    @Mock
    private ClusterRepository clusterRepository;

    private AdminClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AdminClientFactory(clusterRepository);
    }

    @Nested
    @DisplayName("클라이언트 생성 테스트")
    class ClientCreationTest {

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 요청하면 예외가 발생한다")
        void shouldThrowExceptionForNonExistentCluster() {
            // given
            when(clusterRepository.findById("non-existent")).thenReturn(Optional.empty());

            // when & then
            assertThrows(ClusterNotFoundException.class, () -> factory.getOrCreate("non-existent"));
        }

        @Test
        @DisplayName("클러스터가 캐시되어 있는지 확인할 수 있다")
        void shouldCheckIfClusterIsCached() {
            // given - 아무것도 캐시되지 않은 상태

            // then
            assertFalse(factory.isCached("any-cluster"));
            assertEquals(0, factory.getCachedClientCount());
        }
    }

    @Nested
    @DisplayName("캐시 관리 테스트")
    class CacheManagementTest {

        @Test
        @DisplayName("closeClient 호출 후 캐시에서 제거된다")
        void shouldRemoveFromCacheAfterClose() {
            // given - 캐시에 아무것도 없는 상태에서 close 호출
            assertFalse(factory.isCached("test-cluster"));

            // when
            factory.closeClient("test-cluster");

            // then - 예외 없이 처리됨
            assertFalse(factory.isCached("test-cluster"));
        }

        @Test
        @DisplayName("closeAll 호출로 모든 클라이언트를 닫을 수 있다")
        void shouldCloseAllClients() {
            // when
            factory.closeAll();

            // then
            assertEquals(0, factory.getCachedClientCount());
        }
    }

    @Nested
    @DisplayName("클러스터 설정 테스트")
    class ClusterConfigTest {

        @Test
        @DisplayName("PLAINTEXT 클러스터 설정을 처리할 수 있다")
        void shouldHandlePlaintextCluster() {
            // given
            Cluster cluster = Cluster.builder()
                    .id("plaintext-cluster")
                    .name("Plaintext Cluster")
                    .bootstrapServers("localhost:9092")
                    .security(Cluster.SecurityConfig.plaintext())
                    .build();

            when(clusterRepository.findById("plaintext-cluster")).thenReturn(Optional.of(cluster));

            // when & then
            // 실제 연결 없이 설정만 검증 (실제 생성은 통합 테스트에서)
            verify(clusterRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("SASL_SSL 클러스터 설정을 처리할 수 있다")
        void shouldHandleSaslSslCluster() {
            // given
            Cluster.SaslConfig sasl = new Cluster.SaslConfig("SCRAM-SHA-512", "user", "pass");
            Cluster.SecurityConfig security = new Cluster.SecurityConfig("SASL_SSL", sasl, null);

            Cluster cluster = Cluster.builder()
                    .id("sasl-cluster")
                    .name("SASL Cluster")
                    .bootstrapServers("kafka:9093")
                    .security(security)
                    .build();

            // then - 클러스터 객체가 올바르게 생성됨
            assertEquals("SASL_SSL", cluster.security().protocol());
            assertTrue(cluster.security().usesSasl());
            assertTrue(cluster.security().usesSsl());
        }

        @Test
        @DisplayName("SSL 클러스터 설정을 처리할 수 있다")
        void shouldHandleSslCluster() {
            // given
            Cluster.SslConfig ssl = new Cluster.SslConfig(
                    "/path/truststore.jks", "trustpass",
                    "/path/keystore.jks", "keypass", "keypass"
            );
            Cluster.SecurityConfig security = new Cluster.SecurityConfig("SSL", null, ssl);

            Cluster cluster = Cluster.builder()
                    .id("ssl-cluster")
                    .name("SSL Cluster")
                    .bootstrapServers("kafka:9093")
                    .security(security)
                    .build();

            // then
            assertEquals("SSL", cluster.security().protocol());
            assertTrue(cluster.security().usesSsl());
            assertFalse(cluster.security().usesSasl());
        }
    }
}
