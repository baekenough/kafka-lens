'use client';

/**
 * 브로커 목록 컴포넌트
 *
 * 클러스터의 브로커 목록을 테이블 형태로 표시합니다.
 * - 브로커 ID, 호스트, 포트, 상태 표시
 * - 컨트롤러 브로커 하이라이트
 * - 정렬 기능
 *
 * @module components/broker/BrokerList
 */
import { useState, useMemo, useCallback } from 'react';
import {
  Server,
  ChevronUp,
  ChevronDown,
  RefreshCw,
  AlertCircle,
  Crown,
  Circle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useBrokers } from '@/hooks/useBrokers';
import { cn } from '@/lib/utils';
import type { Broker, SortConfig, BrokerStatus } from '@/types';

/**
 * BrokerList Props
 */
interface BrokerListProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 브로커 클릭 핸들러 (선택사항) */
  onBrokerClick?: (brokerId: number) => void;
  /** 클래스명 */
  className?: string;
}

/**
 * 정렬 가능한 컬럼
 */
type SortableColumn = 'id' | 'host' | 'port' | 'status';

/**
 * 브로커 상태 배지 컬러
 */
const statusColors: Record<BrokerStatus, string> = {
  online: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  offline: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  unknown: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400',
};

/**
 * 브로커 상태 아이콘 컬러
 */
const statusIconColors: Record<BrokerStatus, string> = {
  online: 'text-green-500',
  offline: 'text-red-500',
  unknown: 'text-gray-500',
};

/**
 * 로딩 스켈레톤 컴포넌트
 */
function LoadingSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="flex justify-between items-center">
        <div className="h-10 w-48 rounded bg-muted" />
        <div className="h-10 w-24 rounded bg-muted" />
      </div>
      <div className="rounded-md border">
        <div className="h-12 border-b bg-muted/50" />
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-14 border-b flex items-center px-4 gap-4">
            <div className="h-4 w-12 rounded bg-muted" />
            <div className="h-4 w-32 rounded bg-muted" />
            <div className="h-4 w-16 rounded bg-muted" />
            <div className="h-4 w-20 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * 빈 상태 컴포넌트
 */
function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="rounded-full bg-muted p-3 mb-4">
        <Server className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">No brokers found</h3>
      <p className="text-muted-foreground max-w-md">
        Unable to retrieve broker information from the cluster.
      </p>
    </div>
  );
}

/**
 * 에러 상태 컴포넌트
 */
function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="rounded-full bg-red-100 p-3 dark:bg-red-900/20 mb-4">
        <AlertCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
      </div>
      <h3 className="text-lg font-semibold mb-2">Failed to load brokers</h3>
      <p className="text-muted-foreground mb-4 max-w-md">{message}</p>
      <Button onClick={onRetry} variant="outline">
        <RefreshCw className="mr-2 h-4 w-4" />
        Retry
      </Button>
    </div>
  );
}

/**
 * 정렬 아이콘 컴포넌트
 */
function SortIcon({ column, sortConfig }: { column: SortableColumn; sortConfig: SortConfig | null }) {
  if (sortConfig?.field !== column) {
    return <ChevronUp className="ml-1 h-4 w-4 text-muted-foreground/50" />;
  }
  return sortConfig.direction === 'asc' ? (
    <ChevronUp className="ml-1 h-4 w-4" />
  ) : (
    <ChevronDown className="ml-1 h-4 w-4" />
  );
}

/**
 * 브로커 목록 컴포넌트
 *
 * 클러스터의 브로커 목록을 테이블로 표시합니다.
 *
 * @param props - BrokerList Props
 * @returns 브로커 목록 컴포넌트
 *
 * @example
 * ```tsx
 * <BrokerList
 *   clusterId="local"
 *   onBrokerClick={(id) => console.log(`Broker ${id} clicked`)}
 * />
 * ```
 */
export function BrokerList({ clusterId, onBrokerClick, className }: BrokerListProps) {
  /** 정렬 설정 */
  const [sortConfig, setSortConfig] = useState<SortConfig | null>({
    field: 'id',
    direction: 'asc',
  });

  /** 브로커 데이터 페칭 */
  const { brokers, isLoading, error, refresh, isValidating } = useBrokers(clusterId);

  /**
   * 정렬 핸들러
   */
  const handleSort = useCallback((column: SortableColumn) => {
    setSortConfig((current) => {
      if (current?.field === column) {
        return {
          field: column,
          direction: current.direction === 'asc' ? 'desc' : 'asc',
        };
      }
      return { field: column, direction: 'asc' };
    });
  }, []);

  /**
   * 정렬된 브로커 목록
   */
  const sortedBrokers = useMemo(() => {
    const result = [...brokers];

    if (sortConfig) {
      result.sort((a, b) => {
        const aValue = a[sortConfig.field as keyof Broker];
        const bValue = b[sortConfig.field as keyof Broker];

        if (typeof aValue === 'string' && typeof bValue === 'string') {
          return sortConfig.direction === 'asc'
            ? aValue.localeCompare(bValue)
            : bValue.localeCompare(aValue);
        }

        if (typeof aValue === 'number' && typeof bValue === 'number') {
          return sortConfig.direction === 'asc' ? aValue - bValue : bValue - aValue;
        }

        return 0;
      });
    }

    return result;
  }, [brokers, sortConfig]);

  /** 컨트롤러 브로커 수 */
  const controllerCount = useMemo(
    () => brokers.filter((b) => b.isController).length,
    [brokers]
  );

  /** 온라인 브로커 수 */
  const onlineCount = useMemo(
    () => brokers.filter((b) => b.status === 'online').length,
    [brokers]
  );

  // 로딩 상태
  if (isLoading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Server className="h-5 w-5" />
            Brokers
          </CardTitle>
          <CardDescription>Loading broker list...</CardDescription>
        </CardHeader>
        <CardContent>
          <LoadingSkeleton />
        </CardContent>
      </Card>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Server className="h-5 w-5" />
            Brokers
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ErrorState message={error.message} onRetry={refresh} />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-lg flex items-center gap-2">
              <Server className="h-5 w-5" />
              Brokers
              <Badge variant="secondary" className="ml-2">
                {brokers.length}
              </Badge>
            </CardTitle>
            <CardDescription>
              {onlineCount} of {brokers.length} brokers online
              {controllerCount > 0 && ` / ${controllerCount} controller`}
            </CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={refresh}
            disabled={isValidating}
          >
            <RefreshCw
              className={cn('mr-2 h-4 w-4', isValidating && 'animate-spin')}
            />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* 브로커 목록 */}
        {sortedBrokers.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead
                    className="cursor-pointer select-none w-20"
                    onClick={() => handleSort('id')}
                  >
                    <span className="flex items-center">
                      ID
                      <SortIcon column="id" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none"
                    onClick={() => handleSort('host')}
                  >
                    <span className="flex items-center">
                      Host
                      <SortIcon column="host" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none text-right w-24"
                    onClick={() => handleSort('port')}
                  >
                    <span className="flex items-center justify-end">
                      Port
                      <SortIcon column="port" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none w-28"
                    onClick={() => handleSort('status')}
                  >
                    <span className="flex items-center">
                      Status
                      <SortIcon column="status" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead className="w-24">Role</TableHead>
                  <TableHead className="w-24">Rack</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedBrokers.map((broker) => (
                  <TableRow
                    key={broker.id}
                    className={cn(
                      onBrokerClick && 'cursor-pointer hover:bg-muted/80'
                    )}
                    onClick={() => onBrokerClick?.(broker.id)}
                  >
                    <TableCell className="font-mono font-medium">
                      {broker.id}
                    </TableCell>
                    <TableCell className="font-mono text-sm">
                      {broker.host}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {broker.port}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Circle
                          className={cn(
                            'h-2 w-2 fill-current',
                            statusIconColors[broker.status]
                          )}
                        />
                        <Badge
                          variant="outline"
                          className={cn('capitalize', statusColors[broker.status])}
                        >
                          {broker.status}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      {broker.isController ? (
                        <Badge className="bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400">
                          <Crown className="mr-1 h-3 w-3" />
                          Controller
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">Follower</span>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {broker.rack || '-'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default BrokerList;
