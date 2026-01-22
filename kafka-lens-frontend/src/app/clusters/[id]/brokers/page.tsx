'use client';

/**
 * 브로커 목록 페이지
 *
 * 클러스터의 모든 브로커를 테이블 형태로 표시합니다.
 * - 브로커 ID, 호스트, 포트, 상태 표시
 * - 컨트롤러 브로커 하이라이트
 *
 * @module app/clusters/[id]/brokers/page
 */
import { useParams, useRouter } from 'next/navigation';
import { useCallback } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { BrokerList } from '@/components/broker';

/**
 * 브로커 목록 페이지 컴포넌트
 *
 * @returns 브로커 목록 페이지
 */
export default function BrokersPage() {
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
   * 브로커 클릭 핸들러
   * 현재는 상세 페이지가 없으므로 콘솔 로그만 출력
   */
  const handleBrokerClick = useCallback((brokerId: number) => {
    // 브로커 상세 페이지가 구현되면 라우팅 추가
    console.log(`Broker ${brokerId} clicked`);
  }, []);

  return (
    <div className="space-y-6">
      {/* 뒤로가기 버튼 */}
      <Button variant="ghost" size="sm" onClick={handleBack}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Cluster
      </Button>

      {/* 브로커 목록 */}
      <BrokerList clusterId={clusterId} onBrokerClick={handleBrokerClick} />
    </div>
  );
}
