package com.kafkalens.common;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.KafkaConnectionException;
import com.kafkalens.common.exception.KafkaLensException;
import com.kafkalens.common.exception.KafkaTimeoutException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 테스트 클래스.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("KafkaLens 예외 처리")
    class KafkaLensExceptionTest {

        @Test
        @DisplayName("ClusterNotFoundException은 404를 반환한다")
        void shouldReturn404ForClusterNotFound() {
            // given
            ClusterNotFoundException ex = new ClusterNotFoundException("test-cluster");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaLensException(ex);

            // then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().success());
            assertEquals(ErrorCode.CLUSTER_NOT_FOUND, response.getBody().error().code());
            assertEquals("test-cluster", response.getBody().error().details().get("clusterId"));
        }

        @Test
        @DisplayName("KafkaConnectionException은 503을 반환한다")
        void shouldReturn503ForKafkaConnectionError() {
            // given
            KafkaConnectionException ex = new KafkaConnectionException("test-cluster", new RuntimeException("Connection refused"));

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaLensException(ex);

            // then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().success());
            assertEquals(ErrorCode.KAFKA_CONNECTION_ERROR, response.getBody().error().code());
        }

        @Test
        @DisplayName("KafkaTimeoutException은 504를 반환한다")
        void shouldReturn504ForKafkaTimeout() {
            // given
            KafkaTimeoutException ex = new KafkaTimeoutException("test-cluster", "listTopics", new RuntimeException());

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaLensException(ex);

            // then
            assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().success());
            assertEquals(ErrorCode.KAFKA_TIMEOUT, response.getBody().error().code());
        }

        @Test
        @DisplayName("일반 KafkaLensException은 500을 반환한다")
        void shouldReturn500ForGenericKafkaLensException() {
            // given
            KafkaLensException ex = KafkaLensException.internalError("Something went wrong");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaLensException(ex);

            // then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().success());
            assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().error().code());
        }
    }

    @Nested
    @DisplayName("Kafka 네이티브 예외 처리")
    class KafkaExceptionTest {

        @Test
        @DisplayName("TimeoutException은 504를 반환한다")
        void shouldReturn504ForKafkaTimeout() {
            // given
            TimeoutException ex = new TimeoutException("Call timed out");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaTimeoutException(ex);

            // then
            assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.KAFKA_TIMEOUT, response.getBody().error().code());
        }

        @Test
        @DisplayName("AuthenticationException은 401을 반환한다")
        void shouldReturn401ForAuthenticationError() {
            // given
            AuthenticationException ex = new AuthenticationException("Invalid credentials");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaAuthenticationException(ex);

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.KAFKA_AUTH_ERROR, response.getBody().error().code());
        }

        @Test
        @DisplayName("AuthorizationException은 403을 반환한다")
        void shouldReturn403ForAuthorizationError() {
            // given
            AuthorizationException ex = new AuthorizationException("Access denied");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaAuthorizationException(ex);

            // then
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.KAFKA_AUTHORIZATION_ERROR, response.getBody().error().code());
        }

        @Test
        @DisplayName("일반 KafkaException은 500을 반환한다")
        void shouldReturn500ForGenericKafkaException() {
            // given
            KafkaException ex = new KafkaException("Kafka error");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleKafkaException(ex);

            // then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.KAFKA_CONNECTION_ERROR, response.getBody().error().code());
        }
    }

    @Nested
    @DisplayName("ExecutionException 처리")
    class ExecutionExceptionTest {

        @Test
        @DisplayName("TimeoutException 원인은 504를 반환한다")
        void shouldReturn504ForTimeoutCause() {
            // given
            TimeoutException cause = new TimeoutException("Timed out");
            ExecutionException ex = new ExecutionException(cause);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleExecutionException(ex);

            // then
            assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        }

        @Test
        @DisplayName("AuthenticationException 원인은 401을 반환한다")
        void shouldReturn401ForAuthenticationCause() {
            // given
            AuthenticationException cause = new AuthenticationException("Auth failed");
            ExecutionException ex = new ExecutionException(cause);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleExecutionException(ex);

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("알 수 없는 원인은 500을 반환한다")
        void shouldReturn500ForUnknownCause() {
            // given
            RuntimeException cause = new RuntimeException("Unknown error");
            ExecutionException ex = new ExecutionException(cause);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleExecutionException(ex);

            // then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().error().code());
        }
    }

    @Nested
    @DisplayName("검증 예외 처리")
    class ValidationExceptionTest {

        @Test
        @DisplayName("MethodArgumentNotValidException은 400과 필드 에러를 반환한다")
        void shouldReturn400WithFieldErrors() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError1 = new FieldError("object", "name", "must not be blank");
            FieldError fieldError2 = new FieldError("object", "count", "must be positive");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ErrorCode.VALIDATION_ERROR, response.getBody().error().code());
            assertEquals(2, response.getBody().error().details().size());
            assertEquals("must not be blank", response.getBody().error().details().get("name"));
            assertEquals("must be positive", response.getBody().error().details().get("count"));
        }
    }

    @Nested
    @DisplayName("일반 예외 처리")
    class GenericExceptionTest {

        @Test
        @DisplayName("예상치 못한 예외는 500을 반환한다")
        void shouldReturn500ForUnexpectedException() {
            // given
            Exception ex = new RuntimeException("Unexpected error");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

            // then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().success());
            assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().error().code());
            assertEquals("An unexpected error occurred", response.getBody().error().message());
        }

        @Test
        @DisplayName("InterruptedException은 503을 반환한다")
        void shouldReturn503ForInterruptedException() {
            // given
            InterruptedException ex = new InterruptedException("Interrupted");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleInterruptedException(ex);

            // then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, response.getBody().error().code());
        }
    }
}
