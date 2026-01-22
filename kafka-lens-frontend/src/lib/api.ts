import type {
  ApiResponse,
  ApiError,
  Cluster,
  ClusterCreateRequest,
  Topic,
  TopicCreateRequest,
  TopicListFilter,
  Broker,
  ConsumerGroup,
  ConsumerGroupLagSummary,
  Message,
  MessageSearchRequest,
  MessagePublishRequest,
  PaginationParams,
} from '@/types';

// ============================================================================
// API 설정
// ============================================================================

/**
 * API 기본 URL
 * 환경 변수로 설정 가능
 */
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

/**
 * 기본 요청 타임아웃 (ms)
 */
const DEFAULT_TIMEOUT = 30000;

// ============================================================================
// 인증 관리
// ============================================================================

/**
 * 인증 자격 증명 타입
 */
interface Credentials {
  /** 사용자명 */
  username: string;
  /** 비밀번호 */
  password: string;
}

/** 현재 저장된 자격 증명 */
let currentCredentials: Credentials | null = null;

/**
 * 인증 자격 증명 설정
 *
 * Basic Auth에 사용할 자격 증명을 설정합니다.
 *
 * @param credentials - 사용자명과 비밀번호
 *
 * @example
 * ```ts
 * setCredentials({ username: 'admin', password: 'secret' });
 * ```
 */
export function setCredentials(credentials: Credentials | null): void {
  currentCredentials = credentials;
}

/**
 * 현재 인증 상태 확인
 *
 * @returns 인증 자격 증명이 설정되어 있는지 여부
 */
export function isAuthenticated(): boolean {
  return currentCredentials !== null;
}

/**
 * Basic Auth 헤더 생성
 *
 * @returns Authorization 헤더 값 또는 null
 */
function getAuthHeader(): string | null {
  if (!currentCredentials) return null;

  const { username, password } = currentCredentials;
  const encoded = btoa(`${username}:${password}`);
  return `Basic ${encoded}`;
}

// ============================================================================
// HTTP 클라이언트
// ============================================================================

/**
 * API 요청 옵션
 */
interface RequestOptions {
  /** HTTP 메서드 */
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  /** 요청 본문 */
  body?: unknown;
  /** 추가 헤더 */
  headers?: Record<string, string>;
  /** 요청 타임아웃 (ms) */
  timeout?: number;
  /** 쿼리 파라미터 */
  params?: Record<string, string | number | boolean | undefined>;
}

/**
 * API 요청 에러 클래스
 *
 * API 요청 실패 시 발생하는 커스텀 에러입니다.
 */
export class ApiRequestError extends Error {
  /** HTTP 상태 코드 */
  public readonly status: number;
  /** 에러 코드 */
  public readonly code: string;
  /** 상세 정보 */
  public readonly details?: Record<string, unknown>;

  constructor(message: string, status: number, code: string, details?: Record<string, unknown>) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

/**
 * 쿼리 스트링 생성
 *
 * @param params - 쿼리 파라미터 객체
 * @returns URL 쿼리 스트링
 */
function buildQueryString(
  params?: Record<string, string | number | boolean | undefined>
): string {
  if (!params) return '';

  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) {
      searchParams.append(key, String(value));
    }
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

/**
 * 기본 fetch 래퍼
 *
 * 모든 API 요청에 공통 로직을 적용합니다.
 * - 인증 헤더 자동 추가
 * - 타임아웃 처리
 * - 에러 핸들링
 * - JSON 파싱
 *
 * @template T - 응답 데이터 타입
 * @param endpoint - API 엔드포인트 (기본 URL 이후 경로)
 * @param options - 요청 옵션
 * @returns API 응답
 * @throws ApiRequestError - 요청 실패 시
 *
 * @example
 * ```ts
 * const response = await apiRequest<Topic[]>('/topics');
 * ```
 */
async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<ApiResponse<T>> {
  const {
    method = 'GET',
    body,
    headers = {},
    timeout = DEFAULT_TIMEOUT,
    params,
  } = options;

  // URL 구성
  const url = `${API_BASE_URL}${endpoint}${buildQueryString(params)}`;

  // 헤더 구성
  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers,
  };

  // 인증 헤더 추가
  const authHeader = getAuthHeader();
  if (authHeader) {
    requestHeaders['Authorization'] = authHeader;
  }

  // AbortController로 타임아웃 처리
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      method,
      headers: requestHeaders,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    // JSON 응답 파싱
    const data = await response.json();

    // 에러 응답 처리
    if (!response.ok) {
      const error: ApiError = data.error || {
        code: `HTTP_${response.status}`,
        message: response.statusText,
      };

      throw new ApiRequestError(
        error.message,
        response.status,
        error.code,
        error.details
      );
    }

    // 성공 응답 반환
    return data as ApiResponse<T>;
  } catch (error) {
    clearTimeout(timeoutId);

    // AbortError (타임아웃)
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiRequestError(
        'Request timeout',
        408,
        'REQUEST_TIMEOUT'
      );
    }

    // ApiRequestError는 그대로 전파
    if (error instanceof ApiRequestError) {
      throw error;
    }

    // 네트워크 에러 등
    if (error instanceof Error) {
      throw new ApiRequestError(
        error.message || 'Network error',
        0,
        'NETWORK_ERROR'
      );
    }

    // 알 수 없는 에러
    throw new ApiRequestError(
      'Unknown error occurred',
      0,
      'UNKNOWN_ERROR'
    );
  }
}

// ============================================================================
// Cluster API
// ============================================================================

/**
 * 클러스터 API
 *
 * Kafka 클러스터 연결 관리 API입니다.
 */
export const clusterApi = {
  /**
   * 모든 클러스터 목록 조회
   *
   * @returns 클러스터 목록
   */
  async list(): Promise<ApiResponse<Cluster[]>> {
    return apiRequest<Cluster[]>('/clusters');
  },

  /**
   * 특정 클러스터 상세 조회
   *
   * @param id - 클러스터 ID
   * @returns 클러스터 상세 정보
   */
  async get(id: string): Promise<ApiResponse<Cluster>> {
    return apiRequest<Cluster>(`/clusters/${id}`);
  },

  /**
   * 새 클러스터 등록
   *
   * @param data - 클러스터 생성 요청
   * @returns 생성된 클러스터 정보
   */
  async create(data: ClusterCreateRequest): Promise<ApiResponse<Cluster>> {
    return apiRequest<Cluster>('/clusters', {
      method: 'POST',
      body: data,
    });
  },

  /**
   * 클러스터 삭제
   *
   * @param id - 삭제할 클러스터 ID
   * @returns 삭제 결과
   */
  async delete(id: string): Promise<ApiResponse<void>> {
    return apiRequest<void>(`/clusters/${id}`, {
      method: 'DELETE',
    });
  },

  /**
   * 클러스터 연결 테스트
   *
   * @param id - 테스트할 클러스터 ID
   * @returns 연결 테스트 결과
   */
  async testConnection(id: string): Promise<ApiResponse<{ connected: boolean }>> {
    return apiRequest<{ connected: boolean }>(`/clusters/${id}/test`);
  },
};

// ============================================================================
// Topic API
// ============================================================================

/**
 * 토픽 API
 *
 * Kafka 토픽 관리 API입니다.
 */
export const topicApi = {
  /**
   * 토픽 목록 조회
   *
   * @param clusterId - 클러스터 ID
   * @param filter - 필터 옵션
   * @param pagination - 페이지네이션 옵션
   * @returns 토픽 목록
   */
  async list(
    clusterId: string,
    filter?: TopicListFilter,
    pagination?: PaginationParams
  ): Promise<ApiResponse<Topic[]>> {
    return apiRequest<Topic[]>(`/clusters/${clusterId}/topics`, {
      params: {
        search: filter?.search,
        includeInternal: filter?.includeInternal,
        page: pagination?.page,
        pageSize: pagination?.pageSize,
        sortBy: pagination?.sortBy,
        sortOrder: pagination?.sortOrder,
      },
    });
  },

  /**
   * 특정 토픽 상세 조회
   *
   * @param clusterId - 클러스터 ID
   * @param topicName - 토픽 이름
   * @returns 토픽 상세 정보
   */
  async get(clusterId: string, topicName: string): Promise<ApiResponse<Topic>> {
    return apiRequest<Topic>(`/clusters/${clusterId}/topics/${topicName}`);
  },

  /**
   * 새 토픽 생성
   *
   * @param clusterId - 클러스터 ID
   * @param data - 토픽 생성 요청
   * @returns 생성된 토픽 정보
   */
  async create(
    clusterId: string,
    data: TopicCreateRequest
  ): Promise<ApiResponse<Topic>> {
    return apiRequest<Topic>(`/clusters/${clusterId}/topics`, {
      method: 'POST',
      body: data,
    });
  },

  /**
   * 토픽 삭제
   *
   * @param clusterId - 클러스터 ID
   * @param topicName - 삭제할 토픽 이름
   * @returns 삭제 결과
   */
  async delete(clusterId: string, topicName: string): Promise<ApiResponse<void>> {
    return apiRequest<void>(`/clusters/${clusterId}/topics/${topicName}`, {
      method: 'DELETE',
    });
  },

  /**
   * 토픽 설정 조회
   *
   * @param clusterId - 클러스터 ID
   * @param topicName - 토픽 이름
   * @returns 토픽 설정
   */
  async getConfigs(
    clusterId: string,
    topicName: string
  ): Promise<ApiResponse<Record<string, string>>> {
    return apiRequest<Record<string, string>>(
      `/clusters/${clusterId}/topics/${topicName}/configs`
    );
  },

  /**
   * 토픽 설정 업데이트
   *
   * @param clusterId - 클러스터 ID
   * @param topicName - 토픽 이름
   * @param configs - 업데이트할 설정
   * @returns 업데이트 결과
   */
  async updateConfigs(
    clusterId: string,
    topicName: string,
    configs: Record<string, string>
  ): Promise<ApiResponse<void>> {
    return apiRequest<void>(
      `/clusters/${clusterId}/topics/${topicName}/configs`,
      {
        method: 'PATCH',
        body: configs,
      }
    );
  },
};

// ============================================================================
// Broker API
// ============================================================================

/**
 * 브로커 API
 *
 * Kafka 브로커 조회 API입니다.
 */
export const brokerApi = {
  /**
   * 브로커 목록 조회
   *
   * @param clusterId - 클러스터 ID
   * @returns 브로커 목록
   */
  async list(clusterId: string): Promise<ApiResponse<Broker[]>> {
    return apiRequest<Broker[]>(`/clusters/${clusterId}/brokers`);
  },

  /**
   * 특정 브로커 상세 조회
   *
   * @param clusterId - 클러스터 ID
   * @param brokerId - 브로커 ID
   * @returns 브로커 상세 정보
   */
  async get(clusterId: string, brokerId: number): Promise<ApiResponse<Broker>> {
    return apiRequest<Broker>(`/clusters/${clusterId}/brokers/${brokerId}`);
  },

  /**
   * 브로커 설정 조회
   *
   * @param clusterId - 클러스터 ID
   * @param brokerId - 브로커 ID
   * @returns 브로커 설정
   */
  async getConfigs(
    clusterId: string,
    brokerId: number
  ): Promise<ApiResponse<Record<string, string>>> {
    return apiRequest<Record<string, string>>(
      `/clusters/${clusterId}/brokers/${brokerId}/configs`
    );
  },
};

// ============================================================================
// Consumer Group API
// ============================================================================

/**
 * 컨슈머 그룹 API
 *
 * Kafka 컨슈머 그룹 관리 API입니다.
 */
export const consumerGroupApi = {
  /**
   * 컨슈머 그룹 목록 조회
   *
   * @param clusterId - 클러스터 ID
   * @param pagination - 페이지네이션 옵션
   * @returns 컨슈머 그룹 목록
   */
  async list(
    clusterId: string,
    pagination?: PaginationParams
  ): Promise<ApiResponse<ConsumerGroup[]>> {
    return apiRequest<ConsumerGroup[]>(`/clusters/${clusterId}/consumer-groups`, {
      params: pagination
        ? {
            page: pagination.page,
            pageSize: pagination.pageSize,
            sortBy: pagination.sortBy,
            sortOrder: pagination.sortOrder,
          }
        : undefined,
    });
  },

  /**
   * 특정 컨슈머 그룹 상세 조회
   *
   * @param clusterId - 클러스터 ID
   * @param groupId - 컨슈머 그룹 ID
   * @returns 컨슈머 그룹 상세 정보
   */
  async get(
    clusterId: string,
    groupId: string
  ): Promise<ApiResponse<ConsumerGroup>> {
    return apiRequest<ConsumerGroup>(
      `/clusters/${clusterId}/consumer-groups/${groupId}`
    );
  },

  /**
   * 컨슈머 그룹 Lag 조회
   *
   * @param clusterId - 클러스터 ID
   * @param groupId - 컨슈머 그룹 ID
   * @returns Lag 상세 정보
   */
  async getLag(
    clusterId: string,
    groupId: string
  ): Promise<ApiResponse<ConsumerGroupLagSummary>> {
    return apiRequest<ConsumerGroupLagSummary>(
      `/clusters/${clusterId}/consumer-groups/${groupId}/lag`
    );
  },

  /**
   * 컨슈머 그룹 오프셋 리셋
   *
   * @param clusterId - 클러스터 ID
   * @param groupId - 컨슈머 그룹 ID
   * @param topic - 토픽 이름
   * @param offset - 리셋할 오프셋 ('earliest' | 'latest' | number)
   * @returns 리셋 결과
   */
  async resetOffset(
    clusterId: string,
    groupId: string,
    topic: string,
    offset: 'earliest' | 'latest' | number
  ): Promise<ApiResponse<void>> {
    return apiRequest<void>(
      `/clusters/${clusterId}/consumer-groups/${groupId}/reset-offsets`,
      {
        method: 'POST',
        body: { topic, offset },
      }
    );
  },

  /**
   * 컨슈머 그룹 삭제
   *
   * @param clusterId - 클러스터 ID
   * @param groupId - 삭제할 컨슈머 그룹 ID
   * @returns 삭제 결과
   */
  async delete(clusterId: string, groupId: string): Promise<ApiResponse<void>> {
    return apiRequest<void>(
      `/clusters/${clusterId}/consumer-groups/${groupId}`,
      {
        method: 'DELETE',
      }
    );
  },
};

// ============================================================================
// Message API
// ============================================================================

/**
 * 메시지 API
 *
 * Kafka 메시지 조회 및 발행 API입니다.
 */
export const messageApi = {
  /**
   * 메시지 검색
   *
   * @param clusterId - 클러스터 ID
   * @param request - 검색 요청
   * @returns 메시지 목록
   */
  async search(
    clusterId: string,
    request: MessageSearchRequest
  ): Promise<ApiResponse<Message[]>> {
    return apiRequest<Message[]>(`/clusters/${clusterId}/messages/search`, {
      method: 'POST',
      body: request,
    });
  },

  /**
   * 메시지 발행
   *
   * @param clusterId - 클러스터 ID
   * @param request - 발행 요청
   * @returns 발행 결과 (오프셋 등)
   */
  async publish(
    clusterId: string,
    request: MessagePublishRequest
  ): Promise<ApiResponse<{ partition: number; offset: number }>> {
    return apiRequest<{ partition: number; offset: number }>(
      `/clusters/${clusterId}/messages`,
      {
        method: 'POST',
        body: request,
      }
    );
  },

  /**
   * 특정 오프셋의 메시지 조회
   *
   * @param clusterId - 클러스터 ID
   * @param topic - 토픽 이름
   * @param partition - 파티션 ID
   * @param offset - 오프셋
   * @returns 메시지
   */
  async getByOffset(
    clusterId: string,
    topic: string,
    partition: number,
    offset: number
  ): Promise<ApiResponse<Message>> {
    return apiRequest<Message>(
      `/clusters/${clusterId}/topics/${topic}/partitions/${partition}/messages/${offset}`
    );
  },
};

// ============================================================================
// Health API
// ============================================================================

/**
 * 헬스 체크 API
 *
 * 서버 상태 확인용 API입니다.
 */
export const healthApi = {
  /**
   * 서버 상태 확인
   *
   * @returns 서버 상태
   */
  async check(): Promise<ApiResponse<{ status: string; version: string }>> {
    return apiRequest<{ status: string; version: string }>('/health');
  },
};

// ============================================================================
// Export 통합
// ============================================================================

/**
 * API 클라이언트 통합 객체
 *
 * 모든 API를 하나의 객체로 접근할 수 있습니다.
 *
 * @example
 * ```ts
 * import { api } from '@/lib/api';
 *
 * const topics = await api.topic.list('cluster-1');
 * const brokers = await api.broker.list('cluster-1');
 * ```
 */
export const api = {
  cluster: clusterApi,
  topic: topicApi,
  broker: brokerApi,
  consumerGroup: consumerGroupApi,
  message: messageApi,
  health: healthApi,
};

export default api;
