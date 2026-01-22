package com.kafkalens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kafka Lens 애플리케이션 통합 테스트.
 * <p>
 * 애플리케이션 컨텍스트 로딩을 검증합니다.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class KafkaLensApplicationTests {

    /**
     * 스프링 애플리케이션 컨텍스트가 정상적으로 로드되는지 검증합니다.
     */
    @Test
    void contextLoads() {
        // 컨텍스트 로드 성공 시 테스트 통과
    }
}
