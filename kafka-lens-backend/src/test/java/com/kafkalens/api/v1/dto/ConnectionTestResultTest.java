package com.kafkalens.api.v1.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConnectionTestResult DTO 테스트.
 */
class ConnectionTestResultTest {

    @Test
    @DisplayName("연결 성공 결과를 생성한다")
    void success_createsSuccessResult() {
        // when
        ConnectionTestResult result = ConnectionTestResult.success("local", "Local Cluster", 150L);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.clusterId()).isEqualTo("local");
        assertThat(result.clusterName()).isEqualTo("Local Cluster");
        assertThat(result.responseTimeMs()).isEqualTo(150L);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.testedAt()).isNotNull();
    }

    @Test
    @DisplayName("연결 실패 결과를 생성한다 (에러 메시지만)")
    void failure_createsFailureResult() {
        // when
        ConnectionTestResult result = ConnectionTestResult.failure(
                "local", "Local Cluster", "Connection refused"
        );

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.clusterId()).isEqualTo("local");
        assertThat(result.clusterName()).isEqualTo("Local Cluster");
        assertThat(result.responseTimeMs()).isNull();
        assertThat(result.errorMessage()).isEqualTo("Connection refused");
        assertThat(result.testedAt()).isNotNull();
    }

    @Test
    @DisplayName("연결 실패 결과를 생성한다 (응답 시간 포함)")
    void failure_withResponseTime_createsFailureResult() {
        // when
        ConnectionTestResult result = ConnectionTestResult.failure(
                "local", "Local Cluster", 5000L, "Connection timeout"
        );

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.clusterId()).isEqualTo("local");
        assertThat(result.clusterName()).isEqualTo("Local Cluster");
        assertThat(result.responseTimeMs()).isEqualTo(5000L);
        assertThat(result.errorMessage()).isEqualTo("Connection timeout");
        assertThat(result.testedAt()).isNotNull();
    }

    @Test
    @DisplayName("testedAt은 자동으로 현재 시각으로 설정된다")
    void testedAt_isSetAutomatically() {
        // when
        ConnectionTestResult result1 = ConnectionTestResult.success("local", "Local", 100L);
        ConnectionTestResult result2 = ConnectionTestResult.failure("local", "Local", "Error");

        // then
        assertThat(result1.testedAt()).isNotNull();
        assertThat(result2.testedAt()).isNotNull();
    }
}
