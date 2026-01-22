'use client';

/**
 * 토픽 목록 페이지
 *
 * 클러스터의 모든 토픽을 검색 가능한 테이블로 표시합니다.
 * - 토픽 이름 검색
 * - 내부 토픽 포함/제외 토글
 * - 토픽 상세 페이지로 이동
 *
 * @module app/clusters/[id]/topics/page
 */
import { useParams, useRouter } from 'next/navigation';
import { useCallback } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TopicList } from '@/components/topic';

/**
 * 토픽 목록 페이지 컴포넌트
 *
 * @returns 토픽 목록 페이지
 */
export default function TopicsPage() {
  const params = useParams();
  const router = useRouter();
  const clusterId = params.id as string;

  /**
   * 뒤로가기 핸들러
   */
  const handleBack = useCallback(() => {
    router.push(`/clusters/${clusterId}`);
  }, [router, clusterId]);

  /**
   * 토픽 클릭 핸들러
   */
  const handleTopicClick = useCallback(
    (topicName: string) => {
      router.push(`/clusters/${clusterId}/topics/${encodeURIComponent(topicName)}`);
    },
    [router, clusterId]
  );

  return (
    <div className="space-y-6">
      {/* 뒤로가기 버튼 */}
      <Button variant="ghost" size="sm" onClick={handleBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Cluster
      </Button>

      {/* 토픽 목록 */}
      <TopicList clusterId={clusterId} onTopicClick={handleTopicClick} />
    </div>
  );
}
