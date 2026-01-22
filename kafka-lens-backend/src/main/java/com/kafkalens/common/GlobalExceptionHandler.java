package com.kafkalens.common;

import com.kafkalens.common.exception.KafkaLensException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 전역 예외 처리기.
 *
 * <p>모든 컨트롤러에서 발생하는 예외를 일관된 ApiResponse 형식으로 변환합니다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // === KafkaLens 커스텀 예외 ===

    /**
     * KafkaLensException 처리.
     */
    @ExceptionHandler(KafkaLensException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaLensException(KafkaLensException ex) {
        log.warn("KafkaLens exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        ApiError error = ApiError.of(ex.getErrorCode(), ex.getMessage(), ex.getDetails());

        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }

    // === Kafka 관련 예외 ===

    /**
     * Kafka TimeoutException 처리.
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaTimeoutException(TimeoutException ex) {
        log.warn("Kafka timeout: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error(ErrorCode.KAFKA_TIMEOUT, "Kafka operation timed out: " + ex.getMessage()));
    }

    /**
     * Kafka AuthenticationException 처리.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaAuthenticationException(AuthenticationException ex) {
        log.warn("Kafka authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.KAFKA_AUTH_ERROR, "Kafka authentication failed"));
    }

    /**
     * Kafka AuthorizationException 처리.
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaAuthorizationException(AuthorizationException ex) {
        log.warn("Kafka authorization failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.KAFKA_AUTHORIZATION_ERROR, "Kafka authorization failed"));
    }

    /**
     * 일반 KafkaException 처리.
     */
    @ExceptionHandler(KafkaException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaException(KafkaException ex) {
        log.error("Kafka exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.KAFKA_CONNECTION_ERROR, "Kafka error: " + ex.getMessage()));
    }

    /**
     * ExecutionException 처리 (Kafka Future 실행 중 발생).
     */
    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleExecutionException(ExecutionException ex) {
        Throwable cause = ex.getCause();

        // 원인 예외를 분석하여 적절한 핸들러로 위임
        if (cause instanceof TimeoutException kafkaTimeout) {
            return handleKafkaTimeoutException(kafkaTimeout);
        }
        if (cause instanceof AuthenticationException authEx) {
            return handleKafkaAuthenticationException(authEx);
        }
        if (cause instanceof AuthorizationException authzEx) {
            return handleKafkaAuthorizationException(authzEx);
        }
        if (cause instanceof KafkaException kafkaEx) {
            return handleKafkaException(kafkaEx);
        }

        log.error("Execution exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Operation failed: " + cause.getMessage()));
    }

    // === 검증 관련 예외 ===

    /**
     * MethodArgumentNotValidException 처리 (@Valid 검증 실패).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> fieldErrors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, "Validation failed", fieldErrors));
    }

    /**
     * MissingServletRequestParameterException 처리.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());

        Map<String, Object> details = Map.of(
                "parameter", ex.getParameterName(),
                "type", ex.getParameterType()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), details));
    }

    /**
     * MethodArgumentTypeMismatchException 처리.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());

        Map<String, Object> details = new HashMap<>();
        details.put("parameter", ex.getName());
        details.put("value", ex.getValue());
        if (ex.getRequiredType() != null) {
            details.put("expectedType", ex.getRequiredType().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Invalid parameter type", details));
    }

    /**
     * HttpMessageNotReadableException 처리 (JSON 파싱 오류 등).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Message not readable: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Invalid request body"));
    }

    // === HTTP 관련 예외 ===

    /**
     * NoHandlerFoundException 처리 (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND, "Resource not found: " + ex.getRequestURL()));
    }

    /**
     * HttpRequestMethodNotSupportedException 처리 (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Method not supported: " + ex.getMethod()));
    }

    /**
     * HttpMediaTypeNotSupportedException 처리 (415).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, "Media type not supported: " + ex.getContentType()));
    }

    // === 기타 예외 ===

    /**
     * InterruptedException 처리.
     */
    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ApiResponse<Void>> handleInterruptedException(InterruptedException ex) {
        log.warn("Operation interrupted: {}", ex.getMessage());
        Thread.currentThread().interrupt();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ErrorCode.SERVICE_UNAVAILABLE, "Operation was interrupted"));
    }

    /**
     * 기타 모든 예외 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    // === 유틸리티 메서드 ===

    /**
     * 에러 코드를 HTTP 상태 코드로 매핑합니다.
     */
    private HttpStatus mapErrorCodeToStatus(String errorCode) {
        return switch (errorCode) {
            case ErrorCode.NOT_FOUND,
                 ErrorCode.CLUSTER_NOT_FOUND,
                 ErrorCode.TOPIC_NOT_FOUND,
                 ErrorCode.CONSUMER_GROUP_NOT_FOUND,
                 ErrorCode.BROKER_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case ErrorCode.BAD_REQUEST,
                 ErrorCode.VALIDATION_ERROR,
                 ErrorCode.CLUSTER_CONFIG_ERROR -> HttpStatus.BAD_REQUEST;

            case ErrorCode.UNAUTHORIZED,
                 ErrorCode.KAFKA_AUTH_ERROR -> HttpStatus.UNAUTHORIZED;

            case ErrorCode.FORBIDDEN,
                 ErrorCode.KAFKA_AUTHORIZATION_ERROR -> HttpStatus.FORBIDDEN;

            case ErrorCode.KAFKA_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;

            case ErrorCode.SERVICE_UNAVAILABLE,
                 ErrorCode.KAFKA_CONNECTION_ERROR,
                 ErrorCode.CLUSTER_CONNECTION_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
