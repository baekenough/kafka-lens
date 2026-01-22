package com.kafkalens.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * 클러스터 연결 테스트 결과 DTO.
 *
 * <p>연결 테스트의 성공 여부와 관련 정보를 전달합니다.</p>
 *
 * @param success        연결 테스트 성공 여부
 * @param clusterId      테스트한 클러스터 ID
 * @param clusterName    테스트한 클러스터 이름
 * @param responseTimeMs 응답 시간 (밀리초)
 * @param errorMessage   실패 시 에러 메시지 (성공 시 null)
 * @param testedAt       테스트 시각
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectionTestResult(
        boolean success,
        String clusterId,
        String clusterName,
        Long responseTimeMs,
        String errorMessage,
        Instant testedAt
) {
    /**
     * ConnectionTestResult 생성자.
     * testedAt이 null인 경우 현재 시각으로 설정합니다.
     */
    public ConnectionTestResult {
        testedAt = testedAt != null ? testedAt : Instant.now();
    }

    /**
     * 연결 테스트 성공 결과를 생성합니다.
     *
     * @param clusterId      클러스터 ID
     * @param clusterName    클러스터 이름
     * @param responseTimeMs 응답 시간 (밀리초)
     * @return 성공 ConnectionTestResult
     */
    public static ConnectionTestResult success(String clusterId, String clusterName, long responseTimeMs) {
        return new ConnectionTestResult(
                true,
                clusterId,
                clusterName,
                responseTimeMs,
                null,
                Instant.now()
        );
    }

    /**
     * 연결 테스트 실패 결과를 생성합니다.
     *
     * @param clusterId    클러스터 ID
     * @param clusterName  클러스터 이름
     * @param errorMessage 에러 메시지
     * @return 실패 ConnectionTestResult
     */
    public static ConnectionTestResult failure(String clusterId, String clusterName, String errorMessage) {
        return new ConnectionTestResult(
                false,
                clusterId,
                clusterName,
                null,
                errorMessage,
                Instant.now()
        );
    }

    /**
     * 연결 테스트 실패 결과를 생성합니다 (응답 시간 포함).
     *
     * @param clusterId      클러스터 ID
     * @param clusterName    클러스터 이름
     * @param responseTimeMs 응답 시간 (밀리초)
     * @param errorMessage   에러 메시지
     * @return 실패 ConnectionTestResult
     */
    public static ConnectionTestResult failure(
            String clusterId,
            String clusterName,
            long responseTimeMs,
            String errorMessage
    ) {
        return new ConnectionTestResult(
                false,
                clusterId,
                clusterName,
                responseTimeMs,
                errorMessage,
                Instant.now()
        );
    }
}
