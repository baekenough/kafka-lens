package com.kafkalens.domain.message;

import java.util.Map;

/**
 * Kafka 메시지 엔티티.
 *
 * <p>Kafka 토픽에서 조회한 메시지를 나타냅니다.
 * 바이너리 데이터는 Base64로 인코딩됩니다.</p>
 *
 * @param topic         토픽 이름
 * @param partition     파티션 번호
 * @param offset        오프셋
 * @param timestamp     타임스탬프 (밀리초)
 * @param timestampType 타임스탬프 타입 (CreateTime, LogAppendTime)
 * @param key           메시지 키 (Base64 또는 문자열, null 가능)
 * @param value         메시지 값 (Base64 또는 문자열, null 가능)
 * @param headers       메시지 헤더
 */
public record KafkaMessage(
        String topic,
        int partition,
        long offset,
        long timestamp,
        String timestampType,
        String key,
        String value,
        Map<String, String> headers
) {

    /**
     * KafkaMessage 빌더를 생성합니다.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * KafkaMessage 빌더 클래스.
     */
    public static class Builder {
        private String topic;
        private int partition;
        private long offset;
        private long timestamp;
        private String timestampType;
        private String key;
        private String value;
        private Map<String, String> headers = Map.of();

        /**
         * 토픽 이름을 설정합니다.
         *
         * @param topic 토픽 이름
         * @return Builder
         */
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * 파티션 번호를 설정합니다.
         *
         * @param partition 파티션 번호
         * @return Builder
         */
        public Builder partition(int partition) {
            this.partition = partition;
            return this;
        }

        /**
         * 오프셋을 설정합니다.
         *
         * @param offset 오프셋
         * @return Builder
         */
        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 타임스탬프를 설정합니다.
         *
         * @param timestamp 타임스탬프 (밀리초)
         * @return Builder
         */
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * 타임스탬프 타입을 설정합니다.
         *
         * @param timestampType 타임스탬프 타입
         * @return Builder
         */
        public Builder timestampType(String timestampType) {
            this.timestampType = timestampType;
            return this;
        }

        /**
         * 메시지 키를 설정합니다.
         *
         * @param key 메시지 키
         * @return Builder
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * 메시지 값을 설정합니다.
         *
         * @param value 메시지 값
         * @return Builder
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * 메시지 헤더를 설정합니다.
         *
         * @param headers 메시지 헤더
         * @return Builder
         */
        public Builder headers(Map<String, String> headers) {
            this.headers = headers != null ? Map.copyOf(headers) : Map.of();
            return this;
        }

        /**
         * KafkaMessage를 빌드합니다.
         *
         * @return KafkaMessage 인스턴스
         */
        public KafkaMessage build() {
            return new KafkaMessage(
                    topic, partition, offset, timestamp,
                    timestampType, key, value, headers
            );
        }
    }
}
