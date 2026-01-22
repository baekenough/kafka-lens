'use client';

/**
 * ConsumerGroupDetail 컴포넌트
 *
 * 컨슈머 그룹의 상세 정보와 멤버 테이블을 표시합니다.
 * - 그룹 기본 정보 (상태, 코디네이터, 멤버 수)
 * - 멤버 목록 테이블 (클라이언트 ID, 호스트, 할당된 파티션)
 * - 로딩 및 에러 상태 처리
 *
 * @module components/consumer/ConsumerGroupDetail
 */
import { useCallback, useState } from 'react';
import { RefreshCw, AlertCircle, User, Server, Layers, BarChart3, Table as TableIcon, Activity } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useConsumerGroup } from '@/hooks/useConsumerGroups';
import { useLag, formatLag, getLagStatus } from '@/hooks/useLag';
import { LagTable, LagChart } from '@/components/lag';
import { cn } from '@/lib/utils';
import type { ConsumerGroup, ConsumerGroupState, ConsumerGroupMember, MemberAssignment } from '@/types';

/**
 * ConsumerGroupDetail 컴포넌트 Props
 */
export interface ConsumerGroupDetailProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 그룹 ID */
  groupId: string;
  /** 뒤로 가기 콜백 */
  onBack?: () => void;
}

/**
 * 컨슈머 그룹 상태에 따른 배지 변형 결정
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
 * 상태 설명 텍스트
 */
function getStateDescription(state: ConsumerGroupState): string {
  switch (state) {
    case 'Stable':
      return 'All members have joined and partitions are assigned';
    case 'PreparingRebalance':
      return 'Group is preparing for a rebalance';
    case 'CompletingRebalance':
      return 'Group is completing a rebalance';
    case 'Empty':
      return 'No active members, offsets may still exist';
    case 'Dead':
      return 'Group has no members and will be removed';
    default:
      return 'Unknown state';
  }
}

/**
 * 멤버 할당 정보를 그룹화하여 표시 문자열 생성
 *
 * @param assignments - 할당 목록
 * @returns 표시용 문자열
 */
function formatAssignments(assignments: MemberAssignment[]): string {
  if (!assignments || assignments.length === 0) {
    return 'No partitions assigned';
  }

  // 토픽별로 그룹화
  const byTopic = assignments.reduce(
    (acc, assignment) => {
      const partitions = assignment.partitions || [];
      if (!acc[assignment.topic]) {
        acc[assignment.topic] = [];
      }
      acc[assignment.topic].push(...partitions);
      return acc;
    },
    {} as Record<string, number[]>
  );

  return Object.entries(byTopic)
    .map(([topic, partitions]) => {
      const sortedPartitions = [...partitions].sort((a, b) => a - b);
      return `${topic}[${sortedPartitions.join(', ')}]`;
    })
    .join(', ');
}

/**
 * 로딩 스켈레톤 컴포넌트
 */
function DetailSkeleton() {
  return (
    <div data-testid="consumer-group-detail-skeleton" className="space-y-6">
      {/* 헤더 스켈레톤 */}
      <div className="animate-pulse">
        <div className="h-8 w-64 rounded bg-muted mb-2" />
        <div className="h-4 w-48 rounded bg-muted" />
      </div>

      {/* 정보 카드 스켈레톤 */}
      <div className="grid gap-4 md:grid-cols-3">
        {Array.from({ length: 3 }).map((_, index) => (
          <Card key={index} className="animate-pulse">
            <CardHeader className="pb-2">
              <div className="h-4 w-16 rounded bg-muted" />
            </CardHeader>
            <CardContent>
              <div className="h-8 w-24 rounded bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 테이블 스켈레톤 */}
      <Card className="animate-pulse">
        <CardHeader>
          <div className="h-6 w-32 rounded bg-muted" />
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="h-12 w-full rounded bg-muted" />
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * 에러 상태 컴포넌트
 */
function ErrorState({
  message,
  onRetry,
  onBack,
}: {
  message: string;
  onRetry: () => void;
  onBack?: () => void;
}) {
  return (
    <div
      data-testid="consumer-group-detail-error"
      className="flex flex-col items-center justify-center py-12 px-4"
    >
      <div className="rounded-full bg-red-100 p-3 dark:bg-red-900/20 mb-4">
        <AlertCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
      </div>
      <h3 className="text-lg font-semibold mb-2">Failed to load consumer group</h3>
      <p className="text-muted-foreground text-center mb-4 max-w-md">{message}</p>
      <div className="flex gap-2">
        {onBack && (
          <Button onClick={onBack} variant="outline">
            Back to list
          </Button>
        )}
        <Button onClick={onRetry} variant="outline">
          <RefreshCw className="mr-2 h-4 w-4" />
          Retry
        </Button>
      </div>
    </div>
  );
}

/**
 * 멤버 테이블 컴포넌트
 */
function MembersTable({ members }: { members: ConsumerGroupMember[] }) {
  if (!members || members.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <User className="h-8 w-8 mx-auto mb-2 opacity-50" />
        <p>No active members in this group</p>
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-[25%]">Client ID</TableHead>
          <TableHead className="w-[20%]">Member ID</TableHead>
          <TableHead className="w-[15%]">Host</TableHead>
          <TableHead className="w-[40%]">Assigned Partitions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {members.map((member) => (
          <TableRow key={member.memberId} data-testid={`member-row-${member.memberId}`}>
            <TableCell className="font-medium">
              <span className="truncate block max-w-xs" title={member.clientId}>
                {member.clientId}
              </span>
            </TableCell>
            <TableCell className="text-muted-foreground">
              <span className="truncate block max-w-xs" title={member.memberId}>
                {member.memberId.length > 20
                  ? `${member.memberId.substring(0, 20)}...`
                  : member.memberId}
              </span>
            </TableCell>
            <TableCell className="font-mono text-sm">
              {member.clientHost}
            </TableCell>
            <TableCell>
              <span
                className="text-sm text-muted-foreground truncate block"
                title={formatAssignments(member.assignments)}
              >
                {formatAssignments(member.assignments)}
              </span>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/**
 * ConsumerGroupDetail 컴포넌트
 *
 * 컨슈머 그룹의 상세 정보와 멤버 목록을 표시합니다.
 *
 * @param props - ConsumerGroupDetailProps
 * @returns ConsumerGroupDetail 컴포넌트
 *
 * @example
 * ```tsx
 * function ConsumerGroupPage({ params }: { params: { clusterId: string; groupId: string } }) {
 *   const router = useRouter();
 *
 *   return (
 *     <ConsumerGroupDetail
 *       clusterId={params.clusterId}
 *       groupId={params.groupId}
 *       onBack={() => router.back()}
 *     />
 *   );
 * }
 * ```
 */
export function ConsumerGroupDetail({
  clusterId,
  groupId,
  onBack,
}: ConsumerGroupDetailProps) {
  const { group, isLoading, error, refresh, isValidating } = useConsumerGroup(
    clusterId,
    groupId
  );

  /**
   * 재시도 핸들러
   */
  const handleRetry = useCallback(() => {
    refresh();
  }, [refresh]);

  // 로딩 상태
  if (isLoading) {
    return <DetailSkeleton />;
  }

  // 에러 상태
  if (error) {
    return <ErrorState message={error.message} onRetry={handleRetry} onBack={onBack} />;
  }

  // 그룹을 찾을 수 없는 경우
  if (!group) {
    return (
      <ErrorState
        message={`Consumer group '${groupId}' not found`}
        onRetry={handleRetry}
        onBack={onBack}
      />
    );
  }

  return (
    <div data-testid="consumer-group-detail" className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            {onBack && (
              <Button variant="ghost" size="sm" onClick={onBack}>
                Back
              </Button>
            )}
            <h1 className="text-2xl font-bold">{group.groupId}</h1>
            <Badge variant={getStateVariant(group.state)}>{group.state}</Badge>
          </div>
          <p className="text-muted-foreground">{getStateDescription(group.state)}</p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={handleRetry}
          disabled={isValidating}
        >
          <RefreshCw
            className={cn('h-4 w-4 mr-2', isValidating && 'animate-spin')}
          />
          Refresh
        </Button>
      </div>

      {/* 정보 카드 */}
      <div className="grid gap-4 md:grid-cols-3">
        {/* 상태 카드 */}
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-1">
              <Layers className="h-4 w-4" />
              State
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <Badge variant={getStateVariant(group.state)} className="text-lg">
                {group.state}
              </Badge>
            </div>
          </CardContent>
        </Card>

        {/* 멤버 수 카드 */}
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-1">
              <User className="h-4 w-4" />
              Members
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{group.memberCount}</div>
          </CardContent>
        </Card>

        {/* 코디네이터 카드 */}
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-1">
              <Server className="h-4 w-4" />
              Coordinator
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">Broker {group.coordinator}</div>
          </CardContent>
        </Card>
      </div>

      {/* Lag 및 멤버 탭 */}
      <Tabs defaultValue="lag" className="space-y-4">
        <TabsList>
          <TabsTrigger value="lag" className="gap-2">
            <Activity className="h-4 w-4" />
            Consumer Lag
          </TabsTrigger>
          <TabsTrigger value="members" className="gap-2">
            <User className="h-4 w-4" />
            Members ({group.members.length})
          </TabsTrigger>
        </TabsList>

        {/* Lag 탭 */}
        <TabsContent value="lag">
          <ConsumerLagSection clusterId={clusterId} groupId={groupId} />
        </TabsContent>

        {/* 멤버 탭 */}
        <TabsContent value="members">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5" />
                Members ({group.members.length})
              </CardTitle>
              <CardDescription>
                Active members and their partition assignments
              </CardDescription>
            </CardHeader>
            <CardContent>
              <MembersTable members={group.members} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

/**
 * Consumer Lag 섹션 컴포넌트
 */
function ConsumerLagSection({ clusterId, groupId }: { clusterId: string; groupId: string }) {
  const { lag, isLoading, error, refresh, isValidating } = useLag(clusterId, groupId);
  const [viewMode, setViewMode] = useState<'table' | 'chart'>('table');

  // 로딩 상태
  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-8">
          <div className="flex items-center justify-center gap-2 text-muted-foreground">
            <RefreshCw className="h-5 w-5 animate-spin" />
            Loading lag data...
          </div>
        </CardContent>
      </Card>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <Card>
        <CardContent className="py-8">
          <div className="flex flex-col items-center justify-center gap-3">
            <AlertCircle className="h-8 w-8 text-red-500" />
            <p className="text-muted-foreground">{error.message}</p>
            <Button variant="outline" size="sm" onClick={refresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Lag 데이터 없음
  if (!lag) {
    return (
      <Card>
        <CardContent className="py-8">
          <div className="flex flex-col items-center justify-center text-muted-foreground">
            <Activity className="h-8 w-8 mb-2 opacity-50" />
            <p>No lag data available</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const totalStatus = getLagStatus(lag.totalLag);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Consumer Lag
              {isValidating && (
                <RefreshCw className="h-4 w-4 animate-spin text-muted-foreground" />
              )}
            </CardTitle>
            <CardDescription>
              Total lag: {formatLag(lag.totalLag)} across {lag.topics.length} topic(s)
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            {/* 뷰 모드 토글 */}
            <div className="flex rounded-lg border p-1">
              <Button
                variant={viewMode === 'table' ? 'secondary' : 'ghost'}
                size="sm"
                className="h-7 px-2"
                onClick={() => setViewMode('table')}
              >
                <TableIcon className="h-4 w-4" />
              </Button>
              <Button
                variant={viewMode === 'chart' ? 'secondary' : 'ghost'}
                size="sm"
                className="h-7 px-2"
                onClick={() => setViewMode('chart')}
              >
                <BarChart3 className="h-4 w-4" />
              </Button>
            </div>
            <Button variant="outline" size="sm" onClick={refresh} disabled={isValidating}>
              <RefreshCw className={cn('h-4 w-4 mr-2', isValidating && 'animate-spin')} />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {viewMode === 'table' ? (
          <LagTable lag={lag} />
        ) : (
          <div className="space-y-6">
            <LagChart lag={lag} type="topic" height={250} />
            {lag.topics.length > 0 && (
              <LagChart lag={lag} type="partition" height={200} />
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default ConsumerGroupDetail;
