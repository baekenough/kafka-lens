package com.kafkalens.domain.broker;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka 브로커 정보.
 *
 * <p>클러스터 내 브로커의 기본 정보를 나타냅니다.
 * 브로커 ID, 호스트, 포트, 랙 정보 및 컨트롤러 여부를 포함합니다.</p>
 *
 * @param id           브로커 ID
 * @param host         브로커 호스트명
 * @param port         브로커 포트
 * @param rack         브로커 랙 정보 (null 가능)
 * @param isController 컨트롤러 여부
 */
public record Broker(
        int id,
        String host,
        int port,
        String rack,
        @JsonProperty("controller") boolean isController
) {
    /**
     * Broker 생성자.
     *
     * @param id           브로커 ID (0 이상)
     * @param host         브로커 호스트명 (필수)
     * @param port         브로커 포트 (1-65535)
     * @param rack         브로커 랙 정보 (null 가능)
     * @param isController 컨트롤러 여부
     * @throws IllegalArgumentException 유효하지 않은 값이 전달된 경우
     */
    public Broker {
        if (id < 0) {
            throw new IllegalArgumentException("Broker ID must be non-negative");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Broker host cannot be null or blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Broker port must be between 1 and 65535");
        }
    }

    /**
     * Builder를 사용하여 Broker를 생성합니다.
     *
     * @return BrokerBuilder 인스턴스
     */
    public static BrokerBuilder builder() {
        return new BrokerBuilder();
    }

    /**
     * Broker Builder 클래스.
     */
    public static class BrokerBuilder {
        private int id;
        private String host;
        private int port = 9092;
        private String rack;
        private boolean isController = false;

        /**
         * 브로커 ID를 설정합니다.
         *
         * @param id 브로커 ID
         * @return this
         */
        public BrokerBuilder id(int id) {
            this.id = id;
            return this;
        }

        /**
         * 브로커 호스트를 설정합니다.
         *
         * @param host 호스트명
         * @return this
         */
        public BrokerBuilder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * 브로커 포트를 설정합니다.
         *
         * @param port 포트 번호
         * @return this
         */
        public BrokerBuilder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * 브로커 랙을 설정합니다.
         *
         * @param rack 랙 정보
         * @return this
         */
        public BrokerBuilder rack(String rack) {
            this.rack = rack;
            return this;
        }

        /**
         * 컨트롤러 여부를 설정합니다.
         *
         * @param isController 컨트롤러 여부
         * @return this
         */
        public BrokerBuilder isController(boolean isController) {
            this.isController = isController;
            return this;
        }

        /**
         * Broker 인스턴스를 생성합니다.
         *
         * @return Broker 인스턴스
         */
        public Broker build() {
            return new Broker(id, host, port, rack, isController);
        }
    }
}
