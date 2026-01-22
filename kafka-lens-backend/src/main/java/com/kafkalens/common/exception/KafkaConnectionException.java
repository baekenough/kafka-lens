package com.kafkalens.common.exception;

import com.kafkalens.common.ErrorCode;

import java.util.Map;

/**
 * Kafka 연결 실패 시 발생하는 예외.
 */
public class KafkaConnectionException extends KafkaLensException {

    /**
     * 클러스터 ID와 원인으로 예외를 생성합니다.
     *
     * @param clusterId 연결 실패한 클러스터 ID
     * @param cause     원인 예외
     */
    public KafkaConnectionException(String clusterId, Throwable cause) {
        super(
                ErrorCode.KAFKA_CONNECTION_ERROR,
                String.format("Failed to connect to Kafka cluster: %s", clusterId),
                Map.of("clusterId", clusterId),
                cause
        );
    }

    /**
     * 클러스터 ID와 메시지로 예외를 생성합니다.
     *
     * @param clusterId 연결 실패한 클러스터 ID
     * @param message   상세 메시지
     */
    public KafkaConnectionException(String clusterId, String message) {
        super(
                ErrorCode.KAFKA_CONNECTION_ERROR,
                message,
                Map.of("clusterId", clusterId)
        );
    }
}
