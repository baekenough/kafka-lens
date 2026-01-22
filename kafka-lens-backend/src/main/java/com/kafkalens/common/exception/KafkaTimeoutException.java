package com.kafkalens.common.exception;

import com.kafkalens.common.ErrorCode;

import java.util.Map;

/**
 * Kafka 작업 타임아웃 시 발생하는 예외.
 */
public class KafkaTimeoutException extends KafkaLensException {

    /**
     * 클러스터 ID와 작업명으로 예외를 생성합니다.
     *
     * @param clusterId 클러스터 ID
     * @param operation 타임아웃된 작업명
     * @param cause     원인 예외
     */
    public KafkaTimeoutException(String clusterId, String operation, Throwable cause) {
        super(
                ErrorCode.KAFKA_TIMEOUT,
                String.format("Kafka operation timed out: %s on cluster %s", operation, clusterId),
                Map.of("clusterId", clusterId, "operation", operation),
                cause
        );
    }

    /**
     * 작업명만으로 예외를 생성합니다.
     *
     * @param operation 타임아웃된 작업명
     */
    public KafkaTimeoutException(String operation) {
        super(
                ErrorCode.KAFKA_TIMEOUT,
                String.format("Kafka operation timed out: %s", operation),
                Map.of("operation", operation)
        );
    }
}
