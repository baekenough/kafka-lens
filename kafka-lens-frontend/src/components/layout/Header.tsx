'use client';

import { Menu, User, ChevronDown, Database, Settings, LogOut } from 'lucide-react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/utils';
import { useState } from 'react';

/**
 * Header 컴포넌트 Props
 */
interface HeaderProps {
  /** 사이드바 메뉴 토글 핸들러 */
  onMenuToggle?: () => void;
}

/**
 * 현재 클러스터 정보 (임시 - 추후 전역 상태로 관리)
 */
interface CurrentCluster {
  /** 클러스터 ID */
  id: string;
  /** 클러스터 이름 */
  name: string;
  /** 연결 상태 */
  status: 'connected' | 'disconnected' | 'connecting';
}

/**
 * 헤더 컴포넌트
 *
 * 애플리케이션 상단에 고정되는 헤더입니다.
 * - 로고/타이틀: Kafka Lens 브랜딩
 * - 클러스터 인디케이터: 현재 연결된 클러스터 표시
 * - 사용자 메뉴: 인증 기능 추가 시 사용
 *
 * @param props - HeaderProps
 * @returns 헤더 컴포넌트
 *
 * @example
 * ```tsx
 * <Header onMenuToggle={() => setSidebarOpen(!sidebarOpen)} />
 * ```
 */
export function Header({ onMenuToggle }: HeaderProps) {
  /** 사용자 메뉴 드롭다운 상태 */
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  /**
   * 임시 클러스터 정보
   * TODO: 전역 상태 관리로 대체
   */
  const currentCluster: CurrentCluster | null = {
    id: 'local-1',
    name: 'Local Development',
    status: 'connected',
  };

  /**
   * 클러스터 상태에 따른 색상 반환
   *
   * @param status - 클러스터 연결 상태
   * @returns Tailwind 색상 클래스
   */
  const getStatusColor = (status: CurrentCluster['status']) => {
    switch (status) {
      case 'connected':
        return 'bg-green-500';
      case 'connecting':
        return 'bg-yellow-500 animate-pulse';
      case 'disconnected':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-14 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-full items-center justify-between px-4">
        {/* 좌측: 메뉴 버튼 + 로고 */}
        <div className="flex items-center gap-4">
          {/* 모바일 메뉴 토글 버튼 */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onMenuToggle}
            aria-label="Toggle menu"
            className="lg:hidden"
          >
            <Menu className="h-5 w-5" />
          </Button>

          {/* 데스크톱 메뉴 토글 버튼 */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onMenuToggle}
            aria-label="Toggle sidebar"
            className="hidden lg:flex"
          >
            <Menu className="h-5 w-5" />
          </Button>

          {/* 로고 및 타이틀 */}
          <div className="flex items-center gap-2">
            <Database className="h-6 w-6 text-primary" />
            <span className="text-lg font-semibold">Kafka Lens</span>
          </div>
        </div>

        {/* 중앙: 클러스터 인디케이터 */}
        <div className="hidden md:flex items-center">
          {currentCluster ? (
            <Button
              variant="outline"
              className="flex items-center gap-2 h-9 px-3"
            >
              {/* 연결 상태 표시등 */}
              <span
                className={cn(
                  'h-2 w-2 rounded-full',
                  getStatusColor(currentCluster.status)
                )}
                aria-label={`Cluster status: ${currentCluster.status}`}
              />
              {/* 클러스터 이름 */}
              <span className="text-sm font-medium">{currentCluster.name}</span>
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            </Button>
          ) : (
            <Button variant="outline" className="h-9 px-3">
              <span className="text-sm text-muted-foreground">
                No cluster connected
              </span>
            </Button>
          )}
        </div>

        {/* 우측: 사용자 메뉴 */}
        <div className="relative">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setUserMenuOpen(!userMenuOpen)}
            aria-label="User menu"
            aria-expanded={userMenuOpen}
          >
            <User className="h-5 w-5" />
          </Button>

          {/* 사용자 드롭다운 메뉴 */}
          {userMenuOpen && (
            <>
              {/* 배경 오버레이 (클릭 시 닫기) */}
              <div
                className="fixed inset-0 z-40"
                onClick={() => setUserMenuOpen(false)}
              />

              {/* 드롭다운 메뉴 */}
              <div className="absolute right-0 top-full mt-2 w-48 rounded-md border bg-popover p-1 shadow-md z-50">
                <button
                  className="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
                  onClick={() => {
                    setUserMenuOpen(false);
                    // TODO: 설정 페이지로 이동
                  }}
                >
                  <Settings className="h-4 w-4" />
                  <span>Settings</span>
                </button>
                <div className="my-1 h-px bg-border" />
                <button
                  className="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm text-destructive hover:bg-accent"
                  onClick={() => {
                    setUserMenuOpen(false);
                    // TODO: 로그아웃 처리
                  }}
                >
                  <LogOut className="h-4 w-4" />
                  <span>Sign out</span>
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
