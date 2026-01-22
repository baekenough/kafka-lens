package com.kafkalens.api.v1.dto;

import com.kafkalens.domain.cluster.Cluster;

import java.util.List;

/**
 * 클러스터 응답 DTO.
 *
 * <p>API 응답에서 클러스터 정보를 전달하는데 사용됩니다.
 * 민감한 정보(비밀번호 등)를 제외하고 필요한 정보만 포함합니다.</p>
 *
 * @param id               클러스터 고유 식별자
 * @param name             클러스터 표시 이름
 * @param description      클러스터 설명
 * @param environment      환경 (development, staging, production)
 * @param bootstrapServers 부트스트랩 서버 목록
 * @param securityProtocol 보안 프로토콜 (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
 * @param isProduction     프로덕션 환경 여부
 */
public record ClusterResponse(
        String id,
        String name,
        String description,
        String environment,
        List<String> bootstrapServers,
        String securityProtocol,
        boolean isProduction
) {
    /**
     * Cluster 엔티티에서 ClusterResponse를 생성합니다.
     *
     * @param cluster Cluster 엔티티
     * @return ClusterResponse 인스턴스
     */
    public static ClusterResponse from(Cluster cluster) {
        String protocol = cluster.security() != null
                ? cluster.security().protocol()
                : "PLAINTEXT";

        return new ClusterResponse(
                cluster.id(),
                cluster.name(),
                cluster.description(),
                cluster.environment(),
                cluster.bootstrapServers(),
                protocol,
                cluster.isProduction()
        );
    }

    /**
     * Cluster 엔티티 목록에서 ClusterResponse 목록을 생성합니다.
     *
     * @param clusters Cluster 엔티티 목록
     * @return ClusterResponse 목록
     */
    public static List<ClusterResponse> fromList(List<Cluster> clusters) {
        return clusters.stream()
                .map(ClusterResponse::from)
                .toList();
    }
}
