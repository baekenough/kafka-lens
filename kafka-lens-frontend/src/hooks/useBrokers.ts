/**
 * 브로커 관련 커스텀 훅
 *
 * SWR을 사용한 브로커 데이터 페칭 및 캐싱 관리
 * @module hooks/useBrokers
 */
import useSWR, { mutate } from 'swr';
import { brokerApi } from '@/lib/api';
import type { Broker, ApiResponse } from '@/types';

/**
 * useBrokers 훅 반환 타입
 */
export interface UseBrokersReturn {
  /** 브로커 목록 */
  brokers: Broker[];
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
 * useBroker 훅 반환 타입
 */
export interface UseBrokerReturn {
  /** 브로커 상세 정보 */
  broker: Broker | null;
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
 * 브로커 목록 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @returns SWR 키
 */
export const getBrokersKey = (clusterId: string) =>
  `/api/clusters/${clusterId}/brokers`;

/**
 * 브로커 상세 SWR 키 생성
 *
 * @param clusterId - 클러스터 ID
 * @param brokerId - 브로커 ID
 * @returns SWR 키
 */
export const getBrokerKey = (clusterId: string, brokerId: number) =>
  `/api/clusters/${clusterId}/brokers/${brokerId}`;

/**
 * 브로커 목록 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @returns 브로커 목록
 * @throws API 요청 실패 시 에러
 */
async function fetchBrokers(clusterId: string): Promise<Broker[]> {
  const response = await brokerApi.list(clusterId);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch brokers');
  }

  return response.data;
}

/**
 * 브로커 상세 데이터 페처
 *
 * @param clusterId - 클러스터 ID
 * @param brokerId - 브로커 ID
 * @returns 브로커 상세 정보
 * @throws API 요청 실패 시 에러
 */
async function fetchBroker(clusterId: string, brokerId: number): Promise<Broker> {
  const response = await brokerApi.get(clusterId, brokerId);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch broker');
  }

  return response.data;
}

/**
 * 클러스터의 브로커 목록을 가져오는 훅
 *
 * SWR을 사용하여 브로커 목록을 가져오고 캐싱합니다.
 * 자동 재검증 및 포커스 시 리페칭을 지원합니다.
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @returns 브로커 목록, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function BrokerList({ clusterId }: { clusterId: string }) {
 *   const { brokers, isLoading, error, refresh } = useBrokers(clusterId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} onRetry={refresh} />;
 *
 *   return (
 *     <ul>
 *       {brokers.map(broker => (
 *         <li key={broker.id}>{broker.host}:{broker.port}</li>
 *       ))}
 *     </ul>
 *   );
 * }
 * ```
 */
export function useBrokers(clusterId: string | null): UseBrokersReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<Broker[]>(
    // clusterId가 null이면 요청하지 않음
    clusterId ? getBrokersKey(clusterId) : null,
    () => (clusterId ? fetchBrokers(clusterId) : Promise.reject(new Error('No cluster ID'))),
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
    brokers: data || [],
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 단일 브로커 상세 정보를 가져오는 훅
 *
 * @param clusterId - 클러스터 ID (null이면 요청하지 않음)
 * @param brokerId - 브로커 ID (null이면 요청하지 않음)
 * @returns 브로커 상세 정보, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function BrokerDetail({ clusterId, brokerId }: Props) {
 *   const { broker, isLoading, error } = useBroker(clusterId, brokerId);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} />;
 *   if (!broker) return <NotFound />;
 *
 *   return <BrokerInfo broker={broker} />;
 * }
 * ```
 */
export function useBroker(
  clusterId: string | null,
  brokerId: number | null
): UseBrokerReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<Broker>(
    // clusterId와 brokerId가 모두 있을 때만 요청
    clusterId && brokerId !== null ? getBrokerKey(clusterId, brokerId) : null,
    () =>
      clusterId && brokerId !== null
        ? fetchBroker(clusterId, brokerId)
        : Promise.reject(new Error('Missing cluster ID or broker ID')),
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
    broker: data || null,
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 브로커 목록 캐시 무효화 함수
 *
 * @param clusterId - 클러스터 ID
 *
 * @example
 * ```tsx
 * // 브로커 상태 갱신 후 목록 갱신
 * async function refreshBrokers(clusterId: string) {
 *   await invalidateBrokers(clusterId);
 * }
 * ```
 */
export async function invalidateBrokers(clusterId: string): Promise<void> {
  await mutate(getBrokersKey(clusterId));
}

/**
 * 특정 브로커 캐시 무효화 함수
 *
 * @param clusterId - 클러스터 ID
 * @param brokerId - 브로커 ID
 */
export async function invalidateBroker(
  clusterId: string,
  brokerId: number
): Promise<void> {
  await mutate(getBrokerKey(clusterId, brokerId));
}

export default useBrokers;
