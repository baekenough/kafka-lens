package com.kafkalens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka Lens 애플리케이션 진입점.
 * <p>
 * Kafka 클러스터 관리 및 모니터링을 위한 웹 애플리케이션입니다.
 * </p>
 *
 * @author kafka-lens-team
 * @since 0.1.0
 */
@SpringBootApplication
public class KafkaLensApplication {

    /**
     * 애플리케이션 메인 메서드.
     *
     * @param args 명령줄 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaLensApplication.class, args);
    }
}
