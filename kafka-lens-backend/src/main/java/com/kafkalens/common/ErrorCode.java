package com.kafkalens.common;

/**
 * API 에러 코드 상수.
 *
 * <p>클라이언트와 서버 간의 에러 유형을 표준화합니다.
 * 각 에러 코드는 HTTP 상태 코드와 연관됩니다.</p>
 */
public final class ErrorCode {

    private ErrorCode() {
        // 인스턴스화 방지
    }

    // === 일반 에러 (4xx) ===

    /**
     * 잘못된 요청 (400)
     */
    public static final String BAD_REQUEST = "BAD_REQUEST";

    /**
     * 인증 필요 (401)
     */
    public static final String UNAUTHORIZED = "UNAUTHORIZED";

    /**
     * 접근 거부 (403)
     */
    public static final String FORBIDDEN = "FORBIDDEN";

    /**
     * 리소스 없음 (404)
     */
    public static final String NOT_FOUND = "NOT_FOUND";

    /**
     * 요청 검증 실패 (400)
     */
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    // === 클러스터 관련 에러 ===

    /**
     * 클러스터를 찾을 수 없음
     */
    public static final String CLUSTER_NOT_FOUND = "CLUSTER_NOT_FOUND";

    /**
     * 클러스터 연결 실패
     */
    public static final String CLUSTER_CONNECTION_ERROR = "CLUSTER_CONNECTION_ERROR";

    /**
     * 클러스터 설정 오류
     */
    public static final String CLUSTER_CONFIG_ERROR = "CLUSTER_CONFIG_ERROR";

    // === Kafka 관련 에러 ===

    /**
     * Kafka 연결 에러
     */
    public static final String KAFKA_CONNECTION_ERROR = "KAFKA_CONNECTION_ERROR";

    /**
     * Kafka 타임아웃
     */
    public static final String KAFKA_TIMEOUT = "KAFKA_TIMEOUT";

    /**
     * Kafka 인증 실패
     */
    public static final String KAFKA_AUTH_ERROR = "KAFKA_AUTH_ERROR";

    /**
     * Kafka 권한 없음
     */
    public static final String KAFKA_AUTHORIZATION_ERROR = "KAFKA_AUTHORIZATION_ERROR";

    /**
     * 토픽을 찾을 수 없음
     */
    public static final String TOPIC_NOT_FOUND = "TOPIC_NOT_FOUND";

    /**
     * 토픽 이미 존재
     */
    public static final String TOPIC_ALREADY_EXISTS = "TOPIC_ALREADY_EXISTS";

    /**
     * 컨슈머 그룹을 찾을 수 없음
     */
    public static final String CONSUMER_GROUP_NOT_FOUND = "CONSUMER_GROUP_NOT_FOUND";

    /**
     * 브로커를 찾을 수 없음
     */
    public static final String BROKER_NOT_FOUND = "BROKER_NOT_FOUND";

    // === 서버 에러 (5xx) ===

    /**
     * 내부 서버 에러 (500)
     */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * 서비스 사용 불가 (503)
     */
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
}
