package com.kafkalens.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiError 테스트 클래스.
 */
@DisplayName("ApiError")
class ApiErrorTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("유효한 값으로 ApiError를 생성할 수 있다")
        void shouldCreateApiErrorWithValidValues() {
            // given
            String code = "TEST_ERROR";
            String message = "Test error message";
            Map<String, Object> details = Map.of("field", "value");

            // when
            ApiError error = new ApiError(code, message, details);

            // then
            assertEquals(code, error.code());
            assertEquals(message, error.message());
            assertEquals(details, error.details());
        }

        @Test
        @DisplayName("details가 null인 경우 빈 맵으로 대체된다")
        void shouldReplaceNullDetailsWithEmptyMap() {
            // given
            String code = "TEST_ERROR";
            String message = "Test error message";

            // when
            ApiError error = new ApiError(code, message, null);

            // then
            assertNotNull(error.details());
            assertTrue(error.details().isEmpty());
        }

        @Test
        @DisplayName("code가 null인 경우 예외가 발생한다")
        void shouldThrowExceptionWhenCodeIsNull() {
            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new ApiError(null, "message", null)
            );
            assertEquals("Error code must not be null or blank", exception.getMessage());
        }

        @Test
        @DisplayName("code가 빈 문자열인 경우 예외가 발생한다")
        void shouldThrowExceptionWhenCodeIsBlank() {
            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new ApiError("  ", "message", null)
            );
            assertEquals("Error code must not be null or blank", exception.getMessage());
        }

        @Test
        @DisplayName("message가 null인 경우 예외가 발생한다")
        void shouldThrowExceptionWhenMessageIsNull() {
            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new ApiError("CODE", null, null)
            );
            assertEquals("Error message must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTest {

        @Test
        @DisplayName("of(code, message)로 상세 정보 없이 생성할 수 있다")
        void shouldCreateWithoutDetails() {
            // given
            String code = "TEST_ERROR";
            String message = "Test error message";

            // when
            ApiError error = ApiError.of(code, message);

            // then
            assertEquals(code, error.code());
            assertEquals(message, error.message());
            assertTrue(error.details().isEmpty());
        }

        @Test
        @DisplayName("of(code, message, details)로 상세 정보와 함께 생성할 수 있다")
        void shouldCreateWithDetails() {
            // given
            String code = "VALIDATION_ERROR";
            String message = "Validation failed";
            Map<String, Object> details = Map.of("field", "name", "reason", "required");

            // when
            ApiError error = ApiError.of(code, message, details);

            // then
            assertEquals(code, error.code());
            assertEquals(message, error.message());
            assertEquals(2, error.details().size());
            assertEquals("name", error.details().get("field"));
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("details 맵은 수정할 수 없다")
        void detailsShouldBeUnmodifiable() {
            // given
            Map<String, Object> mutableDetails = new HashMap<>();
            mutableDetails.put("key", "value");
            ApiError error = new ApiError("CODE", "message", mutableDetails);

            // when & then
            assertThrows(UnsupportedOperationException.class, () -> {
                error.details().put("newKey", "newValue");
            });
        }

        @Test
        @DisplayName("원본 맵을 수정해도 ApiError의 details에 영향을 주지 않는다")
        void originalMapModificationShouldNotAffectError() {
            // given
            Map<String, Object> mutableDetails = new HashMap<>();
            mutableDetails.put("key", "value");
            ApiError error = new ApiError("CODE", "message", mutableDetails);

            // when
            mutableDetails.put("newKey", "newValue");

            // then
            assertEquals(1, error.details().size());
            assertFalse(error.details().containsKey("newKey"));
        }
    }
}
