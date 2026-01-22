package com.kafkalens.domain.message;

/**
 * 메시지 조회 요청.
 *
 * <p>메시지 조회 시 사용되는 파라미터를 정의합니다.</p>
 *
 * @param topicName 토픽 이름
 * @param partition 파티션 번호 (null이면 모든 파티션)
 * @param offset    시작 오프셋 (null이면 처음부터)
 * @param limit     조회 제한 (기본 100, 최대 1000)
 */
public record MessageFetchRequest(
        String topicName,
        Integer partition,
        Long offset,
        Integer limit
) {

    /**
     * MessageFetchRequest 빌더를 생성합니다.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * MessageFetchRequest 빌더 클래스.
     */
    public static class Builder {
        private String topicName;
        private Integer partition;
        private Long offset;
        private Integer limit;

        /**
         * 토픽 이름을 설정합니다.
         *
         * @param topicName 토픽 이름
         * @return Builder
         */
        public Builder topicName(String topicName) {
            this.topicName = topicName;
            return this;
        }

        /**
         * 파티션 번호를 설정합니다.
         *
         * @param partition 파티션 번호
         * @return Builder
         */
        public Builder partition(Integer partition) {
            this.partition = partition;
            return this;
        }

        /**
         * 시작 오프셋을 설정합니다.
         *
         * @param offset 시작 오프셋
         * @return Builder
         */
        public Builder offset(Long offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 조회 제한을 설정합니다.
         *
         * @param limit 조회 제한
         * @return Builder
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * MessageFetchRequest를 빌드합니다.
         *
         * @return MessageFetchRequest 인스턴스
         */
        public MessageFetchRequest build() {
            return new MessageFetchRequest(topicName, partition, offset, limit);
        }
    }
}
