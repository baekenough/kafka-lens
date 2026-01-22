/**
 * Consumer Lag 관련 커스텀 훅
 *
 * SWR을 사용한 Lag 데이터 페칭 및 캐싱 관리
 * @module hooks/useLag
 */
import useSWR from 'swr';
import { consumerGroupApi } from '@/lib/api';
import type { ConsumerGroupLagSummary } from '@/types';

/**
 * useLag 훅 반환 타입
 */
export interface UseLagReturn {
  /** Lag 요약 정보 */
  lag: ConsumerGroupLagSummary | null;
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
 * Consumer Lag SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @param groupId - 컨슈머 그룹 ID
 * @returns SWR 키
 */
export const getLagKey = (clusterId: string, groupId: string) =>
  `/api/clusters/${clusterId}/consumer-groups/${groupId}/lag`;

/**
 * Consumer Lag 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @param groupId - 컨슈머 그룹 ID
 * @returns Lag 요약 정보
 * @throws API 요청 실패 시 에러
 */
async function fetchLag(clusterId: string, groupId: string): Promise<ConsumerGroupLagSummary> {
  const response = await consumerGroupApi.getLag(clusterId, groupId);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch consumer lag');
  }

  return response.data;
}

/**
 * Consumer Lag를 가져오는 훅
 *
 * SWR을 사용하여 Consumer Lag 정보를 가져오고 캐싱합니다.
 * 자동 새로고침을 지원하며, 5초마다 최신 Lag 데이터를 가져옵니다.
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @param groupId - 컨슈머 그룹 ID (null이면 요청하지 않음)
 * @returns Lag 요약, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function ConsumerLagView({ clusterId, groupId }: Props) {
 *   const { lag, isLoading, error, refresh } = useLag(clusterId, groupId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} onRetry={refresh} />;
 *   if (!lag) return <NotFound />;
 *
 *   return <LagTable lag={lag} />;
 * }
 * ```
 */
export function useLag(clusterId: string | null, groupId: string | null): UseLagReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<ConsumerGroupLagSummary>(
    // clusterId와 groupId가 모두 있을 때만 요청
    clusterId && groupId ? getLagKey(clusterId, groupId) : null,
    () => {
      if (!clusterId || !groupId) {
        return Promise.reject(new Error('Missing cluster ID or group ID'));
      }
      return fetchLag(clusterId, groupId);
    },
    {
      // 5초마다 자동 새로고침 (Lag 모니터링에 적합)
      refreshInterval: 5 * 1000,
      // 포커스 시 재검증
      revalidateOnFocus: true,
      // 마운트 시 재검증
      revalidateOnMount: true,
      // 에러 시 재시도 횟수
      errorRetryCount: 3,
      // 재시도 간격
      errorRetryInterval: 2000,
      // 오래된 데이터 유지 (갱신 중에도 이전 데이터 표시)
      keepPreviousData: true,
    }
  );

  /**
   * 데이터 수동 갱신
   */
  const refresh = async () => {
    await swrMutate();
  };

  return {
    lag: data || null,
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * Lag 임계값 상수
 */
export const LAG_THRESHOLDS = {
  /** Warning 임계값 */
  WARNING: 1000,
  /** Critical 임계값 */
  CRITICAL: 10000,
} as const;

/**
 * Lag 상태 반환 타입
 */
export type LagStatus = 'normal' | 'warning' | 'critical';

/**
 * Lag 값에 따른 상태를 반환합니다.
 *
 * @param lag - Lag 값
 * @returns 'normal', 'warning', 또는 'critical'
 */
export function getLagStatus(lag: number): LagStatus {
  if (lag >= LAG_THRESHOLDS.CRITICAL) {
    return 'critical';
  }
  if (lag >= LAG_THRESHOLDS.WARNING) {
    return 'warning';
  }
  return 'normal';
}

/**
 * Lag 값을 포맷팅합니다.
 *
 * @param lag - Lag 값
 * @returns 포맷팅된 문자열 (예: "1,234", "1.2K", "1.5M")
 */
export function formatLag(lag: number): string {
  if (lag >= 1000000) {
    return `${(lag / 1000000).toFixed(1)}M`;
  }
  if (lag >= 1000) {
    return `${(lag / 1000).toFixed(1)}K`;
  }
  return lag.toLocaleString();
}

export default useLag;
