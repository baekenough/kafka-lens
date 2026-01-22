'use client';

/**
 * 메시지 뷰어 컴포넌트
 *
 * Kafka 토픽의 메시지를 검색하고 표시하는 컴포넌트입니다.
 * - 토픽 선택
 * - 파티션 필터
 * - 오프셋/시간 기반 검색
 * - 메시지 목록 표시
 *
 * @module components/message/MessageViewer
 */
import { useState, useCallback, useMemo } from 'react';
import {
  MessageSquare,
  Search,
  RefreshCw,
  AlertCircle,
  Clock,
  Hash,
  Filter,
  Loader2,
  ChevronDown,
  ChevronUp,
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { useMessages } from '@/hooks/useMessages';
import { useTopics } from '@/hooks/useTopics';
import { cn } from '@/lib/utils';
import type { Message, MessageSearchRequest, SortConfig } from '@/types';

/**
 * MessageViewer Props
 */
interface MessageViewerProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 초기 선택 토픽 */
  initialTopic?: string;
  /** 메시지 클릭 핸들러 */
  onMessageClick?: (message: Message) => void;
  /** 클래스명 */
  className?: string;
}

/**
 * 검색 폼 상태
 */
interface SearchFormState {
  /** 선택된 토픽 */
  topic: string;
  /** 파티션 (전체는 빈 문자열) */
  partition: string;
  /** 시작 오프셋 */
  startOffset: string;
  /** 검색 개수 */
  limit: string;
}

/**
 * 정렬 가능한 컬럼
 */
type SortableColumn = 'partition' | 'offset' | 'timestamp';

/**
 * 날짜 포맷팅 함수
 */
function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

/**
 * 메시지 값 미리보기 (truncate)
 */
function truncateValue(value: string | null, maxLength: number = 100): string {
  if (!value) return '(null)';
  if (value.length <= maxLength) return value;
  return value.slice(0, maxLength) + '...';
}

/**
 * 로딩 스켈레톤 컴포넌트
 */
function LoadingSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="rounded-md border">
        <div className="h-12 border-b bg-muted/50" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-16 border-b flex items-center px-4 gap-4">
            <div className="h-4 w-16 rounded bg-muted" />
            <div className="h-4 w-20 rounded bg-muted" />
            <div className="h-4 w-32 rounded bg-muted" />
            <div className="h-4 flex-1 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * 빈 상태 컴포넌트
 */
function EmptyState({ hasSearched }: { hasSearched: boolean }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="rounded-full bg-muted p-3 mb-4">
        <MessageSquare className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">
        {hasSearched ? 'No messages found' : 'Search for messages'}
      </h3>
      <p className="text-muted-foreground max-w-md">
        {hasSearched
          ? 'No messages match your search criteria. Try adjusting the filters.'
          : 'Select a topic and click Search to view messages.'}
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
      <h3 className="text-lg font-semibold mb-2">Failed to load messages</h3>
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
 * 메시지 뷰어 컴포넌트
 *
 * Kafka 토픽의 메시지를 검색하고 표시합니다.
 *
 * @param props - MessageViewer Props
 * @returns 메시지 뷰어 컴포넌트
 *
 * @example
 * ```tsx
 * <MessageViewer
 *   clusterId="local"
 *   initialTopic="my-topic"
 *   onMessageClick={(msg) => setSelectedMessage(msg)}
 * />
 * ```
 */
export function MessageViewer({
  clusterId,
  initialTopic = '',
  onMessageClick,
  className,
}: MessageViewerProps) {
  /** 토픽 목록 */
  const { topics, isLoading: isTopicsLoading } = useTopics(clusterId);

  /** 메시지 검색 훅 */
  const { searchState, searchMessages, clearMessages } = useMessages(clusterId);

  /** 검색 폼 상태 */
  const [formState, setFormState] = useState<SearchFormState>({
    topic: initialTopic,
    partition: '',
    startOffset: '',
    limit: '100',
  });

  /** 정렬 설정 */
  const [sortConfig, setSortConfig] = useState<SortConfig | null>({
    field: 'offset',
    direction: 'desc',
  });

  /**
   * 폼 값 변경 핸들러
   */
  const handleFormChange = useCallback(
    (field: keyof SearchFormState, value: string) => {
      setFormState((prev) => ({ ...prev, [field]: value }));
    },
    []
  );

  /**
   * 검색 핸들러
   */
  const handleSearch = useCallback(async () => {
    if (!formState.topic) return;

    const request: MessageSearchRequest = {
      topic: formState.topic,
      limit: parseInt(formState.limit, 10) || 100,
    };

    // 파티션 필터
    if (formState.partition && formState.partition !== 'all') {
      request.partition = parseInt(formState.partition, 10);
    }

    // 시작 오프셋
    if (formState.startOffset) {
      request.startOffset = parseInt(formState.startOffset, 10);
    }

    try {
      await searchMessages(request);
    } catch {
      // 에러는 searchState에서 처리됨
    }
  }, [formState, searchMessages]);

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
      return { field: column, direction: 'desc' };
    });
  }, []);

  /**
   * 정렬된 메시지 목록
   */
  const sortedMessages = useMemo(() => {
    const result = [...searchState.messages];

    if (sortConfig) {
      result.sort((a, b) => {
        let aValue: number | string;
        let bValue: number | string;

        if (sortConfig.field === 'timestamp') {
          aValue = new Date(a.timestamp).getTime();
          bValue = new Date(b.timestamp).getTime();
        } else {
          aValue = a[sortConfig.field as keyof Message] as number;
          bValue = b[sortConfig.field as keyof Message] as number;
        }

        if (typeof aValue === 'number' && typeof bValue === 'number') {
          return sortConfig.direction === 'asc' ? aValue - bValue : bValue - aValue;
        }

        return 0;
      });
    }

    return result;
  }, [searchState.messages, sortConfig]);

  /** 선택된 토픽의 파티션 목록 */
  const selectedTopicPartitions = useMemo(() => {
    const topic = topics.find((t) => t.name === formState.topic);
    if (!topic) return [];
    return Array.from({ length: topic.partitionCount }, (_, i) => i);
  }, [topics, formState.topic]);

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="text-lg flex items-center gap-2">
          <MessageSquare className="h-5 w-5" />
          Message Viewer
          {searchState.messages.length > 0 && (
            <Badge variant="secondary" className="ml-2">
              {searchState.messages.length}
            </Badge>
          )}
        </CardTitle>
        <CardDescription>
          Search and browse messages in Kafka topics
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 검색 폼 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 p-4 border rounded-lg bg-muted/30">
          {/* 토픽 선택 */}
          <div className="space-y-2">
            <Label htmlFor="topic" className="flex items-center gap-1">
              <Hash className="h-3 w-3" />
              Topic
            </Label>
            <Select
              value={formState.topic}
              onValueChange={(value) => handleFormChange('topic', value)}
              disabled={isTopicsLoading}
            >
              <SelectTrigger id="topic">
                <SelectValue placeholder="Select topic" />
              </SelectTrigger>
              <SelectContent>
                {topics
                  .filter((t) => !t.isInternal)
                  .map((topic) => (
                    <SelectItem key={topic.name} value={topic.name}>
                      {topic.name}
                    </SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>

          {/* 파티션 필터 */}
          <div className="space-y-2">
            <Label htmlFor="partition" className="flex items-center gap-1">
              <Filter className="h-3 w-3" />
              Partition
            </Label>
            <Select
              value={formState.partition}
              onValueChange={(value) => handleFormChange('partition', value)}
              disabled={!formState.topic}
            >
              <SelectTrigger id="partition">
                <SelectValue placeholder="All partitions" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All partitions</SelectItem>
                {selectedTopicPartitions.map((p) => (
                  <SelectItem key={p} value={String(p)}>
                    Partition {p}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* 시작 오프셋 */}
          <div className="space-y-2">
            <Label htmlFor="startOffset" className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              Start Offset
            </Label>
            <input
              id="startOffset"
              type="number"
              min="0"
              placeholder="Latest"
              value={formState.startOffset}
              onChange={(e) => handleFormChange('startOffset', e.target.value)}
              className="w-full px-3 py-2 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          {/* 검색 개수 */}
          <div className="space-y-2">
            <Label htmlFor="limit">Limit</Label>
            <Select
              value={formState.limit}
              onValueChange={(value) => handleFormChange('limit', value)}
            >
              <SelectTrigger id="limit">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10 messages</SelectItem>
                <SelectItem value="50">50 messages</SelectItem>
                <SelectItem value="100">100 messages</SelectItem>
                <SelectItem value="500">500 messages</SelectItem>
                <SelectItem value="1000">1000 messages</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* 검색 버튼 */}
        <div className="flex items-center gap-2">
          <Button
            onClick={handleSearch}
            disabled={!formState.topic || searchState.isLoading}
          >
            {searchState.isLoading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Search className="mr-2 h-4 w-4" />
            )}
            Search
          </Button>
          {searchState.messages.length > 0 && (
            <Button variant="outline" onClick={clearMessages}>
              Clear
            </Button>
          )}
        </div>

        {/* 결과 영역 */}
        {searchState.isLoading ? (
          <LoadingSkeleton />
        ) : searchState.error ? (
          <ErrorState message={searchState.error.message} onRetry={handleSearch} />
        ) : sortedMessages.length === 0 ? (
          <EmptyState hasSearched={searchState.hasSearched} />
        ) : (
          <div className="rounded-md border overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead
                    className="cursor-pointer select-none w-24"
                    onClick={() => handleSort('partition')}
                  >
                    <span className="flex items-center">
                      Partition
                      <SortIcon column="partition" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none w-28"
                    onClick={() => handleSort('offset')}
                  >
                    <span className="flex items-center">
                      Offset
                      <SortIcon column="offset" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead
                    className="cursor-pointer select-none w-44"
                    onClick={() => handleSort('timestamp')}
                  >
                    <span className="flex items-center">
                      Timestamp
                      <SortIcon column="timestamp" sortConfig={sortConfig} />
                    </span>
                  </TableHead>
                  <TableHead className="w-32">Key</TableHead>
                  <TableHead>Value</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedMessages.map((message) => (
                  <TableRow
                    key={`${message.partition}-${message.offset}`}
                    className={cn(
                      onMessageClick && 'cursor-pointer hover:bg-muted/80'
                    )}
                    onClick={() => onMessageClick?.(message)}
                  >
                    <TableCell className="font-mono text-sm">
                      {message.partition}
                    </TableCell>
                    <TableCell className="font-mono text-sm tabular-nums">
                      {message.offset.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatTimestamp(message.timestamp)}
                    </TableCell>
                    <TableCell className="font-mono text-sm">
                      {message.key ? (
                        <span className="text-primary">
                          {truncateValue(message.key, 30)}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">(null)</span>
                      )}
                    </TableCell>
                    <TableCell className="font-mono text-sm max-w-md truncate">
                      {truncateValue(message.value, 100)}
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

export default MessageViewer;
