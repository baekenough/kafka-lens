'use client';

/**
 * Consumer Groups 페이지
 *
 * 클러스터의 컨슈머 그룹 목록과 상세 정보를 표시하는 페이지입니다.
 * - 좌측: 컨슈머 그룹 목록
 * - 우측: 선택된 그룹의 상세 정보
 *
 * @module app/clusters/[id]/consumers/page
 */
import { useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { MainLayout } from '@/components/layout';
import { ConsumerGroupList, ConsumerGroupDetail } from '@/components/consumer';
import { useCluster } from '@/hooks';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Users } from 'lucide-react';
import type { ConsumerGroup } from '@/types';

/**
 * 그룹 미선택 상태 안내 컴포넌트
 */
function NoGroupSelected() {
  return (
    <Card className="h-full flex items-center justify-center">
      <CardContent className="text-center py-12">
        <div className="rounded-full bg-muted p-4 w-16 h-16 mx-auto mb-4 flex items-center justify-center">
          <Users className="h-8 w-8 text-muted-foreground" />
        </div>
        <CardTitle className="mb-2">Select a Consumer Group</CardTitle>
        <CardDescription className="max-w-sm">
          Click on a consumer group from the list to view its details, members, and partition
          assignments.
        </CardDescription>
      </CardContent>
    </Card>
  );
}

/**
 * Consumer Groups 페이지 컴포넌트
 *
 * 클러스터의 컨슈머 그룹을 관리하는 메인 페이지입니다.
 * Master-Detail 레이아웃으로 목록과 상세 정보를 동시에 표시합니다.
 *
 * @returns Consumer Groups 페이지
 */
export default function ConsumerGroupsPage() {
  const params = useParams();
  const clusterId = params.id as string;

  // 클러스터 정보 조회
  const { cluster, isLoading: isClusterLoading } = useCluster(clusterId);

  // 선택된 컨슈머 그룹 상태
  const [selectedGroup, setSelectedGroup] = useState<ConsumerGroup | null>(null);

  /**
   * 그룹 선택 핸들러
   */
  const handleSelectGroup = useCallback((group: ConsumerGroup) => {
    setSelectedGroup(group);
  }, []);

  /**
   * 상세 뷰에서 뒤로 가기 핸들러 (모바일용)
   */
  const handleBack = useCallback(() => {
    setSelectedGroup(null);
  }, []);

  // 페이지 제목
  const pageTitle = cluster ? `Consumer Groups - ${cluster.name}` : 'Consumer Groups';

  return (
    <MainLayout>
      <div className="flex flex-col h-full">
        {/* 페이지 헤더 */}
        <div className="border-b px-6 py-4">
          <h1 className="text-2xl font-bold">{pageTitle}</h1>
          {cluster && (
            <p className="text-sm text-muted-foreground mt-1">
              Manage and monitor consumer groups in {cluster.name}
            </p>
          )}
        </div>

        {/* 메인 콘텐츠 영역 (Master-Detail Layout) */}
        <div className="flex-1 flex overflow-hidden">
          {/* 좌측: 컨슈머 그룹 목록 */}
          <div
            className={`w-full md:w-2/5 lg:w-1/3 border-r overflow-y-auto p-4 ${
              selectedGroup ? 'hidden md:block' : ''
            }`}
          >
            <ConsumerGroupList
              clusterId={clusterId}
              onSelect={handleSelectGroup}
              selectedGroupId={selectedGroup?.groupId}
            />
          </div>

          {/* 우측: 상세 정보 */}
          <div
            className={`flex-1 overflow-y-auto p-6 ${
              selectedGroup ? '' : 'hidden md:block'
            }`}
          >
            {selectedGroup ? (
              <ConsumerGroupDetail
                clusterId={clusterId}
                groupId={selectedGroup.groupId}
                onBack={handleBack}
              />
            ) : (
              <NoGroupSelected />
            )}
          </div>
        </div>
      </div>
    </MainLayout>
  );
}
