'use client';

/**
 * 토픽 목록 컴포넌트
 *
 * 클러스터의 토픽 목록을 검색 가능한 테이블 형태로 표시합니다.
 * - 토픽 이름 검색
 * - 내부 토픽 포함/제외 토글
 * - 정렬 기능
 *
 * @module components/topic/TopicList
 */
import { useState, useMemo, useCallback } from 'react';
import Link from 'next/link';
import {
  Search,
  FileText,
  ChevronUp,
  ChevronDown,
  RefreshCw,
  Eye,
  EyeOff,
  AlertCircle,
  Loader2,
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
import { useTopics } from '@/hooks/useTopics';
import { cn } from '@/lib/utils';
import type { Topic, SortConfig } from '@/types';

/**
 * TopicList Props
 */
interface TopicListProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 토픽 클릭 핸들러 (선택사항) */
  onTopicClick?: (topicName: string) => void;
  /** 클래스명 */
  className?: string;
}

/**
 * 정렬 가능한 컬럼
 */
type SortableColumn = 'name' | 'partitionCount' | 'replicationFactor';

/**
 * 숫자 포맷팅 함수
 */
function formatNumber(num: number): string {
  return num.toLocaleString('ko-KR');
}

/**
 * 로딩 스켈레톤 컴포넌트
 */
function LoadingSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="flex justify-between items-center">
        <div className="h-10 w-64 rounded bg-muted" />
        <div className="h-10 w-32 rounded bg-muted" />
      </div>
      <div className="rounded-md border">
        <div className="h-12 border-b bg-muted/50" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 border-b flex items-center px-4 gap-4">
            <div className="h-4 w-48 rounded bg-muted" />
            <div className="h-4 w-16 rounded bg-muted" />
            <div className="h-4 w-16 rounded bg-muted" />
            <div className="h-4 w-16 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * 빈 상태 컴포넌트
 */
function EmptyState({ search }: { search: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="rounded-full bg-muted p-3 mb-4">
        <FileText className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">No topics found</h3>
      <p className="text-muted-foreground max-w-md">
        {search
          ? `No topics matching "${search}" were found.`
          : 'This cluster has no topics yet.'}
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
      <h3 className="text-lg font-semibold mb-2">Failed to load topics</h3>
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
 * 토픽 목록 컴포넌트
 *
 * 클러스터의 토픽 목록을 검색 가능한 테이블로 표시합니다.
 *
 * @param props - TopicList Props
 * @returns 토픽 목록 컴포넌트
 *
 * @example
 * ```tsx
 * <TopicList
 *   clusterId="local"
 *   onTopicClick={(name) => router.push(`/clusters/local/topics/${name}`)}
 * />
 * ```
 */
export function TopicList({ clusterId, onTopicClick, className }: TopicListProps) {
  /** 검색어 상태 */
  const [search, setSearch] = useState('');
  /** 내부 토픽 포함 여부 */
  const [includeInternal, setIncludeInternal] = useState(false);
  /** 정렬 설정 */
  const [sortConfig, setSortConfig] = useState<SortConfig | null>({
    field: 'name',
    direction: 'asc',
  });

  /** 토픽 데이터 페칭 */
  const { topics, isLoading, error, refresh, isValidating } = useTopics(clusterId, {
    includeInternal,
  });

  /**
   * 검색 핸들러
   */
  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(e.target.value);
  }, []);

  /**
   * 내부 토픽 토글 핸들러
   */
  const handleToggleInternal = useCallback(() => {
    setIncludeInternal((prev) => !prev);
  }, []);

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
   * 필터링 및 정렬된 토픽 목록
   */
  const filteredTopics = useMemo(() => {
    let result = [...topics];

    // 검색 필터링
    if (search.trim()) {
      const searchLower = search.toLowerCase();
      result = result.filter((topic) =>
        topic.name.toLowerCase().includes(searchLower)
      );
    }

    // 정렬
    if (sortConfig) {
      result.sort((a, b) => {
        const aValue = a[sortConfig.field as keyof Topic];
        const bValue = b[sortConfig.field as keyof Topic];

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
  }, [topics, search, sortConfig]);

  // 로딩 상태
  if (isLoading) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Topics
          </CardTitle>
          <CardDescription>Loading topic list...</CardDescription>
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
            <FileText className="h-5 w-5" />
            Topics
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
              <FileText className="h-5 w-5" />
              Topics
              <Badge variant="secondary" className="ml-2">
                {filteredTopics.length}
              </Badge>
            </CardTitle>
            <CardDescription>
              {topics.length} topics in this cluster
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
        {/* 검색 및 필터 */}
        <div className="flex flex-col sm:flex-row gap-4 mb-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search topics..."
              value={search}
              onChange={handleSearchChange}
              className="w-full pl-10 pr-4 py-2 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>
          <Button
            variant="outline"
            size="default"
            onClick={handleToggleInternal}
            className="shrink-0"
          >
            {includeInternal ? (
              <>
                <EyeOff className="mr-2 h-4 w-4" />
                Hide Internal
              </>
            ) : (
              <>
                <Eye className="mr-2 h-4 w-4" />
                Show Internal
              </>
            )}
          </Button>
        </div>

        {/* 토픽 목록 */}
        {filteredTopics.length === 0 ? (
          <EmptyState search={search} />
        ) : (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead
                    className="cursor-pointer select-none"
                    onClick={() => handleSort('name')}
                  >
                    <span className="flex items-center">
                      Topic Name
                      <SortIcon column="name" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none text-right w-32"
                    onClick={() => handleSort('partitionCount')}
                  >
                    <span className="flex items-center justify-end">
                      Partitions
                      <SortIcon column="partitionCount" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none text-right w-32"
                    onClick={() => handleSort('replicationFactor')}
                  >
                    <span className="flex items-center justify-end">
                      Replicas
                      <SortIcon column="replicationFactor" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead className="w-24">Type</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTopics.map((topic) => (
                  <TableRow
                    key={topic.name}
                    className={cn(
                      'cursor-pointer',
                      onTopicClick && 'hover:bg-muted/80'
                    )}
                    onClick={() => onTopicClick?.(topic.name)}
                  >
                    <TableCell className="font-mono text-sm">
                      <Link
                        href={`/clusters/${clusterId}/topics/${encodeURIComponent(topic.name)}`}
                        className="text-primary hover:underline"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {topic.name}
                      </Link>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatNumber(topic.partitionCount)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {topic.replicationFactor}
                    </TableCell>
                    <TableCell>
                      {topic.isInternal ? (
                        <Badge variant="secondary">Internal</Badge>
                      ) : (
                        <Badge variant="outline">User</Badge>
                      )}
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

export default TopicList;
