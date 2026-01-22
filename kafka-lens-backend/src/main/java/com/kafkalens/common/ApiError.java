package com.kafkalens.common;

import java.util.Collections;
import java.util.Map;

/**
 * API 에러 응답 객체.
 *
 * <p>클라이언트에게 에러 정보를 구조화된 형태로 전달합니다.
 * 에러 코드, 메시지, 추가 상세 정보를 포함합니다.</p>
 *
 * @param code    에러 코드 (예: "CLUSTER_NOT_FOUND", "KAFKA_CONNECTION_ERROR")
 * @param message 사용자에게 표시할 에러 메시지
 * @param details 추가 에러 상세 정보 (필드 검증 오류 등)
 */
public record ApiError(
        String code,
        String message,
        Map<String, Object> details
) {
    /**
     * ApiError 생성자.
     * details가 null인 경우 빈 맵으로 대체합니다.
     * 원본 맵의 변경이 ApiError에 영향을 주지 않도록 복사본을 생성합니다.
     */
    public ApiError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code must not be null or blank");
        }
        if (message == null) {
            throw new IllegalArgumentException("Error message must not be null");
        }
        details = details != null ? Collections.unmodifiableMap(new java.util.HashMap<>(details)) : Collections.emptyMap();
    }

    /**
     * 상세 정보 없이 ApiError를 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @return ApiError 인스턴스
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    /**
     * 상세 정보를 포함한 ApiError를 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param details 상세 정보
     * @return ApiError 인스턴스
     */
    public static ApiError of(String code, String message, Map<String, Object> details) {
        return new ApiError(code, message, details);
    }
}
