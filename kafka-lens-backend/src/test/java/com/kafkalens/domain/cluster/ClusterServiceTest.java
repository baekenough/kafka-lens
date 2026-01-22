package com.kafkalens.domain.cluster;

import com.kafkalens.api.v1.dto.ConnectionTestResult;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.infrastructure.kafka.AdminClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ClusterService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, ClusterService의 핵심 비즈니스 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private AdminClientFactory adminClientFactory;

    private ClusterService clusterService;

    private Cluster localCluster;
    private Cluster prodCluster;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterService(clusterRepository, adminClientFactory);

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
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("모든 클러스터를 반환한다")
        void testFindAll_returnsAllClusters() {
            // given
            List<Cluster> clusters = List.of(localCluster, prodCluster);
            given(clusterRepository.findAll()).willReturn(clusters);

            // when
            List<Cluster> result = clusterService.findAll();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(localCluster, prodCluster);
            verify(clusterRepository).findAll();
        }

        @Test
        @DisplayName("클러스터가 없으면 빈 목록을 반환한다")
        void testFindAll_noClusters_returnsEmptyList() {
            // given
            given(clusterRepository.findAll()).willReturn(List.of());

            // when
            List<Cluster> result = clusterService.findAll();

            // then
            assertThat(result).isEmpty();
            verify(clusterRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("존재하는 클러스터 ID로 조회하면 클러스터를 반환한다")
        void testFindById_existingCluster_returnsCluster() {
            // given
            given(clusterRepository.findById("local")).willReturn(Optional.of(localCluster));

            // when
            Cluster result = clusterService.findById("local");

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("local");
            assertThat(result.name()).isEqualTo("Local Development");
            verify(clusterRepository).findById("local");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 조회하면 예외를 발생시킨다")
        void testFindById_nonExistingCluster_throwsException() {
            // given
            given(clusterRepository.findById("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clusterService.findById("unknown"))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
            verify(clusterRepository).findById("unknown");
        }
    }

    @Nested
    @DisplayName("testConnection()")
    class TestConnection {

        @Test
        @DisplayName("연결 테스트 성공 시 성공 결과를 반환한다")
        void testTestConnection_success() {
            // given
            given(clusterRepository.findById("local")).willReturn(Optional.of(localCluster));
            given(adminClientFactory.testConnection("local")).willReturn(true);

            // when
            ConnectionTestResult result = clusterService.testConnection("local");

            // then
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.clusterId()).isEqualTo("local");
            assertThat(result.clusterName()).isEqualTo("Local Development");
            assertThat(result.errorMessage()).isNull();
            verify(clusterRepository).findById("local");
            verify(adminClientFactory).testConnection("local");
        }

        @Test
        @DisplayName("연결 테스트 실패 시 실패 결과를 반환한다")
        void testTestConnection_failure() {
            // given
            given(clusterRepository.findById("local")).willReturn(Optional.of(localCluster));
            given(adminClientFactory.testConnection("local")).willReturn(false);

            // when
            ConnectionTestResult result = clusterService.testConnection("local");

            // then
            assertThat(result).isNotNull();
            assertThat(result.success()).isFalse();
            assertThat(result.clusterId()).isEqualTo("local");
            assertThat(result.clusterName()).isEqualTo("Local Development");
            assertThat(result.errorMessage()).isNotNull();
            verify(clusterRepository).findById("local");
            verify(adminClientFactory).testConnection("local");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터의 연결 테스트 시 예외를 발생시킨다")
        void testTestConnection_nonExistingCluster_throwsException() {
            // given
            given(clusterRepository.findById("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clusterService.testConnection("unknown"))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
            verify(clusterRepository).findById("unknown");
        }

        @Test
        @DisplayName("연결 테스트 결과에 응답 시간이 포함된다")
        void testTestConnection_includesResponseTime() {
            // given
            given(clusterRepository.findById("local")).willReturn(Optional.of(localCluster));
            given(adminClientFactory.testConnection("local")).willReturn(true);

            // when
            ConnectionTestResult result = clusterService.testConnection("local");

            // then
            assertThat(result.responseTimeMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsById {

        @Test
        @DisplayName("존재하는 클러스터 ID로 확인하면 true를 반환한다")
        void testExistsById_existingCluster_returnsTrue() {
            // given
            given(clusterRepository.existsById("local")).willReturn(true);

            // when
            boolean result = clusterService.existsById("local");

            // then
            assertThat(result).isTrue();
            verify(clusterRepository).existsById("local");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터 ID로 확인하면 false를 반환한다")
        void testExistsById_nonExistingCluster_returnsFalse() {
            // given
            given(clusterRepository.existsById("unknown")).willReturn(false);

            // when
            boolean result = clusterService.existsById("unknown");

            // then
            assertThat(result).isFalse();
            verify(clusterRepository).existsById("unknown");
        }
    }
}
