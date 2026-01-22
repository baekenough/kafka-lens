package com.kafkalens.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * API 응답 래퍼 클래스.
 *
 * <p>모든 API 응답을 일관된 형식으로 반환합니다.
 * 성공/실패 여부, 데이터, 에러 정보, 타임스탬프를 포함합니다.</p>
 *
 * @param <T>       응답 데이터 타입
 * @param success   요청 성공 여부
 * @param data      성공 시 응답 데이터 (실패 시 null)
 * @param error     실패 시 에러 정보 (성공 시 null)
 * @param timestamp 응답 생성 시각
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp
) {
    /**
     * ApiResponse 생성자.
     * timestamp가 null인 경우 현재 시각으로 설정합니다.
     */
    public ApiResponse {
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    /**
     * 성공 응답을 생성합니다.
     *
     * @param data 응답 데이터
     * @param <T>  데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    /**
     * 데이터 없는 성공 응답을 생성합니다.
     *
     * @param <T> 데이터 타입
     * @return 성공 ApiResponse (data가 null)
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }

    /**
     * 에러 응답을 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param <T>     데이터 타입
     * @return 에러 ApiResponse
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, ApiError.of(code, message), Instant.now());
    }

    /**
     * 상세 정보를 포함한 에러 응답을 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param details 상세 정보
     * @param <T>     데이터 타입
     * @return 에러 ApiResponse
     */
    public static <T> ApiResponse<T> error(String code, String message, java.util.Map<String, Object> details) {
        return new ApiResponse<>(false, null, ApiError.of(code, message, details), Instant.now());
    }

    /**
     * ApiError 객체로 에러 응답을 생성합니다.
     *
     * @param apiError ApiError 객체
     * @param <T>      데이터 타입
     * @return 에러 ApiResponse
     */
    public static <T> ApiResponse<T> error(ApiError apiError) {
        return new ApiResponse<>(false, null, apiError, Instant.now());
    }
}
