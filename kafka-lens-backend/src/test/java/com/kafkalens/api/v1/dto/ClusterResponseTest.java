package com.kafkalens.api.v1.dto;

import com.kafkalens.domain.cluster.Cluster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClusterResponse DTO 테스트.
 */
class ClusterResponseTest {

    @Test
    @DisplayName("Cluster 엔티티에서 ClusterResponse를 생성한다")
    void from_createsClusterResponse() {
        // given
        Cluster cluster = Cluster.builder()
                .id("local")
                .name("Local Development")
                .description("Local cluster")
                .environment("development")
                .bootstrapServers("localhost:9092")
                .build();

        // when
        ClusterResponse response = ClusterResponse.from(cluster);

        // then
        assertThat(response.id()).isEqualTo("local");
        assertThat(response.name()).isEqualTo("Local Development");
        assertThat(response.description()).isEqualTo("Local cluster");
        assertThat(response.environment()).isEqualTo("development");
        assertThat(response.bootstrapServers()).containsExactly("localhost:9092");
        assertThat(response.securityProtocol()).isEqualTo("PLAINTEXT");
        assertThat(response.isProduction()).isFalse();
    }

    @Test
    @DisplayName("Production 클러스터는 isProduction이 true이다")
    void from_productionCluster_isProductionTrue() {
        // given
        Cluster cluster = Cluster.builder()
                .id("prod")
                .name("Production")
                .environment("production")
                .bootstrapServers("kafka-prod:9092")
                .build();

        // when
        ClusterResponse response = ClusterResponse.from(cluster);

        // then
        assertThat(response.isProduction()).isTrue();
    }

    @Test
    @DisplayName("Cluster 목록에서 ClusterResponse 목록을 생성한다")
    void fromList_createsClusterResponseList() {
        // given
        List<Cluster> clusters = List.of(
                Cluster.builder()
                        .id("local")
                        .name("Local")
                        .environment("development")
                        .bootstrapServers("localhost:9092")
                        .build(),
                Cluster.builder()
                        .id("prod")
                        .name("Production")
                        .environment("production")
                        .bootstrapServers("kafka-prod:9092")
                        .build()
        );

        // when
        List<ClusterResponse> responses = ClusterResponse.fromList(clusters);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo("local");
        assertThat(responses.get(1).id()).isEqualTo("prod");
    }

    @Test
    @DisplayName("빈 클러스터 목록은 빈 응답 목록을 반환한다")
    void fromList_emptyClusters_returnsEmptyList() {
        // when
        List<ClusterResponse> responses = ClusterResponse.fromList(List.of());

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("보안 프로토콜이 포함된 클러스터")
    void from_clusterWithSecurity_includesSecurityProtocol() {
        // given
        Cluster cluster = Cluster.builder()
                .id("secure")
                .name("Secure Cluster")
                .environment("production")
                .bootstrapServers("kafka:9092")
                .security(new Cluster.SecurityConfig("SASL_SSL", null, null))
                .build();

        // when
        ClusterResponse response = ClusterResponse.from(cluster);

        // then
        assertThat(response.securityProtocol()).isEqualTo("SASL_SSL");
    }
}
