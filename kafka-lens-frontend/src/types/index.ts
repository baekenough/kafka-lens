/**
 * Kafka Lens 프론트엔드 타입 정의
 *
 * 백엔드 API 응답 및 도메인 모델 타입
 */

// ============================================================================
// 공통 타입
// ============================================================================

/**
 * API 응답 래퍼 타입
 * 모든 API 응답은 이 형식을 따름
 *
 * @template T - 응답 데이터 타입
 */
export interface ApiResponse<T> {
  /** 요청 성공 여부 */
  success: boolean;
  /** 응답 데이터 */
  data: T | null;
  /** 에러 정보 (실패 시) */
  error: ApiError | null;
  /** 응답 메타데이터 */
  meta?: ResponseMeta;
}

/**
 * API 에러 타입
 */
export interface ApiError {
  /** 에러 코드 */
  code: string;
  /** 에러 메시지 */
  message: string;
  /** 상세 정보 (선택적) */
  details?: Record<string, unknown>;
}

/**
 * 응답 메타데이터
 */
export interface ResponseMeta {
  /** 총 항목 수 (페이지네이션) */
  total?: number;
  /** 현재 페이지 */
  page?: number;
  /** 페이지당 항목 수 */
  pageSize?: number;
  /** 응답 시간 (ms) */
  responseTime?: number;
}

/**
 * 페이지네이션 요청 파라미터
 */
export interface PaginationParams {
  /** 페이지 번호 (1부터 시작) */
  page: number;
  /** 페이지당 항목 수 */
  pageSize: number;
  /** 정렬 필드 */
  sortBy?: string;
  /** 정렬 방향 */
  sortOrder?: 'asc' | 'desc';
}

// ============================================================================
// Kafka 클러스터 관련 타입
// ============================================================================

/**
 * Kafka 클러스터 연결 상태
 */
export type ClusterStatus = 'connected' | 'disconnected' | 'connecting' | 'error';

/**
 * Kafka 클러스터 정보
 */
export interface Cluster {
  /** 클러스터 고유 ID */
  id: string;
  /** 클러스터 이름 (사용자 정의) */
  name: string;
  /** Bootstrap 서버 주소 목록 */
  bootstrapServers: string[];
  /** 연결 상태 */
  status: ClusterStatus;
  /** 클러스터 내 브로커 목록 */
  brokers: Broker[];
  /** 클러스터 생성 시간 */
  createdAt: string;
  /** 마지막 연결 시간 */
  lastConnectedAt?: string;
  /** 인증 설정 (선택적) */
  auth?: ClusterAuth;
}

/**
 * 클러스터 인증 설정
 */
export interface ClusterAuth {
  /** 인증 방식 */
  type: 'none' | 'sasl_plain' | 'sasl_scram' | 'ssl';
  /** 사용자명 (SASL 인증 시) */
  username?: string;
  /** SSL 인증서 경로 */
  sslCertPath?: string;
}

/**
 * 클러스터 등록 요청
 */
export interface ClusterCreateRequest {
  /** 클러스터 이름 */
  name: string;
  /** Bootstrap 서버 주소 (콤마 구분) */
  bootstrapServers: string;
  /** 인증 설정 */
  auth?: ClusterAuth;
}

// ============================================================================
// 브로커 관련 타입
// ============================================================================

/**
 * 브로커 상태
 */
export type BrokerStatus = 'online' | 'offline' | 'unknown';

/**
 * Kafka 브로커 정보
 */
export interface Broker {
  /** 브로커 ID */
  id: number;
  /** 호스트 주소 */
  host: string;
  /** 포트 번호 */
  port: number;
  /** 브로커 상태 */
  status: BrokerStatus;
  /** 컨트롤러 여부 */
  isController: boolean;
  /** 랙 ID (선택적) */
  rack?: string;
  /** 브로커 설정 (선택적) */
  configs?: Record<string, string>;
}

// ============================================================================
// 토픽 관련 타입
// ============================================================================

/**
 * 토픽 정리 정책
 */
export type CleanupPolicy = 'delete' | 'compact' | 'delete,compact';

/**
 * Kafka 토픽 정보
 */
export interface Topic {
  /** 토픽 이름 */
  name: string;
  /** 파티션 수 */
  partitionCount: number;
  /** 복제 팩터 */
  replicationFactor: number;
  /** 파티션 상세 정보 */
  partitions: Partition[];
  /** 내부 토픽 여부 (__consumer_offsets 등) */
  isInternal: boolean;
  /** 정리 정책 */
  cleanupPolicy: CleanupPolicy;
  /** 보존 기간 (ms) */
  retentionMs: number;
  /** 보존 용량 (bytes) */
  retentionBytes: number;
  /** 토픽 설정 */
  configs: Record<string, string>;
}

/**
 * 토픽 생성 요청
 */
export interface TopicCreateRequest {
  /** 토픽 이름 */
  name: string;
  /** 파티션 수 */
  partitions: number;
  /** 복제 팩터 */
  replicationFactor: number;
  /** 토픽 설정 (선택적) */
  configs?: Record<string, string>;
}

/**
 * 토픽 목록 필터
 */
export interface TopicListFilter {
  /** 이름 검색어 */
  search?: string;
  /** 내부 토픽 포함 여부 */
  includeInternal?: boolean;
}

// ============================================================================
// 파티션 관련 타입
// ============================================================================

/**
 * Kafka 파티션 정보
 */
export interface Partition {
  /** 파티션 ID */
  id: number;
  /** 리더 브로커 ID */
  leader: number;
  /** 복제본 브로커 ID 목록 */
  replicas: number[];
  /** ISR (In-Sync Replicas) 브로커 ID 목록 */
  isr: number[];
  /** 가장 낮은 오프셋 */
  beginningOffset: number;
  /** 가장 높은 오프셋 */
  endOffset: number;
  /** 메시지 수 (endOffset - beginningOffset) */
  messageCount: number;
  /** 언더 레플리케이션 여부 */
  isUnderReplicated: boolean;
}

// ============================================================================
// 컨슈머 그룹 관련 타입
// ============================================================================

/**
 * 컨슈머 그룹 상태
 */
export type ConsumerGroupState =
  | 'Stable'
  | 'PreparingRebalance'
  | 'CompletingRebalance'
  | 'Empty'
  | 'Dead'
  | 'Unknown';

/**
 * Kafka 컨슈머 그룹 정보
 */
export interface ConsumerGroup {
  /** 그룹 ID */
  groupId: string;
  /** 그룹 상태 */
  state: ConsumerGroupState;
  /** 프로토콜 타입 */
  protocolType: string;
  /** 코디네이터 브로커 ID */
  coordinator: number;
  /** 멤버 수 */
  memberCount: number;
  /** 구독 토픽 목록 */
  topics: string[];
  /** 멤버 상세 정보 */
  members: ConsumerGroupMember[];
  /** 총 Lag */
  totalLag: number;
}

/**
 * 컨슈머 그룹 멤버 정보
 */
export interface ConsumerGroupMember {
  /** 멤버 ID */
  memberId: string;
  /** 클라이언트 ID */
  clientId: string;
  /** 클라이언트 호스트 */
  clientHost: string;
  /** 할당된 파티션 목록 */
  assignments: MemberAssignment[];
}

/**
 * 멤버 파티션 할당 정보
 */
export interface MemberAssignment {
  /** 토픽 이름 */
  topic: string;
  /** 파티션 ID 목록 */
  partitions: number[];
}

// ============================================================================
// 컨슈머 Lag 관련 타입
// ============================================================================

/**
 * 컨슈머 Lag 정보
 */
export interface ConsumerLag {
  /** 파티션 ID */
  partition: number;
  /** 현재 오프셋 (커밋된) */
  currentOffset: number;
  /** 로그 종료 오프셋 */
  endOffset: number;
  /** Lag (endOffset - currentOffset) */
  lag: number;
  /** 상태 (normal, warning, critical) */
  status: string;
}

/**
 * 토픽별 Lag 요약
 */
export interface TopicLagSummary {
  /** 토픽 이름 */
  topic: string;
  /** 총 Lag */
  totalLag: number;
  /** 파티션별 Lag 목록 */
  partitions: ConsumerLag[];
}

/**
 * 컨슈머 그룹 Lag 요약
 */
export interface ConsumerGroupLagSummary {
  /** 그룹 ID */
  groupId: string;
  /** 총 Lag */
  totalLag: number;
  /** 토픽별 Lag 요약 */
  topics: TopicLagSummary[];
}

// ============================================================================
// 메시지 관련 타입
// ============================================================================

/**
 * Kafka 메시지
 */
export interface Message {
  /** 토픽 이름 */
  topic: string;
  /** 파티션 ID */
  partition: number;
  /** 오프셋 */
  offset: number;
  /** 타임스탬프 */
  timestamp: string;
  /** 타임스탬프 타입 */
  timestampType: 'CreateTime' | 'LogAppendTime' | 'NoTimestamp';
  /** 메시지 키 (null 가능) */
  key: string | null;
  /** 메시지 값 */
  value: string;
  /** 헤더 목록 */
  headers: MessageHeader[];
}

/**
 * 메시지 헤더
 */
export interface MessageHeader {
  /** 헤더 키 */
  key: string;
  /** 헤더 값 */
  value: string;
}

/**
 * 메시지 검색 요청
 */
export interface MessageSearchRequest {
  /** 토픽 이름 */
  topic: string;
  /** 파티션 ID (선택적, 미지정 시 모든 파티션) */
  partition?: number;
  /** 시작 오프셋 */
  startOffset?: number;
  /** 검색 시작 시간 */
  startTime?: string;
  /** 검색 종료 시간 */
  endTime?: string;
  /** 키 필터 */
  keyFilter?: string;
  /** 값 필터 (정규식) */
  valueFilter?: string;
  /** 최대 결과 수 */
  limit: number;
}

/**
 * 메시지 발행 요청
 */
export interface MessagePublishRequest {
  /** 토픽 이름 */
  topic: string;
  /** 파티션 ID (선택적) */
  partition?: number;
  /** 메시지 키 */
  key?: string;
  /** 메시지 값 */
  value: string;
  /** 헤더 목록 */
  headers?: MessageHeader[];
}

// ============================================================================
// UI 상태 타입
// ============================================================================

/**
 * 로딩 상태
 */
export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

/**
 * 알림 타입
 */
export type NotificationType = 'info' | 'success' | 'warning' | 'error';

/**
 * 알림 메시지
 */
export interface Notification {
  /** 고유 ID */
  id: string;
  /** 알림 타입 */
  type: NotificationType;
  /** 제목 */
  title: string;
  /** 메시지 */
  message: string;
  /** 자동 닫힘 시간 (ms, 0이면 수동 닫힘) */
  duration?: number;
}

/**
 * 정렬 설정
 */
export interface SortConfig {
  /** 정렬 필드 */
  field: string;
  /** 정렬 방향 */
  direction: 'asc' | 'desc';
}
