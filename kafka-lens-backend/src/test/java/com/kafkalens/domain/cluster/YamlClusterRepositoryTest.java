package com.kafkalens.domain.cluster;

import com.kafkalens.common.exception.KafkaLensException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * YamlClusterRepository 테스트 클래스.
 */
@DisplayName("YamlClusterRepository")
class YamlClusterRepositoryTest {

    private ResourceLoader resourceLoader;
    private YamlClusterRepository repository;

    @BeforeEach
    void setUp() {
        resourceLoader = mock(ResourceLoader.class);
    }

    @Nested
    @DisplayName("YAML 파싱 테스트")
    class YamlParsingTest {

        @Test
        @DisplayName("유효한 YAML에서 클러스터를 로드할 수 있다")
        void shouldLoadClustersFromValidYaml() {
            // given
            String yaml = """
                    clusters:
                      - id: test-cluster
                        name: "Test Cluster"
                        description: "Test description"
                        environment: development
                        bootstrap-servers:
                          - localhost:9092
                        security:
                          protocol: PLAINTEXT
                        properties:
                          request.timeout.ms: 30000
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            List<Cluster> clusters = repository.findAll();

            // then
            assertEquals(1, clusters.size());

            Cluster cluster = clusters.get(0);
            assertEquals("test-cluster", cluster.id());
            assertEquals("Test Cluster", cluster.name());
            assertEquals("Test description", cluster.description());
            assertEquals("development", cluster.environment());
            assertEquals(List.of("localhost:9092"), cluster.bootstrapServers());
            assertEquals("PLAINTEXT", cluster.security().protocol());
            assertEquals("30000", cluster.properties().get("request.timeout.ms"));
        }

        @Test
        @DisplayName("여러 클러스터를 로드할 수 있다")
        void shouldLoadMultipleClusters() {
            // given
            String yaml = """
                    clusters:
                      - id: dev-cluster
                        name: "Development"
                        bootstrap-servers:
                          - localhost:9092
                      - id: staging-cluster
                        name: "Staging"
                        environment: staging
                        bootstrap-servers:
                          - staging:9092
                      - id: prod-cluster
                        name: "Production"
                        environment: production
                        bootstrap-servers:
                          - prod1:9092
                          - prod2:9092
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            List<Cluster> clusters = repository.findAll();

            // then
            assertEquals(3, clusters.size());
        }

        @Test
        @DisplayName("기본값이 클러스터 설정에 적용된다")
        void shouldApplyDefaultsToCluster() {
            // given
            String yaml = """
                    defaults:
                      security:
                        protocol: SSL
                      properties:
                        request.timeout.ms: 60000
                        reconnect.backoff.ms: 1000
                    clusters:
                      - id: test-cluster
                        name: "Test"
                        bootstrap-servers:
                          - localhost:9092
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("test-cluster");

            // then
            assertTrue(cluster.isPresent());
            assertEquals("SSL", cluster.get().security().protocol());
            assertEquals("60000", cluster.get().properties().get("request.timeout.ms"));
            assertEquals("1000", cluster.get().properties().get("reconnect.backoff.ms"));
        }

        @Test
        @DisplayName("클러스터 설정이 기본값을 덮어쓴다")
        void shouldOverrideDefaultsWithClusterConfig() {
            // given
            String yaml = """
                    defaults:
                      properties:
                        request.timeout.ms: 30000
                    clusters:
                      - id: test-cluster
                        name: "Test"
                        bootstrap-servers:
                          - localhost:9092
                        properties:
                          request.timeout.ms: 60000
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("test-cluster");

            // then
            assertTrue(cluster.isPresent());
            assertEquals("60000", cluster.get().properties().get("request.timeout.ms"));
        }

        @Test
        @DisplayName("SASL 설정을 파싱할 수 있다")
        void shouldParseSaslConfig() {
            // given
            String yaml = """
                    clusters:
                      - id: sasl-cluster
                        name: "SASL Cluster"
                        bootstrap-servers:
                          - kafka:9093
                        security:
                          protocol: SASL_SSL
                          sasl:
                            mechanism: SCRAM-SHA-512
                            username: testuser
                            password: testpass
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("sasl-cluster");

            // then
            assertTrue(cluster.isPresent());
            assertEquals("SASL_SSL", cluster.get().security().protocol());
            assertTrue(cluster.get().security().usesSasl());
            assertEquals("SCRAM-SHA-512", cluster.get().security().sasl().mechanism());
            assertEquals("testuser", cluster.get().security().sasl().username());
            assertEquals("testpass", cluster.get().security().sasl().password());
        }

        @Test
        @DisplayName("SSL 설정을 파싱할 수 있다")
        void shouldParseSslConfig() {
            // given
            String yaml = """
                    clusters:
                      - id: ssl-cluster
                        name: "SSL Cluster"
                        bootstrap-servers:
                          - kafka:9093
                        security:
                          protocol: SSL
                          ssl:
                            truststore-location: /path/to/truststore.jks
                            truststore-password: trustpass
                            keystore-location: /path/to/keystore.jks
                            keystore-password: keypass
                            key-password: keypass
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("ssl-cluster");

            // then
            assertTrue(cluster.isPresent());
            assertEquals("SSL", cluster.get().security().protocol());
            assertTrue(cluster.get().security().usesSsl());
            assertEquals("/path/to/truststore.jks", cluster.get().security().ssl().truststoreLocation());
        }

        @Test
        @DisplayName("클러스터가 없는 YAML을 처리할 수 있다")
        void shouldHandleEmptyClusters() {
            // given
            String yaml = """
                    defaults:
                      security:
                        protocol: PLAINTEXT
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            List<Cluster> clusters = repository.findAll();

            // then
            assertTrue(clusters.isEmpty());
        }
    }

    @Nested
    @DisplayName("조회 테스트")
    class QueryTest {

        @Test
        @DisplayName("ID로 클러스터를 찾을 수 있다")
        void shouldFindClusterById() {
            // given
            String yaml = """
                    clusters:
                      - id: cluster-1
                        name: "Cluster 1"
                        bootstrap-servers:
                          - localhost:9092
                      - id: cluster-2
                        name: "Cluster 2"
                        bootstrap-servers:
                          - localhost:9093
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("cluster-2");

            // then
            assertTrue(cluster.isPresent());
            assertEquals("cluster-2", cluster.get().id());
            assertEquals("Cluster 2", cluster.get().name());
        }

        @Test
        @DisplayName("존재하지 않는 ID는 빈 Optional을 반환한다")
        void shouldReturnEmptyForNonExistentId() {
            // given
            String yaml = """
                    clusters:
                      - id: cluster-1
                        name: "Cluster 1"
                        bootstrap-servers:
                          - localhost:9092
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            Optional<Cluster> cluster = repository.findById("non-existent");

            // then
            assertTrue(cluster.isEmpty());
        }

        @Test
        @DisplayName("클러스터 존재 여부를 확인할 수 있다")
        void shouldCheckClusterExists() {
            // given
            String yaml = """
                    clusters:
                      - id: existing-cluster
                        name: "Existing"
                        bootstrap-servers:
                          - localhost:9092
                    """;

            repository = createRepositoryWithYaml(yaml);

            // then
            assertTrue(repository.existsById("existing-cluster"));
            assertFalse(repository.existsById("non-existent"));
        }

        @Test
        @DisplayName("환경별로 클러스터를 조회할 수 있다")
        void shouldFindClustersByEnvironment() {
            // given
            String yaml = """
                    clusters:
                      - id: dev-1
                        name: "Dev 1"
                        environment: development
                        bootstrap-servers:
                          - localhost:9092
                      - id: dev-2
                        name: "Dev 2"
                        environment: development
                        bootstrap-servers:
                          - localhost:9093
                      - id: prod-1
                        name: "Prod 1"
                        environment: production
                        bootstrap-servers:
                          - prod:9092
                    """;

            repository = createRepositoryWithYaml(yaml);

            // when
            List<Cluster> devClusters = repository.findByEnvironment("development");
            List<Cluster> prodClusters = repository.findByEnvironment("production");
            List<Cluster> stagingClusters = repository.findByEnvironment("staging");

            // then
            assertEquals(2, devClusters.size());
            assertEquals(1, prodClusters.size());
            assertEquals(0, stagingClusters.size());
        }
    }

    @Nested
    @DisplayName("리로드 테스트")
    class ReloadTest {

        @Test
        @DisplayName("설정을 다시 로드할 수 있다")
        void shouldReloadConfiguration() {
            // given
            String initialYaml = """
                    clusters:
                      - id: cluster-1
                        name: "Cluster 1"
                        bootstrap-servers:
                          - localhost:9092
                    """;

            String updatedYaml = """
                    clusters:
                      - id: cluster-1
                        name: "Updated Cluster 1"
                        bootstrap-servers:
                          - localhost:9092
                      - id: cluster-2
                        name: "Cluster 2"
                        bootstrap-servers:
                          - localhost:9093
                    """;

            // Initial load
            repository = createRepositoryWithYaml(initialYaml);
            assertEquals(1, repository.findAll().size());

            // Reload with updated config
            repository.loadFromInputStream(createInputStream(updatedYaml));

            // then
            assertEquals(2, repository.findAll().size());
            assertEquals("Updated Cluster 1", repository.findById("cluster-1").get().name());
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("파일이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenFileNotFound() throws IOException {
            // given
            Resource resource = mock(Resource.class);
            when(resource.exists()).thenReturn(false);
            when(resourceLoader.getResource(anyString())).thenReturn(resource);

            repository = new YamlClusterRepository(resourceLoader, "classpath:non-existent.yml");

            // when
            repository.reload();

            // then
            assertTrue(repository.findAll().isEmpty());
        }

        @Test
        @DisplayName("IO 에러 발생 시 예외를 던진다")
        void shouldThrowExceptionOnIOError() throws IOException {
            // given
            Resource resource = mock(Resource.class);
            when(resource.exists()).thenReturn(true);
            when(resource.getInputStream()).thenThrow(new IOException("Read error"));
            when(resourceLoader.getResource(anyString())).thenReturn(resource);

            repository = new YamlClusterRepository(resourceLoader, "classpath:error.yml");

            // when & then
            assertThrows(KafkaLensException.class, () -> repository.reload());
        }
    }

    // === Helper Methods ===

    private YamlClusterRepository createRepositoryWithYaml(String yaml) {
        YamlClusterRepository repo = new YamlClusterRepository(resourceLoader, "classpath:test.yml");
        repo.loadFromInputStream(createInputStream(yaml));
        return repo;
    }

    private InputStream createInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
