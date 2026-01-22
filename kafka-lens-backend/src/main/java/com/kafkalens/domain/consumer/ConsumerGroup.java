package com.kafkalens.domain.consumer;

import java.util.List;

/**
 * 컨슈머 그룹 정보.
 *
 * <p>Kafka 컨슈머 그룹의 상세 정보를 나타냅니다.
 * 그룹 ID, 상태, 코디네이터, 멤버 정보를 포함합니다.</p>
 *
 * <h2>컨슈머 그룹 상태</h2>
 * <ul>
 *   <li><b>Stable</b>: 정상 상태, 모든 멤버가 파티션 할당을 완료</li>
 *   <li><b>PreparingRebalance</b>: 리밸런싱 준비 중</li>
 *   <li><b>CompletingRebalance</b>: 리밸런싱 완료 중</li>
 *   <li><b>Empty</b>: 활성 멤버 없음, 커밋된 오프셋만 존재</li>
 *   <li><b>Dead</b>: 그룹이 삭제됨</li>
 *   <li><b>Unknown</b>: 알 수 없는 상태</li>
 * </ul>
 *
 * @param groupId     그룹 ID
 * @param state       그룹 상태
 * @param coordinator 코디네이터 브로커 ID
 * @param memberCount 멤버 수
 * @param members     멤버 목록
 */
public record ConsumerGroup(
        String groupId,
        String state,
        int coordinator,
        int memberCount,
        List<ConsumerMember> members
) {
    /**
     * ConsumerGroup 생성자.
     *
     * @param groupId     그룹 ID (null 불가)
     * @param state       상태 (null 불가)
     * @param coordinator 코디네이터 브로커 ID
     * @param memberCount 멤버 수
     * @param members     멤버 목록 (null이면 빈 리스트로 대체)
     */
    public ConsumerGroup {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID cannot be null or blank");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State cannot be null or blank");
        }
        members = members != null ? List.copyOf(members) : List.of();
    }

    /**
     * 그룹이 안정 상태인지 확인합니다.
     *
     * @return Stable 상태이면 true
     */
    public boolean isStable() {
        return "Stable".equals(state);
    }

    /**
     * 그룹이 리밸런싱 중인지 확인합니다.
     *
     * @return PreparingRebalance 또는 CompletingRebalance 상태이면 true
     */
    public boolean isRebalancing() {
        return "PreparingRebalance".equals(state) || "CompletingRebalance".equals(state);
    }

    /**
     * 그룹이 비어있는지 확인합니다.
     *
     * @return Empty 상태이면 true
     */
    public boolean isEmpty() {
        return "Empty".equals(state);
    }

    /**
     * 그룹이 Dead 상태인지 확인합니다.
     *
     * @return Dead 상태이면 true
     */
    public boolean isDead() {
        return "Dead".equals(state);
    }

    /**
     * ConsumerGroup 빌더.
     *
     * @return Builder 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ConsumerGroup 빌더 클래스.
     */
    public static class Builder {
        private String groupId;
        private String state;
        private int coordinator;
        private int memberCount;
        private List<ConsumerMember> members;

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder coordinator(int coordinator) {
            this.coordinator = coordinator;
            return this;
        }

        public Builder memberCount(int memberCount) {
            this.memberCount = memberCount;
            return this;
        }

        public Builder members(List<ConsumerMember> members) {
            this.members = members;
            return this;
        }

        public ConsumerGroup build() {
            return new ConsumerGroup(groupId, state, coordinator, memberCount, members);
        }
    }
}
