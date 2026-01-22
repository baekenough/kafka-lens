'use client';

import { usePathname } from 'next/navigation';
import Link from 'next/link';
import {
  FileText,
  Users,
  Server,
  MessageSquare,
  ChevronDown,
  Database,
  Plus,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui';
import { useState } from 'react';

/**
 * Sidebar 컴포넌트 Props
 */
interface SidebarProps {
  /** 사이드바 접힘 상태 */
  collapsed?: boolean;
}

/**
 * 네비게이션 아이템 타입
 */
interface NavItem {
  /** 아이템 이름 */
  name: string;
  /** 링크 경로 */
  href: string;
  /** 아이콘 컴포넌트 */
  icon: React.ComponentType<{ className?: string }>;
  /** 뱃지 텍스트 (선택적) */
  badge?: string | number;
}

/**
 * 네비게이션 메뉴 항목 정의
 */
const navItems: NavItem[] = [
  {
    name: 'Topics',
    href: '/topics',
    icon: FileText,
  },
  {
    name: 'Consumer Groups',
    href: '/consumer-groups',
    icon: Users,
  },
  {
    name: 'Brokers',
    href: '/brokers',
    icon: Server,
  },
  {
    name: 'Messages',
    href: '/messages',
    icon: MessageSquare,
  },
];

/**
 * 클러스터 정보 타입
 */
interface ClusterInfo {
  /** 클러스터 ID */
  id: string;
  /** 클러스터 이름 */
  name: string;
  /** 연결 상태 */
  status: 'connected' | 'disconnected' | 'connecting';
}

/**
 * 사이드바 컴포넌트
 *
 * 좌측에 고정되는 네비게이션 사이드바입니다.
 * - 네비게이션 링크: Topics, Consumer Groups, Brokers, Messages
 * - 활성 상태 하이라이팅: 현재 경로와 일치하는 메뉴 강조
 * - 클러스터 스위처: 여러 클러스터 간 전환 지원
 * - 접기/펼치기: 공간 효율적 사용
 *
 * @param props - SidebarProps
 * @returns 사이드바 컴포넌트
 *
 * @example
 * ```tsx
 * <Sidebar collapsed={false} />
 * ```
 */
export function Sidebar({ collapsed = false }: SidebarProps) {
  /** 현재 경로 */
  const pathname = usePathname();

  /** 클러스터 드롭다운 상태 */
  const [clusterMenuOpen, setClusterMenuOpen] = useState(false);

  /**
   * 임시 클러스터 목록
   * TODO: API에서 가져오도록 변경
   */
  const clusters: ClusterInfo[] = [
    { id: 'local-1', name: 'Local Development', status: 'connected' },
    { id: 'staging-1', name: 'Staging', status: 'disconnected' },
    { id: 'prod-1', name: 'Production', status: 'disconnected' },
  ];

  /** 현재 선택된 클러스터 */
  const [selectedCluster, setSelectedCluster] = useState<ClusterInfo>(
    clusters[0]
  );

  /**
   * 경로가 현재 활성 상태인지 확인
   *
   * @param href - 확인할 경로
   * @returns 활성 상태 여부
   */
  const isActive = (href: string) => {
    if (href === '/') return pathname === '/';
    return pathname.startsWith(href);
  };

  /**
   * 클러스터 상태에 따른 색상 반환
   *
   * @param status - 클러스터 연결 상태
   * @returns Tailwind 색상 클래스
   */
  const getStatusColor = (status: ClusterInfo['status']) => {
    switch (status) {
      case 'connected':
        return 'bg-green-500';
      case 'connecting':
        return 'bg-yellow-500 animate-pulse';
      case 'disconnected':
        return 'bg-gray-400';
      default:
        return 'bg-gray-400';
    }
  };

  return (
    <aside
      className={cn(
        'fixed left-0 top-14 bottom-0 z-40 border-r bg-background transition-all duration-300',
        collapsed ? 'w-16' : 'w-64'
      )}
    >
      <div className="flex h-full flex-col">
        {/* 클러스터 스위처 */}
        <div className={cn('border-b p-3', collapsed && 'p-2')}>
          {collapsed ? (
            /* 접힌 상태: 아이콘만 표시 */
            <Button
              variant="ghost"
              size="icon"
              className="w-full"
              aria-label="Select cluster"
            >
              <Database className="h-5 w-5" />
            </Button>
          ) : (
            /* 펼친 상태: 클러스터 선택 드롭다운 */
            <div className="relative">
              <Button
                variant="outline"
                className="w-full justify-between"
                onClick={() => setClusterMenuOpen(!clusterMenuOpen)}
                aria-expanded={clusterMenuOpen}
              >
                <div className="flex items-center gap-2 truncate">
                  <span
                    className={cn(
                      'h-2 w-2 rounded-full shrink-0',
                      getStatusColor(selectedCluster.status)
                    )}
                  />
                  <span className="truncate text-sm">
                    {selectedCluster.name}
                  </span>
                </div>
                <ChevronDown
                  className={cn(
                    'h-4 w-4 shrink-0 transition-transform',
                    clusterMenuOpen && 'rotate-180'
                  )}
                />
              </Button>

              {/* 클러스터 드롭다운 메뉴 */}
              {clusterMenuOpen && (
                <>
                  {/* 배경 오버레이 */}
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setClusterMenuOpen(false)}
                  />

                  {/* 드롭다운 메뉴 */}
                  <div className="absolute left-0 right-0 top-full mt-1 rounded-md border bg-popover p-1 shadow-md z-50">
                    {/* 클러스터 목록 */}
                    {clusters.map((cluster) => (
                      <button
                        key={cluster.id}
                        className={cn(
                          'flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent',
                          selectedCluster.id === cluster.id && 'bg-accent'
                        )}
                        onClick={() => {
                          setSelectedCluster(cluster);
                          setClusterMenuOpen(false);
                        }}
                      >
                        <span
                          className={cn(
                            'h-2 w-2 rounded-full',
                            getStatusColor(cluster.status)
                          )}
                        />
                        <span className="truncate">{cluster.name}</span>
                      </button>
                    ))}

                    <div className="my-1 h-px bg-border" />

                    {/* 클러스터 추가 버튼 */}
                    <button
                      className="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                      onClick={() => {
                        setClusterMenuOpen(false);
                        // TODO: 클러스터 추가 다이얼로그 열기
                      }}
                    >
                      <Plus className="h-4 w-4" />
                      <span>Add Cluster</span>
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>

        {/* 네비게이션 메뉴 */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          {navItems.map((item) => {
            const active = isActive(item.href);
            const Icon = item.icon;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  'hover:bg-accent hover:text-accent-foreground',
                  active
                    ? 'bg-accent text-accent-foreground'
                    : 'text-muted-foreground',
                  collapsed && 'justify-center px-2'
                )}
                title={collapsed ? item.name : undefined}
              >
                <Icon className="h-5 w-5 shrink-0" />
                {!collapsed && (
                  <>
                    <span className="flex-1">{item.name}</span>
                    {item.badge !== undefined && (
                      <span className="ml-auto text-xs bg-primary/10 text-primary px-2 py-0.5 rounded-full">
                        {item.badge}
                      </span>
                    )}
                  </>
                )}
              </Link>
            );
          })}
        </nav>

        {/* 하단 정보 영역 */}
        {!collapsed && (
          <div className="border-t p-3">
            <div className="text-xs text-muted-foreground">
              <p>Kafka Lens v0.1.0</p>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}

export default Sidebar;
