package com.kafkalens.domain.cluster;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kafka 클러스터 엔티티.
 *
 * <p>클러스터 연결 정보와 설정을 포함합니다.
 * YAML 설정 파일에서 로드됩니다.</p>
 *
 * @param id               클러스터 고유 식별자
 * @param name             클러스터 표시 이름
 * @param description      클러스터 설명 (선택사항)
 * @param environment      환경 (development, staging, production)
 * @param bootstrapServers 부트스트랩 서버 목록
 * @param security         보안 설정
 * @param properties       추가 Admin Client 프로퍼티
 */
public record Cluster(
        String id,
        String name,
        String description,
        String environment,
        List<String> bootstrapServers,
        SecurityConfig security,
        Map<String, String> properties
) {
    /**
     * Cluster 생성자.
     * 불변 컬렉션으로 변환합니다.
     */
    public Cluster {
        Objects.requireNonNull(id, "Cluster id must not be null");
        Objects.requireNonNull(name, "Cluster name must not be null");
        Objects.requireNonNull(bootstrapServers, "Bootstrap servers must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Cluster id must not be blank");
        }
        if (bootstrapServers.isEmpty()) {
            throw new IllegalArgumentException("Bootstrap servers must not be empty");
        }

        bootstrapServers = List.copyOf(bootstrapServers);
        properties = properties != null ? Map.copyOf(properties) : Collections.emptyMap();
        security = security != null ? security : SecurityConfig.plaintext();
    }

    /**
     * 부트스트랩 서버 목록을 쉼표로 구분된 문자열로 반환합니다.
     *
     * @return 부트스트랩 서버 문자열 (예: "localhost:9092,localhost:9093")
     */
    public String getBootstrapServersAsString() {
        return String.join(",", bootstrapServers);
    }

    /**
     * 프로덕션 환경인지 확인합니다.
     *
     * @return 프로덕션 환경이면 true
     */
    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    /**
     * 보안 연결(SSL/SASL)을 사용하는지 확인합니다.
     *
     * @return 보안 연결이면 true
     */
    public boolean isSecure() {
        return security != null && security.isSecure();
    }

    /**
     * 보안 설정 레코드.
     *
     * @param protocol      보안 프로토콜 (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
     * @param sasl          SASL 설정 (선택사항)
     * @param ssl           SSL 설정 (선택사항)
     */
    public record SecurityConfig(
            String protocol,
            SaslConfig sasl,
            SslConfig ssl
    ) {
        /**
         * PLAINTEXT 보안 설정을 생성합니다.
         */
        public static SecurityConfig plaintext() {
            return new SecurityConfig("PLAINTEXT", null, null);
        }

        /**
         * 보안 프로토콜인지 확인합니다.
         */
        public boolean isSecure() {
            return protocol != null && !protocol.equalsIgnoreCase("PLAINTEXT");
        }

        /**
         * SASL을 사용하는지 확인합니다.
         */
        public boolean usesSasl() {
            return protocol != null && protocol.toUpperCase().contains("SASL");
        }

        /**
         * SSL을 사용하는지 확인합니다.
         */
        public boolean usesSsl() {
            return protocol != null && protocol.toUpperCase().contains("SSL");
        }
    }

    /**
     * SASL 설정 레코드.
     *
     * @param mechanism SASL 메커니즘 (PLAIN, SCRAM-SHA-256, SCRAM-SHA-512)
     * @param username  SASL 사용자명
     * @param password  SASL 비밀번호
     */
    public record SaslConfig(
            String mechanism,
            String username,
            String password
    ) {}

    /**
     * SSL 설정 레코드.
     *
     * @param truststoreLocation 트러스트스토어 경로
     * @param truststorePassword 트러스트스토어 비밀번호
     * @param keystoreLocation   키스토어 경로
     * @param keystorePassword   키스토어 비밀번호
     * @param keyPassword        키 비밀번호
     */
    public record SslConfig(
            String truststoreLocation,
            String truststorePassword,
            String keystoreLocation,
            String keystorePassword,
            String keyPassword
    ) {}

    /**
     * Cluster 빌더를 생성합니다.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cluster 빌더 클래스.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String environment = "development";
        private List<String> bootstrapServers;
        private SecurityConfig security;
        private Map<String, String> properties;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder bootstrapServers(List<String> bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
            return this;
        }

        public Builder bootstrapServers(String... bootstrapServers) {
            this.bootstrapServers = List.of(bootstrapServers);
            return this;
        }

        public Builder security(SecurityConfig security) {
            this.security = security;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Cluster build() {
            return new Cluster(id, name, description, environment, bootstrapServers, security, properties);
        }
    }
}
