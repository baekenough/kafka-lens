/**
 * 클러스터 관련 커스텀 훅
 *
 * SWR을 사용한 데이터 페칭 및 캐싱 관리
 * @module hooks/useClusters
 */
import useSWR, { mutate } from 'swr';
import { clusterApi } from '@/lib/api';
import type { Cluster, ApiResponse } from '@/types';

/**
 * 연결 테스트 결과 타입
 */
export interface ConnectionTestResult {
  /** 연결 성공 여부 */
  connected: boolean;
  /** 응답 시간 (ms) */
  latency?: number;
  /** 에러 메시지 (실패 시) */
  error?: string;
}

/**
 * useClusters 훅 반환 타입
 */
export interface UseClustersReturn {
  /** 클러스터 목록 */
  clusters: Cluster[];
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
 * useCluster 훅 반환 타입
 */
export interface UseClusterReturn {
  /** 클러스터 상세 정보 */
  cluster: Cluster | null;
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
 * 클러스터 목록 SWR 키
 */
export const CLUSTERS_KEY = '/api/clusters';

/**
 * 클러스터 상세 SWR 키 생성
 *
 * @param id - 클러스터 ID
 * @returns SWR 키
 */
export const getClusterKey = (id: string) => `/api/clusters/${id}`;

/**
 * 클러스터 목록 데이터 페처
 *
 * @returns 클러스터 목록
 * @throws API 요청 실패 시 에러
 */
async function fetchClusters(): Promise<Cluster[]> {
  const response = await clusterApi.list();

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch clusters');
  }

  return response.data;
}

/**
 * 클러스터 상세 데이터 페처
 *
 * @param id - 클러스터 ID
 * @returns 클러스터 상세 정보
 * @throws API 요청 실패 시 에러
 */
async function fetchCluster(id: string): Promise<Cluster> {
  const response = await clusterApi.get(id);

  if (!response.success || !response.data) {
    throw new Error(response.error?.message || 'Failed to fetch cluster');
  }

  return response.data;
}

/**
 * 클러스터 목록을 가져오는 훅
 *
 * SWR을 사용하여 클러스터 목록을 가져오고 캐싱합니다.
 * 자동 재검증 및 포커스 시 리페칭을 지원합니다.
 *
 * @returns 클러스터 목록, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function ClusterList() {
 *   const { clusters, isLoading, error, refresh } = useClusters();
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} onRetry={refresh} />;
 *
 *   return (
 *     <ul>
 *       {clusters.map(cluster => (
 *         <li key={cluster.id}>{cluster.name}</li>
 *       ))}
 *     </ul>
 *   );
 * }
 * ```
 */
export function useClusters(): UseClustersReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<Cluster[]>(
    CLUSTERS_KEY,
    fetchClusters,
    {
      // 5분마다 자동 재검증
      refreshInterval: 5 * 60 * 1000,
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
    clusters: data || [],
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 단일 클러스터 상세 정보를 가져오는 훅
 *
 * @param id - 클러스터 ID (null이면 요청하지 않음)
 * @returns 클러스터 상세 정보, 로딩 상태, 에러, 갱신 함수
 *
 * @example
 * ```tsx
 * function ClusterDetail({ id }: { id: string }) {
 *   const { cluster, isLoading, error } = useCluster(id);
 *
 *   if (isLoading) return <Loading />;
 *   if (error) return <Error message={error.message} />;
 *   if (!cluster) return <NotFound />;
 *
 *   return <ClusterInfo cluster={cluster} />;
 * }
 * ```
 */
export function useCluster(id: string | null): UseClusterReturn {
  const { data, error, isLoading, isValidating, mutate: swrMutate } = useSWR<Cluster>(
    // id가 null이면 요청하지 않음
    id ? getClusterKey(id) : null,
    () => (id ? fetchCluster(id) : Promise.reject(new Error('No cluster ID'))),
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
    cluster: data || null,
    isLoading,
    error: error || null,
    refresh,
    isValidating,
  };
}

/**
 * 클러스터 연결 테스트 함수
 *
 * 지정된 클러스터에 연결 테스트를 수행합니다.
 * 테스트 완료 후 클러스터 목록 캐시를 갱신합니다.
 *
 * @param id - 테스트할 클러스터 ID
 * @returns 연결 테스트 결과
 * @throws API 요청 실패 시 에러
 *
 * @example
 * ```tsx
 * async function handleTestConnection(clusterId: string) {
 *   try {
 *     const result = await testConnection(clusterId);
 *     if (result.connected) {
 *       toast.success('연결 성공');
 *     } else {
 *       toast.error('연결 실패');
 *     }
 *   } catch (error) {
 *     toast.error('테스트 중 오류 발생');
 *   }
 * }
 * ```
 */
export async function testConnection(id: string): Promise<ConnectionTestResult> {
  const startTime = Date.now();

  try {
    const response = await clusterApi.testConnection(id);
    const latency = Date.now() - startTime;

    if (!response.success) {
      return {
        connected: false,
        latency,
        error: response.error?.message || 'Connection test failed',
      };
    }

    const result: ConnectionTestResult = {
      connected: response.data?.connected || false,
      latency,
    };

    // 테스트 후 클러스터 캐시 갱신
    await mutate(CLUSTERS_KEY);
    await mutate(getClusterKey(id));

    return result;
  } catch (error) {
    const latency = Date.now() - startTime;
    return {
      connected: false,
      latency,
      error: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

/**
 * 클러스터 캐시 무효화 함수
 *
 * 클러스터 생성, 수정, 삭제 후 호출하여 캐시를 갱신합니다.
 *
 * @example
 * ```tsx
 * async function createCluster(data: ClusterCreateRequest) {
 *   await clusterApi.create(data);
 *   await invalidateClusters();
 * }
 * ```
 */
export async function invalidateClusters(): Promise<void> {
  await mutate(CLUSTERS_KEY);
}

/**
 * 특정 클러스터 캐시 무효화 함수
 *
 * @param id - 클러스터 ID
 *
 * @example
 * ```tsx
 * async function updateCluster(id: string, data: ClusterUpdateRequest) {
 *   await clusterApi.update(id, data);
 *   await invalidateCluster(id);
 * }
 * ```
 */
export async function invalidateCluster(id: string): Promise<void> {
  await mutate(getClusterKey(id));
}

export default useClusters;
