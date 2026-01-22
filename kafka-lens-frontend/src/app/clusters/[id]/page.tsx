'use client';

/**
 * 클러스터 상세 페이지
 *
 * 선택된 클러스터의 상세 정보와 리소스 탐색 탭을 제공합니다.
 * - 클러스터 기본 정보
 * - 연결 테스트 버튼
 * - Topics, Consumer Groups, Brokers, Messages 탭 네비게이션
 *
 * @module app/clusters/[id]/page
 */
import { useState, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Server,
  FileText,
  Users,
  MessageSquare,
  ArrowLeft,
  Zap,
  Loader2,
  CheckCircle,
  AlertCircle,
  Clock,
  Activity,
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
import { useCluster, testConnection, type ConnectionTestResult } from '@/hooks/useClusters';
import { TopicList } from '@/components/topic';
import { cn } from '@/lib/utils';
import type { ClusterStatus } from '@/types';

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
 * 날짜 포맷팅 함수
 */
function formatDate(dateString: string | undefined): string {
  if (!dateString) return 'Never';

  try {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return 'Invalid date';
  }
}

/**
 * 정보 아이템 컴포넌트
 */
function InfoItem({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="flex items-start gap-3">
      {Icon && (
        <div className="mt-0.5">
          <Icon className="h-4 w-4 text-muted-foreground" />
        </div>
      )}
      <div className="min-w-0">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="text-sm font-medium break-all">{value}</p>
      </div>
    </div>
  );
}

/**
 * 빈 탭 콘텐츠 컴포넌트
 */
function EmptyTabContent({
  title,
  description,
  icon: Icon,
}: {
  title: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="rounded-full bg-muted p-3 mb-4">
        <Icon className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">{title}</h3>
      <p className="text-muted-foreground text-center max-w-md">{description}</p>
    </div>
  );
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
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="h-5 w-24 rounded bg-muted" />
          </CardHeader>
          <CardContent className="space-y-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="space-y-2">
                <div className="h-3 w-16 rounded bg-muted" />
                <div className="h-4 w-full rounded bg-muted" />
              </div>
            ))}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <div className="h-5 w-24 rounded bg-muted" />
          </CardHeader>
          <CardContent className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex justify-between">
                <div className="h-4 w-20 rounded bg-muted" />
                <div className="h-4 w-12 rounded bg-muted" />
              </div>
            ))}
          </CardContent>
        </Card>
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
      <h3 className="text-lg font-semibold mb-2">Failed to load cluster</h3>
      <p className="text-muted-foreground text-center mb-4 max-w-md">{message}</p>
      <Button onClick={onBack} variant="outline">
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Dashboard
      </Button>
    </div>
  );
}

/**
 * 클러스터 상세 페이지 컴포넌트
 *
 * @returns 클러스터 상세 페이지
 */
export default function ClusterDetailPage() {
  const params = useParams();
  const router = useRouter();
  const clusterId = params.id as string;

  const { cluster, isLoading, error, refresh } = useCluster(clusterId);

  /** 연결 테스트 상태 */
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<ConnectionTestResult | null>(null);

  /** 현재 활성 탭 */
  const [activeTab, setActiveTab] = useState('overview');

  /**
   * 뒤로가기 핸들러
   */
  const handleBack = useCallback(() => {
    router.push('/');
  }, [router]);

  /**
   * 연결 테스트 핸들러
   */
  const handleTestConnection = useCallback(async () => {
    setIsTesting(true);
    setTestResult(null);

    try {
      const result = await testConnection(clusterId);
      setTestResult(result);
      // 테스트 후 클러스터 데이터 새로고침
      refresh();
    } catch (err) {
      setTestResult({
        connected: false,
        error: err instanceof Error ? err.message : 'Test failed',
      });
    } finally {
      setIsTesting(false);
    }
  }, [clusterId, refresh]);

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={handleBack}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back
        </Button>
        <LoadingSkeleton />
      </div>
    );
  }

  // 에러 상태
  if (error || !cluster) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={handleBack}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back
        </Button>
        <ErrorState
          message={error?.message || 'Cluster not found'}
          onBack={handleBack}
        />
      </div>
    );
  }

  // 브로커 통계
  const totalBrokers = cluster.brokers?.length || 0;
  const onlineBrokers = cluster.brokers?.filter((b) => b.status === 'online').length || 0;
  const controllerBroker = cluster.brokers?.find((b) => b.isController);

  return (
    <div className="space-y-6">
      {/* 뒤로가기 버튼 */}
      <Button variant="ghost" size="sm" onClick={handleBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Dashboard
      </Button>

      {/* 클러스터 헤더 */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <div className="rounded-lg bg-primary/10 p-3">
            <Server className="h-6 w-6 text-primary" />
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold">{cluster.name}</h1>
              <Badge variant={statusVariantMap[cluster.status]}>
                {cluster.status}
              </Badge>
            </div>
            <p className="text-muted-foreground">
              {cluster.bootstrapServers.join(', ')}
            </p>
          </div>
        </div>

        <Button
          onClick={handleTestConnection}
          disabled={isTesting}
          variant="outline"
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
      </div>

      {/* 연결 테스트 결과 */}
      {testResult && (
        <div
          className={cn(
            'flex items-center gap-3 p-4 rounded-lg',
            testResult.connected
              ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400'
              : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'
          )}
        >
          {testResult.connected ? (
            <>
              <CheckCircle className="h-5 w-5" />
              <span>
                Connection successful
                {testResult.latency && ` - Response time: ${testResult.latency}ms`}
              </span>
            </>
          ) : (
            <>
              <AlertCircle className="h-5 w-5" />
              <span>{testResult.error || 'Connection failed'}</span>
            </>
          )}
        </div>
      )}

      {/* 탭 네비게이션 */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="w-full justify-start">
          <TabsTrigger value="overview">
            <Activity className="mr-2 h-4 w-4" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="topics">
            <FileText className="mr-2 h-4 w-4" />
            Topics
          </TabsTrigger>
          <TabsTrigger value="consumers">
            <Users className="mr-2 h-4 w-4" />
            Consumer Groups
          </TabsTrigger>
          <TabsTrigger value="brokers">
            <Server className="mr-2 h-4 w-4" />
            Brokers
          </TabsTrigger>
          <TabsTrigger value="messages">
            <MessageSquare className="mr-2 h-4 w-4" />
            Messages
          </TabsTrigger>
        </TabsList>

        {/* Overview 탭 */}
        <TabsContent value="overview" className="mt-6">
          <div className="grid gap-6 md:grid-cols-2">
            {/* 클러스터 정보 카드 */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Cluster Information</CardTitle>
                <CardDescription>Basic cluster configuration</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <InfoItem
                  label="Cluster ID"
                  value={cluster.id}
                  icon={Server}
                />
                <InfoItem
                  label="Bootstrap Servers"
                  value={cluster.bootstrapServers.join(', ')}
                  icon={Server}
                />
                <InfoItem
                  label="Created"
                  value={formatDate(cluster.createdAt)}
                  icon={Clock}
                />
                <InfoItem
                  label="Last Connected"
                  value={formatDate(cluster.lastConnectedAt)}
                  icon={Clock}
                />
                {cluster.auth && (
                  <InfoItem
                    label="Authentication"
                    value={cluster.auth.type.toUpperCase()}
                    icon={Activity}
                  />
                )}
              </CardContent>
            </Card>

            {/* 브로커 상태 카드 */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Broker Status</CardTitle>
                <CardDescription>
                  {onlineBrokers}/{totalBrokers} brokers online
                </CardDescription>
              </CardHeader>
              <CardContent>
                {totalBrokers === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No broker information available. Connect to the cluster to
                    fetch broker data.
                  </p>
                ) : (
                  <div className="space-y-3">
                    {cluster.brokers?.map((broker) => (
                      <div
                        key={broker.id}
                        className="flex items-center justify-between py-2 border-b last:border-0"
                      >
                        <div className="flex items-center gap-2">
                          <div
                            className={cn(
                              'h-2 w-2 rounded-full',
                              broker.status === 'online'
                                ? 'bg-green-500'
                                : broker.status === 'offline'
                                  ? 'bg-red-500'
                                  : 'bg-gray-400'
                            )}
                          />
                          <span className="font-mono text-sm">
                            Broker {broker.id}
                          </span>
                          {broker.isController && (
                            <Badge variant="secondary" className="text-xs">
                              Controller
                            </Badge>
                          )}
                        </div>
                        <span className="text-sm text-muted-foreground">
                          {broker.host}:{broker.port}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Topics 탭 */}
        <TabsContent value="topics" className="mt-6">
          <TopicList
            clusterId={clusterId}
            onTopicClick={(topicName) =>
              router.push(`/clusters/${clusterId}/topics/${encodeURIComponent(topicName)}`)
            }
          />
        </TabsContent>

        {/* Consumer Groups 탭 */}
        <TabsContent value="consumers" className="mt-6">
          <EmptyTabContent
            title="Consumer Groups"
            description="Monitor consumer groups, view lag metrics, and manage offsets."
            icon={Users}
          />
        </TabsContent>

        {/* Brokers 탭 */}
        <TabsContent value="brokers" className="mt-6">
          <EmptyTabContent
            title="Brokers"
            description="View broker details, configurations, and performance metrics."
            icon={Server}
          />
        </TabsContent>

        {/* Messages 탭 */}
        <TabsContent value="messages" className="mt-6">
          <EmptyTabContent
            title="Messages"
            description="Browse and search messages. Publish new messages to topics."
            icon={MessageSquare}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
