'use client';

/**
 * ClusterCard 컴포넌트
 *
 * 개별 클러스터 정보를 카드 형태로 표시합니다.
 * - 클러스터 이름 및 상태
 * - Bootstrap 서버 주소
 * - 연결 테스트 버튼
 * - 클릭하여 선택
 *
 * @module components/cluster/ClusterCard
 */
import { useState, useCallback } from 'react';
import { Server, Zap, Clock, AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { Card, CardHeader, CardContent, CardFooter, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { testConnection, type ConnectionTestResult } from '@/hooks/useClusters';
import type { Cluster, ClusterStatus } from '@/types';

/**
 * ClusterCard 컴포넌트 Props
 */
export interface ClusterCardProps {
  /** 클러스터 데이터 */
  cluster: Cluster;
  /** 선택 상태 여부 */
  selected?: boolean;
  /** 클릭 시 콜백 */
  onClick?: (cluster: Cluster) => void;
  /** 연결 테스트 완료 콜백 */
  onTestComplete?: (result: ConnectionTestResult) => void;
}

/**
 * 상태별 배지 변형 매핑
 */
const statusVariantMap: Record<ClusterStatus, 'healthy' | 'warning' | 'error' | 'offline'> = {
  connected: 'healthy',
  connecting: 'warning',
  disconnected: 'offline',
  error: 'error',
};

/**
 * 상태별 표시 텍스트
 */
const statusTextMap: Record<ClusterStatus, string> = {
  connected: 'connected',
  connecting: 'connecting',
  disconnected: 'disconnected',
  error: 'error',
};

/**
 * 상태별 아이콘
 */
const StatusIcon = ({ status }: { status: ClusterStatus }) => {
  const iconClass = 'h-3.5 w-3.5';

  switch (status) {
    case 'connected':
      return <CheckCircle className={cn(iconClass, 'text-green-600')} />;
    case 'connecting':
      return <Loader2 className={cn(iconClass, 'text-yellow-600 animate-spin')} />;
    case 'disconnected':
      return <AlertCircle className={cn(iconClass, 'text-gray-500')} />;
    case 'error':
      return <AlertCircle className={cn(iconClass, 'text-red-600')} />;
    default:
      return <AlertCircle className={cn(iconClass, 'text-gray-500')} />;
  }
};

/**
 * 날짜 포맷팅 함수
 *
 * @param dateString - ISO 날짜 문자열
 * @returns 포맷된 날짜 문자열
 */
function formatDate(dateString: string | undefined): string {
  if (!dateString) return 'Never';

  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  } catch {
    return 'Invalid date';
  }
}

/**
 * ClusterCard 컴포넌트
 *
 * 클러스터 정보를 카드 형태로 표시합니다.
 *
 * @param props - ClusterCardProps
 * @returns ClusterCard 컴포넌트
 *
 * @example
 * ```tsx
 * <ClusterCard
 *   cluster={cluster}
 *   selected={selectedId === cluster.id}
 *   onClick={handleSelect}
 *   onTestComplete={handleTestResult}
 * />
 * ```
 */
export function ClusterCard({
  cluster,
  selected = false,
  onClick,
  onTestComplete,
}: ClusterCardProps) {
  /** 연결 테스트 진행 중 상태 */
  const [isTesting, setIsTesting] = useState(false);
  /** 마지막 테스트 결과 */
  const [testResult, setTestResult] = useState<ConnectionTestResult | null>(null);

  /**
   * 카드 클릭 핸들러
   */
  const handleClick = useCallback(() => {
    onClick?.(cluster);
  }, [cluster, onClick]);

  /**
   * 연결 테스트 실행
   */
  const handleTestConnection = useCallback(
    async (e: React.MouseEvent) => {
      // 카드 클릭 이벤트 전파 방지
      e.stopPropagation();

      setIsTesting(true);
      setTestResult(null);

      try {
        const result = await testConnection(cluster.id);
        setTestResult(result);
        onTestComplete?.(result);
      } catch (error) {
        const errorResult: ConnectionTestResult = {
          connected: false,
          error: error instanceof Error ? error.message : 'Test failed',
        };
        setTestResult(errorResult);
        onTestComplete?.(errorResult);
      } finally {
        setIsTesting(false);
      }
    },
    [cluster.id, onTestComplete]
  );

  /** Bootstrap 서버 표시용 문자열 */
  const bootstrapServersDisplay = cluster.bootstrapServers.join(', ');

  /** 브로커 수 */
  const brokerCount = cluster.brokers?.length || 0;

  return (
    <Card
      data-testid={`cluster-card-${cluster.id}`}
      data-selected={selected}
      className={cn(
        'cursor-pointer transition-all duration-200 hover:shadow-md',
        selected && 'ring-2 ring-primary shadow-md',
        !selected && 'hover:border-primary/50'
      )}
      onClick={handleClick}
    >
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <Server className="h-5 w-5 shrink-0 text-muted-foreground" />
            <CardTitle className="text-lg truncate">{cluster.name}</CardTitle>
          </div>
          <Badge variant={statusVariantMap[cluster.status]} className="shrink-0">
            <StatusIcon status={cluster.status} />
            <span className="ml-1">{statusTextMap[cluster.status]}</span>
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="pb-3">
        <div className="space-y-3">
          {/* Bootstrap Servers */}
          <div className="space-y-1">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              Bootstrap Servers
            </p>
            <p className="text-sm font-mono truncate" title={bootstrapServersDisplay}>
              {bootstrapServersDisplay}
            </p>
          </div>

          {/* 클러스터 정보 그리드 */}
          <div className="grid grid-cols-2 gap-3 text-sm">
            {/* 브로커 수 */}
            <div className="flex items-center gap-1.5 text-muted-foreground">
              <Server className="h-4 w-4" />
              <span>{brokerCount} broker{brokerCount !== 1 ? 's' : ''}</span>
            </div>

            {/* 마지막 연결 시간 */}
            <div className="flex items-center gap-1.5 text-muted-foreground">
              <Clock className="h-4 w-4" />
              <span>{formatDate(cluster.lastConnectedAt)}</span>
            </div>
          </div>

          {/* 테스트 결과 표시 */}
          {testResult && (
            <div
              className={cn(
                'flex items-center gap-2 text-sm p-2 rounded-md',
                testResult.connected
                  ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400'
                  : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'
              )}
            >
              {testResult.connected ? (
                <>
                  <CheckCircle className="h-4 w-4" />
                  <span>
                    Connected
                    {testResult.latency && ` (${testResult.latency}ms)`}
                  </span>
                </>
              ) : (
                <>
                  <AlertCircle className="h-4 w-4" />
                  <span>{testResult.error || 'Connection failed'}</span>
                </>
              )}
            </div>
          )}
        </div>
      </CardContent>

      <CardFooter className="pt-0">
        <Button
          variant="outline"
          size="sm"
          className="w-full"
          disabled={isTesting}
          onClick={handleTestConnection}
          aria-label="Test connection"
        >
          {isTesting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Testing...
            </>
          ) : (
            <>
              <Zap className="mr-2 h-4 w-4" />
              Test Connection
            </>
          )}
        </Button>
      </CardFooter>
    </Card>
  );
}

export default ClusterCard;
