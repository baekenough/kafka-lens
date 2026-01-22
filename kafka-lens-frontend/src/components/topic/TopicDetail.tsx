'use client';

/**
 * 토픽 상세 정보 컴포넌트
 *
 * 토픽의 상세 정보, 파티션 목록, 설정값을 표시합니다.
 * - 토픽 기본 정보 카드
 * - 파티션 목록 테이블
 * - 토픽 설정 테이블
 *
 * @module components/topic/TopicDetail
 */
import { useState, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  FileText,
  Layers,
  Settings,
  RefreshCw,
  AlertCircle,
  CheckCircle,
  AlertTriangle,
  Copy,
  Check,
  Server,
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useTopic, type TopicDetail as TopicDetailType, type PartitionInfo } from '@/hooks/useTopics';
import { cn } from '@/lib/utils';

/**
 * TopicDetail Props
 */
interface TopicDetailProps {
  /** 클러스터 ID */
  clusterId: string;
  /** 토픽 이름 */
  topicName: string;
  /** 클래스명 */
  className?: string;
}

/**
 * 숫자 포맷팅 함수
 */
function formatNumber(num: number): string {
  return num.toLocaleString('ko-KR');
}

/**
 * 바이트 포맷팅 함수
 */
function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  if (bytes < 0) return 'Unlimited';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

/**
 * 시간 포맷팅 함수 (ms -> human readable)
 */
function formatMs(ms: number): string {
  if (ms < 0) return 'Unlimited';
  if (ms === 0) return '0';

  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds}s`;

  const minutes = seconds / 60;
  if (minutes < 60) return `${Math.round(minutes)}m`;

  const hours = minutes / 60;
  if (hours < 24) return `${Math.round(hours)}h`;

  const days = hours / 24;
  return `${Math.round(days)}d`;
}

/**
 * 로딩 스켈레톤 컴포넌트
 */
function LoadingSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* 헤더 스켈레톤 */}
      <div className="flex items-center gap-4">
        <div className="h-10 w-10 rounded bg-muted" />
        <div>
          <div className="h-6 w-48 rounded bg-muted mb-2" />
          <div className="h-4 w-32 rounded bg-muted" />
        </div>
      </div>

      {/* 카드 스켈레톤 */}
      <div className="grid gap-4 md:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="pt-6">
              <div className="h-8 w-16 rounded bg-muted mb-2" />
              <div className="h-4 w-24 rounded bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 테이블 스켈레톤 */}
      <div className="rounded-md border">
        <div className="h-12 border-b bg-muted/50" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 border-b" />
        ))}
      </div>
    </div>
  );
}

/**
 * 에러 상태 컴포넌트
 */
function ErrorState({ message, onBack }: { message: string; onBack: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="rounded-full bg-red-100 p-3 dark:bg-red-900/20 mb-4">
        <AlertCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
      </div>
      <h3 className="text-lg font-semibold mb-2">Failed to load topic</h3>
      <p className="text-muted-foreground text-center mb-4 max-w-md">{message}</p>
      <Button onClick={onBack} variant="outline">
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Topics
      </Button>
    </div>
  );
}

/**
 * 정보 카드 컴포넌트
 */
function InfoCard({
  title,
  value,
  description,
  icon: Icon,
  variant = 'default',
}: {
  title: string;
  value: string | number;
  description?: string;
  icon: React.ComponentType<{ className?: string }>;
  variant?: 'default' | 'warning' | 'success';
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center gap-3">
          <div
            className={cn(
              'rounded-lg p-2',
              variant === 'warning' && 'bg-yellow-100 dark:bg-yellow-900/20',
              variant === 'success' && 'bg-green-100 dark:bg-green-900/20',
              variant === 'default' && 'bg-primary/10'
            )}
          >
            <Icon
              className={cn(
                'h-5 w-5',
                variant === 'warning' && 'text-yellow-600 dark:text-yellow-400',
                variant === 'success' && 'text-green-600 dark:text-green-400',
                variant === 'default' && 'text-primary'
              )}
            />
          </div>
          <div>
            <p className="text-2xl font-bold">{value}</p>
            <p className="text-sm text-muted-foreground">{title}</p>
            {description && (
              <p className="text-xs text-muted-foreground mt-1">{description}</p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 파티션 테이블 컴포넌트
 */
function PartitionTable({ partitions }: { partitions: PartitionInfo[] }) {
  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-24">Partition</TableHead>
            <TableHead>Leader</TableHead>
            <TableHead>Replicas</TableHead>
            <TableHead>ISR</TableHead>
            <TableHead className="text-right">Begin Offset</TableHead>
            <TableHead className="text-right">End Offset</TableHead>
            <TableHead className="text-right">Messages</TableHead>
            <TableHead className="w-24">Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {partitions.map((partition) => {
            const isUnderReplicated = partition.isr.length < partition.replicas.length;
            return (
              <TableRow key={partition.partition}>
                <TableCell className="font-mono">{partition.partition}</TableCell>
                <TableCell>
                  <Badge variant="outline" className="font-mono">
                    {partition.leader}
                  </Badge>
                </TableCell>
                <TableCell className="font-mono text-sm">
                  [{partition.replicas.join(', ')}]
                </TableCell>
                <TableCell className="font-mono text-sm">
                  [{partition.isr.join(', ')}]
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {formatNumber(partition.beginningOffset)}
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {formatNumber(partition.endOffset)}
                </TableCell>
                <TableCell className="text-right tabular-nums font-medium">
                  {formatNumber(partition.messageCount)}
                </TableCell>
                <TableCell>
                  {isUnderReplicated ? (
                    <Badge variant="warning" className="flex items-center gap-1 w-fit">
                      <AlertTriangle className="h-3 w-3" />
                      Under
                    </Badge>
                  ) : (
                    <Badge variant="healthy" className="flex items-center gap-1 w-fit">
                      <CheckCircle className="h-3 w-3" />
                      Healthy
                    </Badge>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

/**
 * 설정 테이블 컴포넌트
 */
function ConfigTable({ configs }: { configs: Record<string, string> }) {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const sortedConfigs = useMemo(() => {
    return Object.entries(configs).sort(([a], [b]) => a.localeCompare(b));
  }, [configs]);

  const handleCopy = async (key: string, value: string) => {
    await navigator.clipboard.writeText(value);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  /**
   * 설정값 포맷팅
   */
  const formatConfigValue = (key: string, value: string): string => {
    if (key.endsWith('.ms')) {
      const ms = parseInt(value, 10);
      if (!isNaN(ms)) return `${value} (${formatMs(ms)})`;
    }
    if (key.endsWith('.bytes')) {
      const bytes = parseInt(value, 10);
      if (!isNaN(bytes)) return `${value} (${formatBytes(bytes)})`;
    }
    return value;
  };

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-1/3">Configuration Key</TableHead>
            <TableHead>Value</TableHead>
            <TableHead className="w-16"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedConfigs.map(([key, value]) => (
            <TableRow key={key}>
              <TableCell className="font-mono text-sm">{key}</TableCell>
              <TableCell className="font-mono text-sm">
                {formatConfigValue(key, value)}
              </TableCell>
              <TableCell>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleCopy(key, value)}
                  className="h-8 w-8 p-0"
                >
                  {copiedKey === key ? (
                    <Check className="h-4 w-4 text-green-600" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

/**
 * 토픽 상세 정보 컴포넌트
 *
 * 토픽의 상세 정보, 파티션, 설정을 탭으로 구분하여 표시합니다.
 *
 * @param props - TopicDetail Props
 * @returns 토픽 상세 정보 컴포넌트
 *
 * @example
 * ```tsx
 * <TopicDetail clusterId="local" topicName="orders" />
 * ```
 */
export function TopicDetail({ clusterId, topicName, className }: TopicDetailProps) {
  const router = useRouter();
  const { topic, isLoading, error, refresh, isValidating } = useTopic(clusterId, topicName);

  /** 현재 활성 탭 */
  const [activeTab, setActiveTab] = useState('partitions');

  /**
   * 뒤로가기 핸들러
   */
  const handleBack = () => {
    router.push(`/clusters/${clusterId}/topics`);
  };

  // 로딩 상태
  if (isLoading) {
    return (
      <div className={cn('space-y-6', className)}>
        <Button variant="ghost" size="sm" onClick={handleBack}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Topics
        </Button>
        <LoadingSkeleton />
      </div>
    );
  }

  // 에러 상태
  if (error || !topic) {
    return (
      <div className={cn('space-y-6', className)}>
        <Button variant="ghost" size="sm" onClick={handleBack}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Topics
        </Button>
        <ErrorState
          message={error?.message || 'Topic not found'}
          onBack={handleBack}
        />
      </div>
    );
  }

  // 통계 계산
  const totalMessages = topic.partitions.reduce((sum, p) => sum + p.messageCount, 0);
  const underReplicatedCount = topic.partitions.filter(
    (p) => p.isr.length < p.replicas.length
  ).length;

  return (
    <div className={cn('space-y-6', className)}>
      {/* 뒤로가기 버튼 */}
      <Button variant="ghost" size="sm" onClick={handleBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Topics
      </Button>

      {/* 토픽 헤더 */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <div className="rounded-lg bg-primary/10 p-3">
            <FileText className="h-6 w-6 text-primary" />
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold font-mono">{topic.name}</h1>
              {topic.isInternal ? (
                <Badge variant="secondary">Internal</Badge>
              ) : (
                <Badge variant="outline">User Topic</Badge>
              )}
            </div>
            <p className="text-muted-foreground">
              {topic.partitionCount} partitions, {topic.replicationFactor}x replication
            </p>
          </div>
        </div>

        <Button
          variant="outline"
          onClick={() => refresh()}
          disabled={isValidating}
        >
          <RefreshCw
            className={cn('mr-2 h-4 w-4', isValidating && 'animate-spin')}
          />
          Refresh
        </Button>
      </div>

      {/* 통계 카드 */}
      <div className="grid gap-4 md:grid-cols-4">
        <InfoCard
          title="Partitions"
          value={topic.partitionCount}
          icon={Layers}
        />
        <InfoCard
          title="Replication Factor"
          value={topic.replicationFactor}
          icon={Server}
        />
        <InfoCard
          title="Total Messages"
          value={formatNumber(totalMessages)}
          icon={FileText}
        />
        <InfoCard
          title="Under-replicated"
          value={underReplicatedCount}
          icon={underReplicatedCount > 0 ? AlertTriangle : CheckCircle}
          variant={underReplicatedCount > 0 ? 'warning' : 'success'}
          description={
            underReplicatedCount > 0
              ? `${underReplicatedCount} partition(s) need attention`
              : 'All partitions healthy'
          }
        />
      </div>

      {/* 탭 영역 */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="partitions">
            <Layers className="mr-2 h-4 w-4" />
            Partitions ({topic.partitions.length})
          </TabsTrigger>
          <TabsTrigger value="configs">
            <Settings className="mr-2 h-4 w-4" />
            Configurations ({Object.keys(topic.configs).length})
          </TabsTrigger>
        </TabsList>

        {/* 파티션 탭 */}
        <TabsContent value="partitions" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Partition Details</CardTitle>
              <CardDescription>
                View partition leaders, replicas, and offset information
              </CardDescription>
            </CardHeader>
            <CardContent>
              <PartitionTable partitions={topic.partitions} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* 설정 탭 */}
        <TabsContent value="configs" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Topic Configurations</CardTitle>
              <CardDescription>
                Current topic configuration settings
              </CardDescription>
            </CardHeader>
            <CardContent>
              {Object.keys(topic.configs).length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
                  No configuration overrides. Using default broker settings.
                </p>
              ) : (
                <ConfigTable configs={topic.configs} />
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

export default TopicDetail;
