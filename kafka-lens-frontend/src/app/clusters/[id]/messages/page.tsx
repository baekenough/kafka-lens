'use client';

/**
 * 메시지 페이지
 *
 * Kafka 토픽의 메시지를 검색하고 조회하는 페이지입니다.
 * - 토픽 선택 및 메시지 검색
 * - 메시지 목록 표시
 * - 메시지 상세 정보 모달
 *
 * @module app/clusters/[id]/messages/page
 */
import { useState, useCallback } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { MessageViewer, MessageDetail } from '@/components/message';
import type { Message } from '@/types';

/**
 * 메시지 페이지 컴포넌트
 *
 * @returns 메시지 페이지
 */
export default function MessagesPage() {
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const clusterId = params.id as string;

  /** URL에서 초기 토픽 가져오기 */
  const initialTopic = searchParams.get('topic') || '';

  /** 선택된 메시지 상태 */
  const [selectedMessage, setSelectedMessage] = useState<Message | null>(null);

  /**
   * 뒤로가기 핸들러
   */
  const handleBack = useCallback(() => {
    router.push(`/clusters/${clusterId}`);
  }, [router, clusterId]);

  /**
   * 메시지 클릭 핸들러
   */
  const handleMessageClick = useCallback((message: Message) => {
    setSelectedMessage(message);
  }, []);

  /**
   * 모달 닫기 핸들러
   */
  const handleCloseModal = useCallback(() => {
    setSelectedMessage(null);
  }, []);

  return (
    <div className="space-y-6">
      {/* 뒤로가기 버튼 */}
      <Button variant="ghost" size="sm" onClick={handleBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Cluster
      </Button>

      {/* 메시지 뷰어 */}
      <MessageViewer
        clusterId={clusterId}
        initialTopic={initialTopic}
        onMessageClick={handleMessageClick}
      />

      {/* 메시지 상세 모달 */}
      <MessageDetail
        message={selectedMessage}
        open={!!selectedMessage}
        onClose={handleCloseModal}
      />
    </div>
  );
}
