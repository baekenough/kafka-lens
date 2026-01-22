'use client';

import { ReactNode, useState } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { cn } from '@/lib/utils';

/**
 * MainLayout 컴포넌트 Props
 */
interface MainLayoutProps {
  /** 레이아웃 내부에 렌더링될 자식 컴포넌트 */
  children: ReactNode;
}

/**
 * 메인 레이아웃 컴포넌트
 *
 * 애플리케이션의 기본 레이아웃 구조를 제공합니다.
 * - Header: 상단 고정 헤더 (로고, 클러스터 표시, 사용자 메뉴)
 * - Sidebar: 좌측 네비게이션 (접기/펼치기 지원)
 * - Content: 메인 콘텐츠 영역
 *
 * @param props - MainLayoutProps
 * @returns 메인 레이아웃 컴포넌트
 *
 * @example
 * ```tsx
 * <MainLayout>
 *   <TopicsPage />
 * </MainLayout>
 * ```
 */
export function MainLayout({ children }: MainLayoutProps) {
  /** 사이드바 접힘 상태 */
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  /**
   * 사이드바 토글 핸들러
   */
  const handleSidebarToggle = () => {
    setSidebarCollapsed((prev) => !prev);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 상단 헤더 - 고정 위치 */}
      <Header onMenuToggle={handleSidebarToggle} />

      {/* 메인 영역: 사이드바 + 콘텐츠 */}
      <div className="flex pt-14">
        {/* 좌측 사이드바 */}
        <Sidebar collapsed={sidebarCollapsed} />

        {/* 메인 콘텐츠 영역 */}
        <main
          className={cn(
            'flex-1 p-6 transition-all duration-300',
            sidebarCollapsed ? 'ml-16' : 'ml-64'
          )}
        >
          <div className="mx-auto max-w-7xl">{children}</div>
        </main>
      </div>
    </div>
  );
}

export default MainLayout;
