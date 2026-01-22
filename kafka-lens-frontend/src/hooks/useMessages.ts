/**
 * 메시지 관련 커스텀 훅
 *
 * 메시지 검색 및 발행 기능을 제공하는 훅
 * @module hooks/useMessages
 */
import { useState, useCallback } from 'react';
import { messageApi } from '@/lib/api';
import type { Message, MessageSearchRequest, MessagePublishRequest, ApiResponse } from '@/types';

/**
 * 메시지 검색 상태
 */
export interface MessageSearchState {
  /** 검색된 메시지 목록 */
  messages: Message[];
  /** 로딩 상태 */
  isLoading: boolean;
  /** 에러 정보 */
  error: Error | null;
  /** 마지막 검색 요청 */
  lastRequest: MessageSearchRequest | null;
  /** 검색 완료 여부 */
  hasSearched: boolean;
}

/**
 * 메시지 발행 상태
 */
export interface MessagePublishState {
  /** 발행 중 상태 */
  isPublishing: boolean;
  /** 에러 정보 */
  error: Error | null;
  /** 마지막 발행 결과 */
  lastResult: { partition: number; offset: number } | null;
}

/**
 * useMessages 훅 반환 타입
 */
export interface UseMessagesReturn {
  /** 메시지 검색 상태 */
  searchState: MessageSearchState;
  /** 메시지 발행 상태 */
  publishState: MessagePublishState;
  /** 메시지 검색 함수 */
  searchMessages: (request: MessageSearchRequest) => Promise<Message[]>;
  /** 메시지 발행 함수 */
  publishMessage: (request: MessagePublishRequest) => Promise<{ partition: number; offset: number }>;
  /** 검색 결과 초기화 함수 */
  clearMessages: () => void;
  /** 에러 초기화 함수 */
  clearErrors: () => void;
}

/**
 * 메시지 검색 및 발행 훅
 *
 * Kafka 토픽의 메시지를 검색하고 발행하는 기능을 제공합니다.
 * SWR을 사용하지 않고 명시적인 요청 방식을 사용합니다.
 *
 * @param clusterId - 클러스터 ID
 * @returns 메시지 검색/발행 상태 및 함수
 *
 * @example
 * ```tsx
 * function MessageViewer({ clusterId }: { clusterId: string }) {
 *   const {
 *     searchState,
 *     publishState,
 *     searchMessages,
 *     publishMessage,
 *     clearMessages,
 *   } = useMessages(clusterId);
 *
 *   const handleSearch = async () => {
 *     await searchMessages({
 *       topic: 'my-topic',
 *       limit: 100,
 *     });
 *   };
 *
 *   return (
 *     <div>
 *       {searchState.isLoading && <Loading />}
 *       {searchState.messages.map(msg => (
 *         <MessageItem key={`${msg.partition}-${msg.offset}`} message={msg} />
 *       ))}
 *     </div>
 *   );
 * }
 * ```
 */
export function useMessages(clusterId: string): UseMessagesReturn {
  /** 메시지 검색 상태 */
  const [searchState, setSearchState] = useState<MessageSearchState>({
    messages: [],
    isLoading: false,
    error: null,
    lastRequest: null,
    hasSearched: false,
  });

  /** 메시지 발행 상태 */
  const [publishState, setPublishState] = useState<MessagePublishState>({
    isPublishing: false,
    error: null,
    lastResult: null,
  });

  /**
   * 메시지 검색 함수
   *
   * 지정된 조건으로 메시지를 검색합니다.
   *
   * @param request - 메시지 검색 요청
   * @returns 검색된 메시지 목록
   * @throws API 요청 실패 시 에러
   */
  const searchMessages = useCallback(
    async (request: MessageSearchRequest): Promise<Message[]> => {
      setSearchState((prev) => ({
        ...prev,
        isLoading: true,
        error: null,
        lastRequest: request,
      }));

      try {
        const response = await messageApi.search(clusterId, request);

        if (!response.success || !response.data) {
          throw new Error(response.error?.message || 'Failed to search messages');
        }

        const messages = response.data;

        setSearchState((prev) => ({
          ...prev,
          messages,
          isLoading: false,
          hasSearched: true,
        }));

        return messages;
      } catch (error) {
        const errorObj = error instanceof Error ? error : new Error('Unknown error');

        setSearchState((prev) => ({
          ...prev,
          isLoading: false,
          error: errorObj,
          hasSearched: true,
        }));

        throw errorObj;
      }
    },
    [clusterId]
  );

  /**
   * 메시지 발행 함수
   *
   * 지정된 토픽에 메시지를 발행합니다.
   *
   * @param request - 메시지 발행 요청
   * @returns 발행된 메시지의 파티션과 오프셋
   * @throws API 요청 실패 시 에러
   */
  const publishMessage = useCallback(
    async (
      request: MessagePublishRequest
    ): Promise<{ partition: number; offset: number }> => {
      setPublishState((prev) => ({
        ...prev,
        isPublishing: true,
        error: null,
      }));

      try {
        const response = await messageApi.publish(clusterId, request);

        if (!response.success || !response.data) {
          throw new Error(response.error?.message || 'Failed to publish message');
        }

        const result = response.data;

        setPublishState((prev) => ({
          ...prev,
          isPublishing: false,
          lastResult: result,
        }));

        return result;
      } catch (error) {
        const errorObj = error instanceof Error ? error : new Error('Unknown error');

        setPublishState((prev) => ({
          ...prev,
          isPublishing: false,
          error: errorObj,
        }));

        throw errorObj;
      }
    },
    [clusterId]
  );

  /**
   * 검색 결과 초기화
   */
  const clearMessages = useCallback(() => {
    setSearchState({
      messages: [],
      isLoading: false,
      error: null,
      lastRequest: null,
      hasSearched: false,
    });
  }, []);

  /**
   * 에러 초기화
   */
  const clearErrors = useCallback(() => {
    setSearchState((prev) => ({ ...prev, error: null }));
    setPublishState((prev) => ({ ...prev, error: null }));
  }, []);

  return {
    searchState,
    publishState,
    searchMessages,
    publishMessage,
    clearMessages,
    clearErrors,
  };
}

/**
 * 단일 메시지 조회 훅
 *
 * 특정 오프셋의 메시지를 조회합니다.
 *
 * @param clusterId - 클러스터 ID
 * @param topic - 토픽 이름
 * @param partition - 파티션 번호
 * @param offset - 오프셋
 * @returns 메시지 조회 상태 및 함수
 */
export function useMessage(
  clusterId: string,
  topic: string | null,
  partition: number | null,
  offset: number | null
) {
  const [message, setMessage] = useState<Message | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  /**
   * 메시지 조회 함수
   */
  const fetchMessage = useCallback(async () => {
    if (!topic || partition === null || offset === null) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await messageApi.getByOffset(clusterId, topic, partition, offset);

      if (!response.success || !response.data) {
        throw new Error(response.error?.message || 'Failed to fetch message');
      }

      setMessage(response.data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Unknown error'));
    } finally {
      setIsLoading(false);
    }
  }, [clusterId, topic, partition, offset]);

  return {
    message,
    isLoading,
    error,
    fetchMessage,
    clearMessage: () => setMessage(null),
  };
}

export default useMessages;
