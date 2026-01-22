package com.kafkalens.common.exception;

import com.kafkalens.common.ErrorCode;

import java.util.Collections;
import java.util.Map;

/**
 * Kafka Lens 애플리케이션 기본 예외 클래스.
 *
 * <p>모든 커스텀 예외의 부모 클래스로, 에러 코드와 상세 정보를 포함합니다.</p>
 */
public class KafkaLensException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    /**
     * 기본 생성자.
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     */
    public KafkaLensException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * 원인 예외를 포함한 생성자.
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @param cause     원인 예외
     */
    public KafkaLensException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * 상세 정보를 포함한 생성자.
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @param details   상세 정보
     */
    public KafkaLensException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? Collections.unmodifiableMap(details) : Collections.emptyMap();
    }

    /**
     * 모든 정보를 포함한 생성자.
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @param details   상세 정보
     * @param cause     원인 예외
     */
    public KafkaLensException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? Collections.unmodifiableMap(details) : Collections.emptyMap();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * 내부 서버 에러 예외를 생성합니다.
     *
     * @param message 에러 메시지
     * @return KafkaLensException
     */
    public static KafkaLensException internalError(String message) {
        return new KafkaLensException(ErrorCode.INTERNAL_ERROR, message);
    }

    /**
     * 내부 서버 에러 예외를 원인과 함께 생성합니다.
     *
     * @param message 에러 메시지
     * @param cause   원인 예외
     * @return KafkaLensException
     */
    public static KafkaLensException internalError(String message, Throwable cause) {
        return new KafkaLensException(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
