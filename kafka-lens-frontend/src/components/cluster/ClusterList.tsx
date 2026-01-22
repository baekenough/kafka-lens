'use client';

/**
 * ClusterList 컴포넌트
 *
 * 클러스터 목록을 그리드 형태로 표시합니다.
 * - 로딩 스켈레톤
 * - 에러 상태 처리
 * - 빈 상태 처리
 * - 클러스터 선택 기능
 *
 * @module components/cluster/ClusterList
 */
import { useCallback } from 'react';
import { Plus, RefreshCw, AlertCircle, Server } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useClusters, type ConnectionTestResult } from '@/hooks/useClusters';
import { ClusterCard } from './ClusterCard';
import { cn } from '@/lib/utils';
import type { Cluster } from '@/types';

/**
 * ClusterList 컴포넌트 Props
 */
export interface ClusterListProps {
  /** 클러스터 선택 시 콜백 */
  onSelect?: (cluster: Cluster) => void;
  /** 클러스터 추가 버튼 클릭 콜백 */
  onAddCluster?: () => void;
  /** 현재 선택된 클러스터 ID */
  selectedClusterId?: string;
  /** 그리드 컬럼 수 (반응형 기본값 사용) */
  columns?: 1 | 2 | 3 | 4;
}

/**
 * 로딩 스켈레톤 컴포넌트
 *
 * 데이터 로딩 중 표시되는 플레이스홀더입니다.
 */
function ClusterSkeleton() {
  return (
    <Card data-testid="cluster-skeleton" className="animate-pulse">
      <div className="p-6">
        {/* 헤더 스켈레톤 */}
        <div className="flex items-start justify-between gap-2 mb-4">
          <div className="flex items-center gap-2">
            <div className="h-5 w-5 rounded bg-muted" />
            <div className="h-6 w-32 rounded bg-muted" />
          </div>
          <div className="h-6 w-20 rounded-full bg-muted" />
        </div>

        {/* 콘텐츠 스켈레톤 */}
        <div className="space-y-3">
          <div>
            <div className="h-3 w-24 rounded bg-muted mb-2" />
            <div className="h-4 w-full rounded bg-muted" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="h-4 w-20 rounded bg-muted" />
            <div className="h-4 w-16 rounded bg-muted" />
          </div>
        </div>

        {/* 버튼 스켈레톤 */}
        <div className="mt-4">
          <div className="h-9 w-full rounded bg-muted" />
        </div>
      </div>
    </Card>
  );
}

/**
 * 로딩 상태 컴포넌트
 *
 * 여러 스켈레톤을 그리드로 표시합니다.
 */
function LoadingState() {
  return (
    <div data-testid="cluster-list-loading" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <ClusterSkeleton key={index} />
      ))}
    </div>
  );
}

/**
 * 에러 상태 컴포넌트
 */
function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div
      data-testid="cluster-list-error"
      className="flex flex-col items-center justify-center py-12 px-4"
    >
      <div className="rounded-full bg-red-100 p-3 dark:bg-red-900/20 mb-4">
        <AlertCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
      </div>
      <h3 className="text-lg font-semibold mb-2">Failed to load clusters</h3>
      <p className="text-muted-foreground text-center mb-4 max-w-md">{message}</p>
      <Button onClick={onRetry} variant="outline">
        <RefreshCw className="mr-2 h-4 w-4" />
        Retry
      </Button>
    </div>
  );
}

/**
 * 빈 상태 컴포넌트
 */
function EmptyState({ onAddCluster }: { onAddCluster?: () => void }) {
  return (
    <div
      data-testid="cluster-list-empty"
      className="flex flex-col items-center justify-center py-12 px-4"
    >
      <div className="rounded-full bg-muted p-3 mb-4">
        <Server className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">No clusters configured</h3>
      <p className="text-muted-foreground text-center mb-4 max-w-md">
        Get started by adding your first Kafka cluster connection.
      </p>
      {onAddCluster && (
        <Button onClick={onAddCluster}>
          <Plus className="mr-2 h-4 w-4" />
          Add Cluster
        </Button>
      )}
    </div>
  );
}

/**
 * ClusterList 컴포넌트
 *
 * 클러스터 목록을 그리드 형태로 표시합니다.
 * SWR을 사용하여 데이터를 가져오고 캐싱합니다.
 *
 * @param props - ClusterListProps
 * @returns ClusterList 컴포넌트
 *
 * @example
 * ```tsx
 * function DashboardPage() {
 *   const [selectedCluster, setSelectedCluster] = useState<Cluster | null>(null);
 *
 *   return (
 *     <ClusterList
 *       selectedClusterId={selectedCluster?.id}
 *       onSelect={setSelectedCluster}
 *       onAddCluster={() => setShowAddDialog(true)}
 *     />
 *   );
 * }
 * ```
 */
export function ClusterList({
  onSelect,
  onAddCluster,
  selectedClusterId,
  columns,
}: ClusterListProps) {
  const { clusters, isLoading, error, refresh, isValidating } = useClusters();

  /**
   * 클러스터 선택 핸들러
   */
  const handleSelect = useCallback(
    (cluster: Cluster) => {
      onSelect?.(cluster);
    },
    [onSelect]
  );

  /**
   * 연결 테스트 완료 핸들러
   */
  const handleTestComplete = useCallback((result: ConnectionTestResult) => {
    // 테스트 결과에 따른 추가 처리 (예: 토스트 알림)
    console.log('Connection test result:', result);
  }, []);

  /**
   * 재시도 핸들러
   */
  const handleRetry = useCallback(() => {
    refresh();
  }, [refresh]);

  // 로딩 상태
  if (isLoading) {
    return <LoadingState />;
  }

  // 에러 상태
  if (error) {
    return <ErrorState message={error.message} onRetry={handleRetry} />;
  }

  // 빈 상태
  if (clusters.length === 0) {
    return <EmptyState onAddCluster={onAddCluster} />;
  }

  // 그리드 컬럼 클래스 결정
  const gridCols = columns
    ? `grid-cols-${columns}`
    : 'sm:grid-cols-2 lg:grid-cols-3';

  return (
    <div data-testid="cluster-list" className="space-y-4">
      {/* 헤더 영역 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold">Clusters</h2>
          <span className="text-sm text-muted-foreground">
            ({clusters.length})
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRetry}
            disabled={isValidating}
            aria-label="Refresh clusters"
          >
            <RefreshCw
              className={cn('h-4 w-4', isValidating && 'animate-spin')}
            />
          </Button>
          {onAddCluster && (
            <Button size="sm" onClick={onAddCluster}>
              <Plus className="mr-2 h-4 w-4" />
              Add Cluster
            </Button>
          )}
        </div>
      </div>

      {/* 클러스터 그리드 */}
      <div className={cn('grid gap-4', gridCols)}>
        {clusters.map((cluster) => (
          <ClusterCard
            key={cluster.id}
            cluster={cluster}
            selected={selectedClusterId === cluster.id}
            onClick={handleSelect}
            onTestComplete={handleTestComplete}
          />
        ))}
      </div>
    </div>
  );
}

export default ClusterList;
