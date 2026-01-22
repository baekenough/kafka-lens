'use client';

/**
 * 대시보드 메인 페이지
 *
 * 클러스터 목록과 빠른 통계 개요를 표시합니다.
 *
 * @module app/page
 */
import { useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Server, FileText, Users, Activity, TrendingUp } from 'lucide-react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { ClusterList } from '@/components/cluster';
import { useClusters } from '@/hooks/useClusters';
import type { Cluster } from '@/types';

/**
 * 통계 카드 컴포넌트
 */
interface StatCardProps {
  /** 카드 제목 */
  title: string;
  /** 메인 값 */
  value: string | number;
  /** 부가 설명 */
  description?: string;
  /** 아이콘 */
  icon: React.ReactNode;
  /** 트렌드 (증가/감소) */
  trend?: {
    value: number;
    isPositive: boolean;
  };
}

/**
 * 통계 카드 컴포넌트
 */
function StatCard({ title, value, description, icon, trend }: StatCardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <div className="text-muted-foreground">{icon}</div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground mt-1">{description}</p>
        )}
        {trend && (
          <div className="flex items-center text-xs mt-1">
            <TrendingUp
              className={`h-3 w-3 mr-1 ${
                trend.isPositive ? 'text-green-600' : 'text-red-600 rotate-180'
              }`}
            />
            <span
              className={trend.isPositive ? 'text-green-600' : 'text-red-600'}
            >
              {trend.isPositive ? '+' : '-'}
              {Math.abs(trend.value)}%
            </span>
            <span className="text-muted-foreground ml-1">from last week</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * 대시보드 메인 페이지 컴포넌트
 *
 * @returns 대시보드 페이지
 */
export default function DashboardPage() {
  const router = useRouter();
  const { clusters, isLoading } = useClusters();

  /** 선택된 클러스터 ID */
  const [selectedClusterId, setSelectedClusterId] = useState<string | null>(null);

  /**
   * 클러스터 선택 핸들러
   * 선택 후 상세 페이지로 이동
   */
  const handleSelectCluster = useCallback(
    (cluster: Cluster) => {
      setSelectedClusterId(cluster.id);
      // 클러스터 상세 페이지로 이동
      router.push(`/clusters/${cluster.id}`);
    },
    [router]
  );

  /**
   * 클러스터 추가 핸들러
   */
  const handleAddCluster = useCallback(() => {
    // TODO: 클러스터 추가 다이얼로그 구현
    console.log('Add cluster clicked');
  }, []);

  // 통계 계산
  const connectedClusters = clusters.filter((c) => c.status === 'connected').length;
  const totalBrokers = clusters.reduce(
    (sum, c) => sum + (c.brokers?.length || 0),
    0
  );
  const onlineBrokers = clusters.reduce(
    (sum, c) => sum + (c.brokers?.filter((b) => b.status === 'online').length || 0),
    0
  );

  return (
    <div className="space-y-6">
      {/* 페이지 헤더 */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Kafka 클러스터 현황 및 모니터링 개요
        </p>
      </div>

      {/* 빠른 통계 그리드 */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Connected Clusters"
          value={isLoading ? '-' : connectedClusters}
          description={`${clusters.length} total clusters`}
          icon={<Server className="h-4 w-4" />}
        />
        <StatCard
          title="Online Brokers"
          value={isLoading ? '-' : `${onlineBrokers}/${totalBrokers}`}
          description="Active broker instances"
          icon={<Activity className="h-4 w-4" />}
        />
        <StatCard
          title="Topics"
          value="-"
          description="Select a cluster to view"
          icon={<FileText className="h-4 w-4" />}
        />
        <StatCard
          title="Consumer Groups"
          value="-"
          description="Select a cluster to view"
          icon={<Users className="h-4 w-4" />}
        />
      </div>

      {/* 클러스터 목록 섹션 */}
      <div className="space-y-4">
        <ClusterList
          selectedClusterId={selectedClusterId || undefined}
          onSelect={handleSelectCluster}
          onAddCluster={handleAddCluster}
        />
      </div>
    </div>
  );
}
