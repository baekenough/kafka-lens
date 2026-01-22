package com.kafkalens.domain.consumer;

import java.util.List;

/**
 * 컨슈머 그룹 멤버 정보.
 *
 * <p>컨슈머 그룹의 개별 멤버(클라이언트) 정보를 나타냅니다.
 * 멤버 ID, 클라이언트 ID, 호스트, 할당된 파티션 정보를 포함합니다.</p>
 *
 * @param memberId    멤버 고유 ID
 * @param clientId    클라이언트 ID
 * @param host        클라이언트 호스트 주소
 * @param assignments 할당된 토픽 파티션 목록
 */
public record ConsumerMember(
        String memberId,
        String clientId,
        String host,
        List<TopicPartitionAssignment> assignments
) {
    /**
     * ConsumerMember 생성자.
     *
     * @param memberId    멤버 ID (null 불가)
     * @param clientId    클라이언트 ID (null 불가)
     * @param host        호스트 주소 (null 불가)
     * @param assignments 할당 목록 (null이면 빈 리스트로 대체)
     */
    public ConsumerMember {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID cannot be null or blank");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID cannot be null or blank");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank");
        }
        assignments = assignments != null ? List.copyOf(assignments) : List.of();
    }

    /**
     * ConsumerMember 빌더.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ConsumerMember 빌더 클래스.
     */
    public static class Builder {
        private String memberId;
        private String clientId;
        private String host;
        private List<TopicPartitionAssignment> assignments;

        public Builder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder assignments(List<TopicPartitionAssignment> assignments) {
            this.assignments = assignments;
            return this;
        }

        public ConsumerMember build() {
            return new ConsumerMember(memberId, clientId, host, assignments);
        }
    }
}
