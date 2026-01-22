/**
 * 토픽 관련 커스텀 훅
 *
 * SWR을 사용한 토픽 데이터 페칭 및 캐싱 관리
 * @module hooks/useTopics
 */
import useSWR, { mutate } from 'swr';
import { topicApi } from '@/lib/api';
import type { Topic, ApiResponse, TopicListFilter } from '@/types';

/**
 * 토픽 상세 정보 타입 (백엔드 응답)
 */
export interface TopicDetail {
  /** 토픽 이름 */
  name: string;
  /** 파티션 수 */
  partitionCount: number;
  /** 복제 팩터 */
  replicationFactor: number;
  /** 내부 토픽 여부 */
  isInternal: boolean;
  /** 파티션 목록 */
  partitions: PartitionInfo[];
  /** 토픽 설정 */
  configs: Record<string, string>;
}

/**
 * 파티션 정보 타입
 */
export interface PartitionInfo {
  /** 파티션 번호 */
  partition: number;
  /** 리더 브로커 ID */
  leader: number;
  /** 레플리카 브로커 ID 목록 */
  replicas: number[];
  /** ISR 브로커 ID 목록 */
  isr: number[];
  /** 시작 오프셋 */
  beginningOffset: number;
  /** 종료 오프셋 */
  endOffset: number;
  /** 메시지 수 (계산값) */
  messageCount: number;
}

/**
 * useTopics 훅 반환 타입
 */
export interface UseTopicsReturn {
  /** 토픽 목록 */
  topics: Topic[];
  /** 로딩 상태 */
  isLoading: boolean;
  /** 에러 정보 */
  error: Error | null;
  /** 데이터 갱신 함수 */
  refresh: () => Promise<void>;
  /** 유효성 검증 상태 */
  isValidating: boolean;
}

/**
 * useTopic 훅 반환 타입
 */
export interface UseTopicReturn {
  /** 토픽 상세 정보 */
  topic: TopicDetail | null;
  /** 로딩 상태 */
  isLoading: boolean;
  /** 에러 정보 */
  error: Error | null;
  /** 데이터 갱신 함수 */
  refresh: () => Promise<void>;
  /** 유효성 검증 상태 */
  isValidating: boolean;
}

/**
 * 토픽 목록 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @param includeInternal - 내부 토픽 포함 여부
 * @returns SWR 키
 */
export const getTopicsKey = (clusterId: string, includeInternal = false) =>
  `/api/clusters/${clusterId}/topics?includeInternal=${includeInternal}`;

/**
 * 토픽 상세 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @param topicName - 토픽 이름
 * @returns SWR 키
 */
export const getTopicKey = (clusterId: string, topicName: string) =>
  `/api/clusters/${clusterId}/topics/${topicName}`;

/**
 * 토픽 목록 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @param filter - 필터 옵션
 * @returns 토픽 목록
 * @throws API 요청 실패 시 에러
 */
async function fetchTopics(clusterId: string, filter?: TopicListFilter): Promise<Topic[]> {
  const response = await topicApi.list(clusterId, filter);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch topics');
  }

  return response.data;
}

/**
 * 토픽 상세 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @param topicName - 토픽 이름
 * @returns 토픽 상세 정보
 * @throws API 요청 실패 시 에러
 */
async function fetchTopic(clusterId: string, topicName: string): Promise<TopicDetail> {
  const response = await topicApi.get(clusterId, topicName);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch topic');
  }

  // 백엔드 응답을 TopicDetail로 매핑 (messageCount 계산)
  const data = response.data as unknown as TopicDetail;
  return {
    ...data,
    partitions: data.partitions?.map((p) => ({
      ...p,
      messageCount: p.endOffset - p.beginningOffset,
    })) || [],
  };
}

/**
 * 클러스터의 토픽 목록을 가져오는 훅
 *
 * SWR을 사용하여 토픽 목록을 가져오고 캐싱합니다.
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @param options - 조회 옵션
 * @param options.includeInternal - 내부 토픽 포함 여부 (기본값: false)
 * @returns 토픽 목록, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function TopicList({ clusterId }: { clusterId: string }) {
 *   const { topics, isLoading, error, refresh } = useTopics(clusterId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} onRetry={refresh} />;
 *
 *   return (
 *     <ul>
 *       {topics.map(topic => (
 *         <li key={topic.name}>{topic.name}</li>
 *       ))}
 *     </ul>
 *   );
 * }
 * ```
 */
export function useTopics(
  clusterId: string | null,
  options: { includeInternal?: boolean } = {}
): UseTopicsReturn {
  const { includeInternal = false } = options;

  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<Topic[]>(
    // clusterId가 null이면 요청하지 않음
    clusterId ? getTopicsKey(clusterId, includeInternal) : null,
    () =>
      clusterId
        ? fetchTopics(clusterId, { includeInternal })
        : Promise.reject(new Error('No cluster ID')),
    {
      // 30초마다 자동 재검증
      refreshInterval: 30 * 1000,
      // 포커스 시 재검증
      revalidateOnFocus: true,
      // 마운트 시 재검증
      revalidateOnMount: true,
      // 에러 시 재시도 횟수
      errorRetryCount: 3,
      // 재시도 간격
      errorRetryInterval: 3000,
    }
  );

  /**
   * 데이터 수동 갱신
   */
  const refresh = async () => {
    await swrMutate();
  };

  return {
    topics: data || [],
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 단일 토픽 상세 정보를 가져오는 훅
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @param topicName - 토픽 이름 (null이면 요청하지 않음)
 * @returns 토픽 상세 정보, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function TopicDetail({ clusterId, topicName }: { clusterId: string; topicName: string }) {
 *   const { topic, isLoading, error } = useTopic(clusterId, topicName);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} />;
 *   if (!topic) return <NotFound />;
 *
 *   return <TopicInfo topic={topic} />;
 * }
 * ```
 */
export function useTopic(
  clusterId: string | null,
  topicName: string | null
): UseTopicReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<TopicDetail>(
    // clusterId와 topicName이 모두 있어야 요청
    clusterId && topicName ? getTopicKey(clusterId, topicName) : null,
    () =>
      clusterId && topicName
        ? fetchTopic(clusterId, topicName)
        : Promise.reject(new Error('No cluster ID or topic name')),
    {
      // 30초마다 자동 재검증
      refreshInterval: 30 * 1000,
      revalidateOnFocus: true,
      errorRetryCount: 3,
    }
  );

  const refresh = async () => {
    await swrMutate();
  };

  return {
    topic: data || null,
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 토픽 캐시 무효화 함수
 *
 * 토픽 생성, 수정, 삭제 후 호출하여 캐시를 갱신합니다.
 *
 * @param clusterId - 클러스터 ID
 *
 * @example
 * ```tsx
 * async function createTopic(clusterId: string, data: TopicCreateRequest) {
 *   await topicApi.create(clusterId, data);
 *   await invalidateTopics(clusterId);
 * }
 * ```
 */
export async function invalidateTopics(clusterId: string): Promise<void> {
  // 내부 토픽 포함/제외 두 가지 캐시 모두 무효화
  await mutate(getTopicsKey(clusterId, false));
  await mutate(getTopicsKey(clusterId, true));
}

/**
 * 특정 토픽 캐시 무효화 함수
 *
 * @param clusterId - 클러스터 ID
 * @param topicName - 토픽 이름
 *
 * @example
 * ```tsx
 * async function updateTopicConfig(clusterId: string, topicName: string, configs: Record<string, string>) {
 *   await topicApi.updateConfigs(clusterId, topicName, configs);
 *   await invalidateTopic(clusterId, topicName);
 * }
 * ```
 */
export async function invalidateTopic(clusterId: string, topicName: string): Promise<void> {
  await mutate(getTopicKey(clusterId, topicName));
}

export default useTopics;
