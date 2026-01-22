package com.kafkalens.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 테스트 클래스.
 */
@DisplayName("ApiResponse")
class ApiResponseTest {

    @Nested
    @DisplayName("성공 응답 테스트")
    class SuccessResponseTest {

        @Test
        @DisplayName("데이터와 함께 성공 응답을 생성할 수 있다")
        void shouldCreateSuccessResponseWithData() {
            // given
            String data = "test data";

            // when
            ApiResponse<String> response = ApiResponse.ok(data);

            // then
            assertTrue(response.success());
            assertEquals(data, response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        @DisplayName("복잡한 객체로 성공 응답을 생성할 수 있다")
        void shouldCreateSuccessResponseWithComplexData() {
            // given
            List<String> data = List.of("item1", "item2", "item3");

            // when
            ApiResponse<List<String>> response = ApiResponse.ok(data);

            // then
            assertTrue(response.success());
            assertEquals(3, response.data().size());
            assertNull(response.error());
        }

        @Test
        @DisplayName("데이터 없이 성공 응답을 생성할 수 있다")
        void shouldCreateSuccessResponseWithoutData() {
            // when
            ApiResponse<Void> response = ApiResponse.ok();

            // then
            assertTrue(response.success());
            assertNull(response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        @DisplayName("null 데이터로 성공 응답을 생성할 수 있다")
        void shouldCreateSuccessResponseWithNullData() {
            // when
            ApiResponse<String> response = ApiResponse.ok(null);

            // then
            assertTrue(response.success());
            assertNull(response.data());
            assertNull(response.error());
        }
    }

    @Nested
    @DisplayName("에러 응답 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("코드와 메시지로 에러 응답을 생성할 수 있다")
        void shouldCreateErrorResponseWithCodeAndMessage() {
            // given
            String code = "NOT_FOUND";
            String message = "Resource not found";

            // when
            ApiResponse<String> response = ApiResponse.error(code, message);

            // then
            assertFalse(response.success());
            assertNull(response.data());
            assertNotNull(response.error());
            assertEquals(code, response.error().code());
            assertEquals(message, response.error().message());
            assertNotNull(response.timestamp());
        }

        @Test
        @DisplayName("상세 정보와 함께 에러 응답을 생성할 수 있다")
        void shouldCreateErrorResponseWithDetails() {
            // given
            String code = "VALIDATION_ERROR";
            String message = "Validation failed";
            Map<String, Object> details = Map.of(
                    "field", "name",
                    "constraint", "NotBlank"
            );

            // when
            ApiResponse<Object> response = ApiResponse.error(code, message, details);

            // then
            assertFalse(response.success());
            assertNull(response.data());
            assertNotNull(response.error());
            assertEquals(code, response.error().code());
            assertEquals(2, response.error().details().size());
        }

        @Test
        @DisplayName("ApiError 객체로 에러 응답을 생성할 수 있다")
        void shouldCreateErrorResponseWithApiError() {
            // given
            ApiError apiError = ApiError.of("INTERNAL_ERROR", "Internal server error");

            // when
            ApiResponse<String> response = ApiResponse.error(apiError);

            // then
            assertFalse(response.success());
            assertNull(response.data());
            assertEquals(apiError, response.error());
        }
    }

    @Nested
    @DisplayName("타임스탬프 테스트")
    class TimestampTest {

        @Test
        @DisplayName("타임스탬프가 자동으로 설정된다")
        void shouldSetTimestampAutomatically() {
            // given
            Instant before = Instant.now();

            // when
            ApiResponse<String> response = ApiResponse.ok("data");

            // then
            Instant after = Instant.now();
            assertNotNull(response.timestamp());
            assertFalse(response.timestamp().isBefore(before));
            assertFalse(response.timestamp().isAfter(after));
        }

        @Test
        @DisplayName("생성자에서 null timestamp는 현재 시각으로 대체된다")
        void shouldReplaceNullTimestampWithNow() {
            // given
            Instant before = Instant.now();

            // when
            ApiResponse<String> response = new ApiResponse<>(true, "data", null, null);

            // then
            Instant after = Instant.now();
            assertNotNull(response.timestamp());
            assertFalse(response.timestamp().isBefore(before));
            assertFalse(response.timestamp().isAfter(after));
        }

        @Test
        @DisplayName("명시적 타임스탬프가 유지된다")
        void shouldPreserveExplicitTimestamp() {
            // given
            Instant explicitTime = Instant.parse("2025-01-01T00:00:00Z");

            // when
            ApiResponse<String> response = new ApiResponse<>(true, "data", null, explicitTime);

            // then
            assertEquals(explicitTime, response.timestamp());
        }
    }

    @Nested
    @DisplayName("제네릭 타입 테스트")
    class GenericTypeTest {

        record TestDto(String name, int value) {}

        @Test
        @DisplayName("DTO 객체를 데이터로 사용할 수 있다")
        void shouldWorkWithDtoObject() {
            // given
            TestDto dto = new TestDto("test", 42);

            // when
            ApiResponse<TestDto> response = ApiResponse.ok(dto);

            // then
            assertTrue(response.success());
            assertEquals("test", response.data().name());
            assertEquals(42, response.data().value());
        }

        @Test
        @DisplayName("Map을 데이터로 사용할 수 있다")
        void shouldWorkWithMap() {
            // given
            Map<String, Object> data = Map.of("key1", "value1", "key2", 123);

            // when
            ApiResponse<Map<String, Object>> response = ApiResponse.ok(data);

            // then
            assertTrue(response.success());
            assertEquals("value1", response.data().get("key1"));
            assertEquals(123, response.data().get("key2"));
        }
    }
}
