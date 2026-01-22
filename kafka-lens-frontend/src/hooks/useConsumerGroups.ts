/**
 * 컨슈머 그룹 관련 커스텀 훅
 *
 * SWR을 사용한 데이터 페칭 및 캐싱 관리
 * @module hooks/useConsumerGroups
 */
import useSWR, { mutate } from 'swr';
import { consumerGroupApi } from '@/lib/api';
import type { ConsumerGroup, ApiResponse } from '@/types';

/**
 * useConsumerGroups 훅 반환 타입
 */
export interface UseConsumerGroupsReturn {
  /** 컨슈머 그룹 목록 */
  groups: ConsumerGroup[];
  /** 로딩 상태 */
  isLoading: boolean;
  /** 에러 정보 */
  error: Error | null;
  /** 데이터 갱신 함수 */
  refresh: () => Promise<void>;
  /** 유효성 검증 상태 (revalidating) */
  isValidating: boolean;
}

/**
 * useConsumerGroup 훅 반환 타입
 */
export interface UseConsumerGroupReturn {
  /** 컨슈머 그룹 상세 정보 */
  group: ConsumerGroup | null;
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
 * 컨슈머 그룹 목록 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @returns SWR 키
 */
export const getConsumerGroupsKey = (clusterId: string) =>
  `/api/clusters/${clusterId}/consumer-groups`;

/**
 * 컨슈머 그룹 상세 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @param groupId - 그룹 ID
 * @returns SWR 키
 */
export const getConsumerGroupKey = (clusterId: string, groupId: string) =>
  `/api/clusters/${clusterId}/consumer-groups/${groupId}`;

/**
 * 컨슈머 그룹 목록 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @returns 컨슈머 그룹 목록
 * @throws API 요청 실패 시 에러
 */
async function fetchConsumerGroups(clusterId: string): Promise<ConsumerGroup[]> {
  const response = await consumerGroupApi.list(clusterId);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch consumer groups');
  }

  return response.data;
}

/**
 * 컨슈머 그룹 상세 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @param groupId - 그룹 ID
 * @returns 컨슈머 그룹 상세 정보
 * @throws API 요청 실패 시 에러
 */
async function fetchConsumerGroup(
  clusterId: string,
  groupId: string
): Promise<ConsumerGroup> {
  const response = await consumerGroupApi.get(clusterId, groupId);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch consumer group');
  }

  return response.data;
}

/**
 * 컨슈머 그룹 목록을 가져오는 훅
 *
 * SWR을 사용하여 컨슈머 그룹 목록을 가져오고 캐싱합니다.
 * 자동 재검증 및 포커스 시 리페칭을 지원합니다.
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @returns 컨슈머 그룹 목록, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function ConsumerGroupList({ clusterId }: { clusterId: string }) {
 *   const { groups, isLoading, error, refresh } = useConsumerGroups(clusterId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} onRetry={refresh} />;
 *
 *   return (
 *     <ul>
 *       {groups.map(group => (
 *         <li key={group.groupId}>{group.groupId}</li>
 *       ))}
 *     </ul>
 *   );
 * }
 * ```
 */
export function useConsumerGroups(clusterId: string | null): UseConsumerGroupsReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<ConsumerGroup[]>(
    // clusterId가 null이면 요청하지 않음
    clusterId ? getConsumerGroupsKey(clusterId) : null,
    () => (clusterId ? fetchConsumerGroups(clusterId) : Promise.reject(new Error('No cluster ID'))),
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
    groups: data || [],
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 단일 컨슈머 그룹 상세 정보를 가져오는 훅
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @param groupId - 그룹 ID (null이면 요청하지 않음)
 * @returns 컨슈머 그룹 상세 정보, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function ConsumerGroupDetail({ clusterId, groupId }: Props) {
 *   const { group, isLoading, error } = useConsumerGroup(clusterId, groupId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} />;
 *   if (!group) return <NotFound />;
 *
 *   return <GroupInfo group={group} />;
 * }
 * ```
 */
export function useConsumerGroup(
  clusterId: string | null,
  groupId: string | null
): UseConsumerGroupReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<ConsumerGroup>(
    // clusterId와 groupId가 모두 있을 때만 요청
    clusterId && groupId ? getConsumerGroupKey(clusterId, groupId) : null,
    () =>
      clusterId && groupId
        ? fetchConsumerGroup(clusterId, groupId)
        : Promise.reject(new Error('Missing cluster ID or group ID')),
    {
      // 15초마다 자동 재검증 (상세 정보는 더 자주 갱신)
      refreshInterval: 15 * 1000,
      revalidateOnFocus: true,
      errorRetryCount: 3,
    }
  );

  const refresh = async () => {
    await swrMutate();
  };

  return {
    group: data || null,
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 컨슈머 그룹 목록 캐시 무효화 함수
 *
 * @param clusterId - 클러스터 ID
 *
 * @example
 * ```tsx
 * // 컨슈머 그룹 삭제 후 목록 갱신
 * async function deleteGroup(clusterId: string, groupId: string) {
 *   await consumerGroupApi.delete(clusterId, groupId);
 *   await invalidateConsumerGroups(clusterId);
 * }
 * ```
 */
export async function invalidateConsumerGroups(clusterId: string): Promise<void> {
  await mutate(getConsumerGroupsKey(clusterId));
}

/**
 * 특정 컨슈머 그룹 캐시 무효화 함수
 *
 * @param clusterId - 클러스터 ID
 * @param groupId - 그룹 ID
 */
export async function invalidateConsumerGroup(
  clusterId: string,
  groupId: string
): Promise<void> {
  await mutate(getConsumerGroupKey(clusterId, groupId));
}

export default useConsumerGroups;
