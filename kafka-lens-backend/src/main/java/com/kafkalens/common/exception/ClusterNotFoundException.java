package com.kafkalens.common.exception;

import com.kafkalens.common.ErrorCode;

import java.util.Map;

/**
 * 클러스터를 찾을 수 없을 때 발생하는 예외.
 */
public class ClusterNotFoundException extends KafkaLensException {

    /**
     * 클러스터 ID로 예외를 생성합니다.
     *
     * @param clusterId 찾을 수 없는 클러스터 ID
     */
    public ClusterNotFoundException(String clusterId) {
        super(
                ErrorCode.CLUSTER_NOT_FOUND,
                String.format("Cluster not found: %s", clusterId),
                Map.of("clusterId", clusterId)
        );
    }
}
