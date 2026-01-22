/**
 * Lag 차트 컴포넌트
 *
 * Consumer Lag을 시각적인 바 차트로 표시합니다.
 * 외부 라이브러리 없이 Tailwind CSS로 구현합니다.
 *
 * @module components/lag/LagChart
 */
'use client';

import * as React from 'react';
import { cn } from '@/lib/utils';
import { getLagStatus, formatLag, type LagStatus } from '@/hooks/useLag';
import type { ConsumerGroupLagSummary, TopicLagSummary, ConsumerLag } from '@/types';

/**
 * LagChart 컴포넌트 Props
 */
export interface LagChartProps {
  /** Lag 요약 정보 */
  lag: ConsumerGroupLagSummary;
  /** 차트 높이 (기본값: 300) */
  height?: number;
  /** 추가 CSS 클래스 */
  className?: string;
  /** 차트 타입: 토픽별 또는 파티션별 */
  type?: 'topic' | 'partition';
}

/**
 * Lag 상태에 따른 바 색상 클래스를 반환합니다.
 */
function getLagBarColor(status: LagStatus): string {
  switch (status) {
    case 'critical':
      return 'bg-red-500';
    case 'warning':
      return 'bg-yellow-500';
    default:
      return 'bg-green-500';
  }
}

/**
 * 단일 바 컴포넌트
 */
interface BarProps {
  label: string;
  value: number;
  maxValue: number;
  height: number;
}

function Bar({ label, value, maxValue, height }: BarProps) {
  const status = getLagStatus(value);
  const barHeight = maxValue > 0 ? (value / maxValue) * (height - 60) : 0;
  const minBarHeight = 4; // 최소 바 높이

  return (
    <div className="flex flex-col items-center flex-1 min-w-[40px] max-w-[80px]">
      {/* 값 표시 */}
      <div className="text-xs font-mono text-muted-foreground mb-1">
        {formatLag(value)}
      </div>

      {/* 바 컨테이너 */}
      <div
        className="w-full flex items-end justify-center"
        style={{ height: `${height - 60}px` }}
      >
        <div
          className={cn(
            'w-full max-w-[40px] rounded-t transition-all duration-300',
            getLagBarColor(status),
            value === 0 && 'opacity-30'
          )}
          style={{
            height: `${Math.max(barHeight, minBarHeight)}px`,
          }}
          title={`${label}: ${value.toLocaleString()}`}
        />
      </div>

      {/* 레이블 */}
      <div
        className="text-xs text-center mt-2 truncate w-full px-1"
        title={label}
      >
        {label}
      </div>
    </div>
  );
}

/**
 * 토픽별 바 차트
 */
function TopicBarChart({ lag, height }: { lag: ConsumerGroupLagSummary; height: number }) {
  const maxLag = Math.max(...lag.topics.map((t) => t.totalLag), 1);

  if (lag.topics.length === 0) {
    return (
      <div
        className="flex items-center justify-center text-muted-foreground"
        style={{ height: `${height}px` }}
      >
        No lag data available
      </div>
    );
  }

  return (
    <div className="flex items-end justify-around gap-2 px-4" style={{ height: `${height}px` }}>
      {lag.topics.map((topic) => (
        <Bar
          key={topic.topic}
          label={topic.topic}
          value={topic.totalLag}
          maxValue={maxLag}
          height={height}
        />
      ))}
    </div>
  );
}

/**
 * 파티션별 바 차트 (특정 토픽)
 */
function PartitionBarChart({
  topic,
  height,
}: {
  topic: TopicLagSummary;
  height: number;
}) {
  const maxLag = Math.max(...topic.partitions.map((p) => p.lag), 1);

  return (
    <div>
      <div className="text-sm font-medium mb-2 px-4">{topic.topic}</div>
      <div className="flex items-end justify-around gap-1 px-4" style={{ height: `${height - 30}px` }}>
        {topic.partitions.map((partition) => (
          <Bar
            key={`${topic.topic}-${partition.partition}`}
            label={`P${partition.partition}`}
            value={partition.lag}
            maxValue={maxLag}
            height={height - 30}
          />
        ))}
      </div>
    </div>
  );
}

/**
 * Lag 차트 컴포넌트
 *
 * Consumer Group의 Lag을 시각적인 바 차트로 표시합니다.
 *
 * @example
 * ```tsx
 * // 토픽별 차트
 * <LagChart lag={lagSummary} type="topic" />
 *
 * // 파티션별 차트
 * <LagChart lag={lagSummary} type="partition" />
 * ```
 */
export function LagChart({
  lag,
  height = 300,
  className,
  type = 'topic',
}: LagChartProps) {
  return (
    <div className={cn('w-full', className)}>
      {/* Y축 레이블 */}
      <div className="flex items-center gap-2 mb-4 px-4">
        <span className="text-sm font-medium">Lag</span>
        <div className="flex-1 h-px bg-border" />
      </div>

      {type === 'topic' ? (
        <TopicBarChart lag={lag} height={height} />
      ) : (
        <div className="space-y-6">
          {lag.topics.map((topic) => (
            <PartitionBarChart key={topic.topic} topic={topic} height={height / 2} />
          ))}
        </div>
      )}

      {/* X축 레이블 */}
      <div className="flex items-center gap-2 mt-4 px-4">
        <div className="flex-1 h-px bg-border" />
        <span className="text-sm text-muted-foreground">
          {type === 'topic' ? 'Topics' : 'Partitions'}
        </span>
        <div className="flex-1 h-px bg-border" />
      </div>

      {/* 범례 */}
      <div className="flex items-center justify-center gap-6 mt-4 text-xs">
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded bg-green-500" />
          Normal
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded bg-yellow-500" />
          Warning
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded bg-red-500" />
          Critical
        </span>
      </div>
    </div>
  );
}

/**
 * 스파크라인 형태의 미니 Lag 차트
 */
export interface LagSparklineProps {
  /** 파티션별 Lag 목록 */
  partitions: ConsumerLag[];
  /** 차트 너비 (기본값: 100) */
  width?: number;
  /** 차트 높이 (기본값: 24) */
  height?: number;
  /** 추가 CSS 클래스 */
  className?: string;
}

/**
 * 스파크라인 컴포넌트
 *
 * 작은 공간에 Lag 분포를 간단히 표시합니다.
 */
export function LagSparkline({
  partitions,
  width = 100,
  height = 24,
  className,
}: LagSparklineProps) {
  if (partitions.length === 0) {
    return null;
  }

  const maxLag = Math.max(...partitions.map((p) => p.lag), 1);
  const barWidth = Math.max(2, (width - partitions.length + 1) / partitions.length);

  return (
    <div
      className={cn('flex items-end gap-px', className)}
      style={{ width: `${width}px`, height: `${height}px` }}
    >
      {partitions.map((partition) => {
        const status = getLagStatus(partition.lag);
        const barHeight = maxLag > 0 ? (partition.lag / maxLag) * height : 0;

        return (
          <div
            key={partition.partition}
            className={cn(
              'rounded-t transition-all',
              getLagBarColor(status),
              partition.lag === 0 && 'opacity-30'
            )}
            style={{
              width: `${barWidth}px`,
              height: `${Math.max(barHeight, 2)}px`,
            }}
            title={`P${partition.partition}: ${partition.lag.toLocaleString()}`}
          />
        );
      })}
    </div>
  );
}

export default LagChart;
