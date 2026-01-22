/**
 * ClusterList 컴포넌트 테스트
 *
 * T023: 클러스터 목록 컴포넌트 TDD 테스트
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ClusterList } from '@/components/cluster/ClusterList';
import { SWRConfig } from 'swr';
import type { Cluster } from '@/types';

/**
 * 테스트용 목 클러스터 데이터
 */
const mockClusters: Cluster[] = [
  {
    id: 'cluster-1',
    name: 'Local Development',
    bootstrapServers: ['localhost:9092'],
    status: 'connected',
    brokers: [
      { id: 1, host: 'localhost', port: 9092, status: 'online', isController: true },
    ],
    createdAt: '2025-01-01T00:00:00Z',
    lastConnectedAt: '2025-01-22T10:00:00Z',
  },
  {
    id: 'cluster-2',
    name: 'Staging Cluster',
    bootstrapServers: ['staging.kafka.example.com:9092'],
    status: 'disconnected',
    brokers: [],
    createdAt: '2025-01-02T00:00:00Z',
  },
  {
    id: 'cluster-3',
    name: 'Production Cluster',
    bootstrapServers: ['prod-1.kafka.example.com:9092', 'prod-2.kafka.example.com:9092'],
    status: 'connecting',
    brokers: [],
    createdAt: '2025-01-03T00:00:00Z',
  },
];

/**
 * SWR 캐시 초기화를 위한 래퍼 컴포넌트
 */
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <SWRConfig value={{ provider: () => new Map(), dedupingInterval: 0 }}>
      {children}
    </SWRConfig>
  );
}

/**
 * fetch 모킹 헬퍼
 */
function mockFetch(data: unknown, ok = true, status = 200) {
  return jest.fn().mockResolvedValue({
    ok,
    status,
    json: async () => data,
  });
}

describe('ClusterList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('렌더링', () => {
    it('클러스터 목록을 올바르게 렌더링한다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: mockClusters,
        error: null,
      });

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      // 로딩 상태 확인
      expect(screen.getByTestId('cluster-list-loading')).toBeInTheDocument();

      // 데이터 로드 후 클러스터 카드 확인
      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });

      expect(screen.getByText('Staging Cluster')).toBeInTheDocument();
      expect(screen.getByText('Production Cluster')).toBeInTheDocument();
    });

    it('각 클러스터 카드에 올바른 정보가 표시된다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: mockClusters,
        error: null,
      });

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });

      // Bootstrap servers 표시 확인
      expect(screen.getByText('localhost:9092')).toBeInTheDocument();

      // 상태 배지 확인
      expect(screen.getByText('connected')).toBeInTheDocument();
      expect(screen.getByText('disconnected')).toBeInTheDocument();
      expect(screen.getByText('connecting')).toBeInTheDocument();
    });
  });

  describe('로딩 상태', () => {
    it('데이터 로딩 중 스켈레톤을 표시한다', async () => {
      // 응답을 지연시켜 로딩 상태 확인
      global.fetch = jest.fn().mockImplementation(
        () =>
          new Promise((resolve) =>
            setTimeout(
              () =>
                resolve({
                  ok: true,
                  status: 200,
                  json: async () => ({
                    success: true,
                    data: mockClusters,
                    error: null,
                  }),
                }),
              100
            )
          )
      );

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      // 로딩 스켈레톤 확인
      expect(screen.getByTestId('cluster-list-loading')).toBeInTheDocument();
      expect(screen.getAllByTestId('cluster-skeleton')).toHaveLength(3);
    });
  });

  describe('에러 상태', () => {
    it('API 에러 시 에러 메시지를 표시한다', async () => {
      global.fetch = mockFetch(
        {
          success: false,
          data: null,
          error: { code: 'NETWORK_ERROR', message: 'Failed to fetch clusters' },
        },
        false,
        500
      );

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByTestId('cluster-list-error')).toBeInTheDocument();
      });

      expect(screen.getByText(/Failed to fetch clusters/i)).toBeInTheDocument();
    });

    it('에러 상태에서 재시도 버튼이 동작한다', async () => {
      // 첫 번째 호출: 에러, 두 번째 호출: 성공
      global.fetch = jest
        .fn()
        .mockResolvedValueOnce({
          ok: false,
          status: 500,
          json: async () => ({
            success: false,
            data: null,
            error: { code: 'NETWORK_ERROR', message: 'Failed to fetch' },
          }),
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => ({
            success: true,
            data: mockClusters,
            error: null,
          }),
        });

      const user = userEvent.setup();

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      // 에러 상태 확인
      await waitFor(() => {
        expect(screen.getByTestId('cluster-list-error')).toBeInTheDocument();
      });

      // 재시도 버튼 클릭
      const retryButton = screen.getByRole('button', { name: /retry/i });
      await user.click(retryButton);

      // 성공적으로 데이터 로드 확인
      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });
    });
  });

  describe('빈 상태', () => {
    it('클러스터가 없을 때 빈 상태 메시지를 표시한다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: [],
        error: null,
      });

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByTestId('cluster-list-empty')).toBeInTheDocument();
      });

      expect(screen.getByText(/no clusters/i)).toBeInTheDocument();
    });

    it('빈 상태에서 클러스터 추가 버튼이 표시된다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: [],
        error: null,
      });

      const handleAddCluster = jest.fn();

      render(
        <TestWrapper>
          <ClusterList onAddCluster={handleAddCluster} />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add cluster/i })).toBeInTheDocument();
      });
    });
  });

  describe('클러스터 선택', () => {
    it('클러스터 카드 클릭 시 onSelect 콜백이 호출된다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: mockClusters,
        error: null,
      });

      const handleSelect = jest.fn();
      const user = userEvent.setup();

      render(
        <TestWrapper>
          <ClusterList onSelect={handleSelect} />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });

      // 첫 번째 클러스터 클릭
      const clusterCard = screen.getByTestId('cluster-card-cluster-1');
      await user.click(clusterCard);

      expect(handleSelect).toHaveBeenCalledWith(mockClusters[0]);
    });

    it('현재 선택된 클러스터가 하이라이트된다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: mockClusters,
        error: null,
      });

      render(
        <TestWrapper>
          <ClusterList selectedClusterId="cluster-1" />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });

      const selectedCard = screen.getByTestId('cluster-card-cluster-1');
      expect(selectedCard).toHaveAttribute('data-selected', 'true');
    });
  });

  describe('연결 테스트', () => {
    it('연결 테스트 버튼 클릭 시 테스트가 실행된다', async () => {
      global.fetch = mockFetch({
        success: true,
        data: mockClusters,
        error: null,
      });

      const user = userEvent.setup();

      render(
        <TestWrapper>
          <ClusterList />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Local Development')).toBeInTheDocument();
      });

      // 테스트 연결 버튼 클릭
      const testButtons = screen.getAllByRole('button', { name: /test connection/i });

      // 연결 테스트 API 호출 모킹
      global.fetch = mockFetch({
        success: true,
        data: { connected: true },
        error: null,
      });

      await user.click(testButtons[0]);

      // API 호출 확인
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining('/clusters/cluster-1/test'),
          expect.any(Object)
        );
      });
    });
  });
});
