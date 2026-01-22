/**
 * Lag 테이블 컴포넌트
 *
 * Consumer Lag 정보를 테이블 형식으로 표시합니다.
 * Lag가 1000 이상이면 warning 색상으로 표시됩니다.
 *
 * @module components/lag/LagTable
 */
'use client';

import * as React from 'react';
import { AlertTriangle, AlertCircle, CheckCircle2 } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { getLagStatus, formatLag, LAG_THRESHOLDS, type LagStatus } from '@/hooks/useLag';
import type { ConsumerGroupLagSummary, ConsumerLag, TopicLagSummary } from '@/types';

/**
 * LagTable 컴포넌트 Props
 */
export interface LagTableProps {
  /** Lag 요약 정보 */
  lag: ConsumerGroupLagSummary;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * Lag 상태에 따른 아이콘을 반환합니다.
 */
function LagStatusIcon({ status }: { status: LagStatus }) {
  switch (status) {
    case 'critical':
      return <AlertCircle className="h-4 w-4 text-red-500" />;
    case 'warning':
      return <AlertTriangle className="h-4 w-4 text-yellow-500" />;
    default:
      return <CheckCircle2 className="h-4 w-4 text-green-500" />;
  }
}

/**
 * Lag 상태에 따른 배지 스타일을 반환합니다.
 */
function getLagBadgeVariant(status: LagStatus): 'default' | 'secondary' | 'warning' | 'error' {
  switch (status) {
    case 'critical':
      return 'error';
    case 'warning':
      return 'warning';
    default:
      return 'secondary';
  }
}

/**
 * 파티션 Lag 행 컴포넌트
 */
function PartitionLagRow({ partition }: { partition: ConsumerLag }) {
  const status = getLagStatus(partition.lag);

  return (
    <TableRow
      className={cn(
        status === 'critical' && 'bg-red-50 dark:bg-red-900/10',
        status === 'warning' && 'bg-yellow-50 dark:bg-yellow-900/10'
      )}
    >
      <TableCell className="font-mono text-sm">
        {partition.partition}
      </TableCell>
      <TableCell className="font-mono text-sm">
        {partition.currentOffset.toLocaleString()}
      </TableCell>
      <TableCell className="font-mono text-sm">
        {partition.endOffset.toLocaleString()}
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2">
          <LagStatusIcon status={status} />
          <span
            className={cn(
              'font-mono text-sm font-medium',
              status === 'critical' && 'text-red-600 dark:text-red-400',
              status === 'warning' && 'text-yellow-600 dark:text-yellow-400',
              status === 'normal' && 'text-green-600 dark:text-green-400'
            )}
          >
            {formatLag(partition.lag)}
          </span>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant={getLagBadgeVariant(status)}>
          {status}
        </Badge>
      </TableCell>
    </TableRow>
  );
}

/**
 * 토픽 Lag 섹션 컴포넌트
 */
function TopicLagSection({ topic }: { topic: TopicLagSummary }) {
  const status = getLagStatus(topic.totalLag);

  return (
    <div className="mb-6 last:mb-0">
      {/* 토픽 헤더 */}
      <div className="flex items-center justify-between mb-3 px-4 py-2 bg-muted/50 rounded-lg">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-sm">{topic.topic}</span>
          <Badge variant="outline" className="text-xs">
            {topic.partitions.length} partitions
          </Badge>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Total Lag:</span>
          <span
            className={cn(
              'font-mono text-sm font-bold',
              status === 'critical' && 'text-red-600 dark:text-red-400',
              status === 'warning' && 'text-yellow-600 dark:text-yellow-400',
              status === 'normal' && 'text-green-600 dark:text-green-400'
            )}
          >
            {formatLag(topic.totalLag)}
          </span>
        </div>
      </div>

      {/* 파티션 테이블 */}
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[100px]">Partition</TableHead>
            <TableHead>Current Offset</TableHead>
            <TableHead>End Offset</TableHead>
            <TableHead>Lag</TableHead>
            <TableHead className="w-[100px]">Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {topic.partitions.map((partition) => (
            <PartitionLagRow
              key={`${topic.topic}-${partition.partition}`}
              partition={partition}
            />
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

/**
 * Lag 테이블 컴포넌트
 *
 * Consumer Group의 Lag 정보를 토픽별로 테이블 형식으로 표시합니다.
 *
 * @example
 * ```tsx
 * <LagTable lag={lagSummary} />
 * ```
 */
export function LagTable({ lag, className }: LagTableProps) {
  const totalStatus = getLagStatus(lag.totalLag);

  return (
    <div className={cn('space-y-4', className)}>
      {/* 요약 정보 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-4 bg-muted/50 rounded-lg">
          <p className="text-sm text-muted-foreground">Total Lag</p>
          <p
            className={cn(
              'text-2xl font-bold font-mono',
              totalStatus === 'critical' && 'text-red-600 dark:text-red-400',
              totalStatus === 'warning' && 'text-yellow-600 dark:text-yellow-400',
              totalStatus === 'normal' && 'text-green-600 dark:text-green-400'
            )}
          >
            {formatLag(lag.totalLag)}
          </p>
        </div>
        <div className="p-4 bg-muted/50 rounded-lg">
          <p className="text-sm text-muted-foreground">Topics</p>
          <p className="text-2xl font-bold">{lag.topics.length}</p>
        </div>
        <div className="p-4 bg-muted/50 rounded-lg">
          <p className="text-sm text-muted-foreground">Partitions</p>
          <p className="text-2xl font-bold">
            {lag.topics.reduce((sum, t) => sum + t.partitions.length, 0)}
          </p>
        </div>
        <div className="p-4 bg-muted/50 rounded-lg">
          <p className="text-sm text-muted-foreground">Status</p>
          <div className="flex items-center gap-2 mt-1">
            <LagStatusIcon status={totalStatus} />
            <Badge variant={getLagBadgeVariant(totalStatus)} className="text-sm">
              {totalStatus === 'critical' && 'Critical'}
              {totalStatus === 'warning' && 'Warning'}
              {totalStatus === 'normal' && 'Healthy'}
            </Badge>
          </div>
        </div>
      </div>

      {/* 임계값 안내 */}
      <div className="flex items-center gap-4 text-xs text-muted-foreground px-1">
        <span className="flex items-center gap-1">
          <div className="w-2 h-2 rounded-full bg-green-500" />
          Normal: &lt; {LAG_THRESHOLDS.WARNING.toLocaleString()}
        </span>
        <span className="flex items-center gap-1">
          <div className="w-2 h-2 rounded-full bg-yellow-500" />
          Warning: {LAG_THRESHOLDS.WARNING.toLocaleString()} - {(LAG_THRESHOLDS.CRITICAL - 1).toLocaleString()}
        </span>
        <span className="flex items-center gap-1">
          <div className="w-2 h-2 rounded-full bg-red-500" />
          Critical: &gt;= {LAG_THRESHOLDS.CRITICAL.toLocaleString()}
        </span>
      </div>

      {/* 토픽별 Lag 테이블 */}
      {lag.topics.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No lag data available
        </div>
      ) : (
        lag.topics.map((topic) => (
          <TopicLagSection key={topic.topic} topic={topic} />
        ))
      )}
    </div>
  );
}

export default LagTable;
