'use client';

/**
 * ConsumerGroupList 컴포넌트
 *
 * 컨슈머 그룹 목록을 테이블 형태로 표시합니다.
 * - 로딩 스켈레톤
 * - 에러 상태 처리
 * - 빈 상태 처리
 * - 상태별 배지 표시
 * - 그룹 선택 기능
 *
 * @module components/consumer/ConsumerGroupList
 */
import { useCallback } from 'react';
import { RefreshCw, AlertCircle, Users } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useConsumerGroups } from '@/hooks/useConsumerGroups';
import { cn } from '@/lib/utils';
import type { ConsumerGroup, ConsumerGroupState } from '@/types';

/**
 * ConsumerGroupList 컴포넌트 Props
 */
export interface ConsumerGroupListProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 컨슈머 그룹 선택 시 콜백 */
  onSelect?: (group: ConsumerGroup) => void;
  /** 현재 선택된 그룹 ID */
  selectedGroupId?: string;
}

/**
 * 컨슈머 그룹 상태에 따른 배지 변형 결정
 *
 * @param state - 컨슈머 그룹 상태
 * @returns 배지 변형
 */
function getStateVariant(
  state: ConsumerGroupState
): 'healthy' | 'warning' | 'error' | 'offline' | 'default' {
  switch (state) {
    case 'Stable':
      return 'healthy';
    case 'PreparingRebalance':
    case 'CompletingRebalance':
      return 'warning';
    case 'Dead':
      return 'error';
    case 'Empty':
      return 'offline';
    default:
      return 'default';
  }
}

/**
 * 컨슈머 그룹 상태 한글 변환
 *
 * @param state - 컨슈머 그룹 상태
 * @returns 한글 상태명
 */
function getStateLabel(state: ConsumerGroupState): string {
  switch (state) {
    case 'Stable':
      return 'Stable';
    case 'PreparingRebalance':
      return 'Rebalancing';
    case 'CompletingRebalance':
      return 'Rebalancing';
    case 'Empty':
      return 'Empty';
    case 'Dead':
      return 'Dead';
    default:
      return 'Unknown';
  }
}

/**
 * 로딩 스켈레톤 컴포넌트
 *
 * 데이터 로딩 중 표시되는 플레이스홀더입니다.
 */
function TableSkeleton() {
  return (
    <TableBody data-testid="consumer-group-skeleton">
      {Array.from({ length: 5 }).map((_, index) => (
        <TableRow key={index} className="animate-pulse">
          <TableCell>
            <div className="h-4 w-48 rounded bg-muted" />
          </TableCell>
          <TableCell>
            <div className="h-6 w-20 rounded-full bg-muted" />
          </TableCell>
          <TableCell>
            <div className="h-4 w-12 rounded bg-muted" />
          </TableCell>
          <TableCell>
            <div className="h-4 w-16 rounded bg-muted" />
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
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
      data-testid="consumer-group-list-error"
      className="flex flex-col items-center justify-center py-12 px-4"
    >
      <div className="rounded-full bg-red-100 p-3 dark:bg-red-900/20 mb-4">
        <AlertCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
      </div>
      <h3 className="text-lg font-semibold mb-2">Failed to load consumer groups</h3>
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
function EmptyState() {
  return (
    <div
      data-testid="consumer-group-list-empty"
      className="flex flex-col items-center justify-center py-12 px-4"
    >
      <div className="rounded-full bg-muted p-3 mb-4">
        <Users className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">No consumer groups found</h3>
      <p className="text-muted-foreground text-center max-w-md">
        This cluster doesn't have any consumer groups yet. Consumer groups are created when
        applications subscribe to topics.
      </p>
    </div>
  );
}

/**
 * ConsumerGroupList 컴포넌트
 *
 * 컨슈머 그룹 목록을 테이블 형태로 표시합니다.
 * SWR을 사용하여 데이터를 가져오고 캐싱합니다.
 *
 * @param props - ConsumerGroupListProps
 * @returns ConsumerGroupList 컴포넌트
 *
 * @example
 * ```tsx
 * function ConsumerGroupsPage({ clusterId }: { clusterId: string }) {
 *   const [selectedGroup, setSelectedGroup] = useState<ConsumerGroup | null>(null);
 *
 *   return (
 *     <ConsumerGroupList
 *       clusterId={clusterId}
 *       selectedGroupId={selectedGroup?.groupId}
 *       onSelect={setSelectedGroup}
 *     />
 *   );
 * }
 * ```
 */
export function ConsumerGroupList({
  clusterId,
  onSelect,
  selectedGroupId,
}: ConsumerGroupListProps) {
  const { groups, isLoading, error, refresh, isValidating } = useConsumerGroups(clusterId);

  /**
   * 그룹 선택 핸들러
   */
  const handleSelect = useCallback(
    (group: ConsumerGroup) => {
      onSelect?.(group);
    },
    [onSelect]
  );

  /**
   * 재시도 핸들러
   */
  const handleRetry = useCallback(() => {
    refresh();
  }, [refresh]);

  // 에러 상태
  if (error) {
    return <ErrorState message={error.message} onRetry={handleRetry} />;
  }

  // 빈 상태 (로딩 완료 후)
  if (!isLoading && groups.length === 0) {
    return <EmptyState />;
  }

  return (
    <div data-testid="consumer-group-list" className="space-y-4">
      {/* 헤더 영역 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold">Consumer Groups</h2>
          <span className="text-sm text-muted-foreground">
            ({groups.length})
          </span>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleRetry}
          disabled={isValidating}
          aria-label="Refresh consumer groups"
        >
          <RefreshCw
            className={cn('h-4 w-4', isValidating && 'animate-spin')}
          />
        </Button>
      </div>

      {/* 테이블 영역 */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[40%]">Group ID</TableHead>
              <TableHead className="w-[20%]">State</TableHead>
              <TableHead className="w-[15%]">Members</TableHead>
              <TableHead className="w-[25%]">Coordinator</TableHead>
            </TableRow>
          </TableHeader>
          {isLoading ? (
            <TableSkeleton />
          ) : (
            <TableBody>
              {groups.map((group) => (
                <TableRow
                  key={group.groupId}
                  className={cn(
                    'cursor-pointer transition-colors',
                    selectedGroupId === group.groupId && 'bg-muted/50',
                    'hover:bg-muted/30'
                  )}
                  onClick={() => handleSelect(group)}
                  data-testid={`consumer-group-row-${group.groupId}`}
                >
                  <TableCell className="font-medium">
                    <span className="truncate block max-w-xs" title={group.groupId}>
                      {group.groupId}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={getStateVariant(group.state)}
                      data-testid={`state-badge-${group.groupId}`}
                    >
                      {getStateLabel(group.state)}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <span className="flex items-center gap-1">
                      <Users className="h-4 w-4 text-muted-foreground" />
                      {group.memberCount}
                    </span>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    Broker {group.coordinator}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          )}
        </Table>
      </div>
    </div>
  );
}

export default ConsumerGroupList;
