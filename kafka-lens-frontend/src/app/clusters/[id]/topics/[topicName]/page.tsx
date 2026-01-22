'use client';

/**
 * 토픽 상세 페이지
 *
 * 특정 토픽의 상세 정보를 표시합니다.
 * - 토픽 기본 정보
 * - 파티션 목록 및 상태
 * - 토픽 설정
 *
 * @module app/clusters/[id]/topics/[topicName]/page
 */
import { useParams } from 'next/navigation';
import { TopicDetail } from '@/components/topic';

/**
 * 토픽 상세 페이지 컴포넌트
 *
 * @returns 토픽 상세 페이지
 */
export default function TopicDetailPage() {
  const params = useParams();
  const clusterId = params.id as string;
  const topicName = decodeURIComponent(params.topicName as string);

  return <TopicDetail clusterId={clusterId} topicName={topicName} />;
}
