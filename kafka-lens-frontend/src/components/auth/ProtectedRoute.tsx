'use client';

/**
 * 보호된 라우트 컴포넌트
 *
 * 인증이 필요한 페이지를 감싸서 미인증 사용자를 로그인 페이지로 리다이렉트합니다.
 *
 * @module components/auth/ProtectedRoute
 */
import { useEffect, type ReactNode } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { useAuthContext } from './AuthProvider';

/**
 * ProtectedRoute Props
 */
interface ProtectedRouteProps {
  /** 보호할 자식 컴포넌트 */
  children: ReactNode;
  /** 리다이렉트할 로그인 페이지 경로 (기본값: /login) */
  loginPath?: string;
  /** 로딩 중 표시할 컴포넌트 (선택사항) */
  loadingComponent?: ReactNode;
}

/**
 * 기본 로딩 컴포넌트
 */
function DefaultLoading() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Checking authentication...</p>
      </div>
    </div>
  );
}

/**
 * 보호된 라우트 컴포넌트
 *
 * 인증이 필요한 페이지에 사용하여 미인증 사용자를 로그인 페이지로 리다이렉트합니다.
 *
 * @param props - ProtectedRoute Props
 * @returns 인증된 경우 자식 컴포넌트, 미인증 경우 null (리다이렉트)
 *
 * @example
 * ```tsx
 * // 단일 페이지 보호
 * function DashboardPage() {
 *   return (
 *     <ProtectedRoute>
 *       <Dashboard />
 *     </ProtectedRoute>
 *   );
 * }
 *
 * // 커스텀 로딩 컴포넌트
 * function AdminPage() {
 *   return (
 *     <ProtectedRoute loadingComponent={<AdminLoadingSkeleton />}>
 *       <AdminDashboard />
 *     </ProtectedRoute>
 *   );
 * }
 *
 * // 레이아웃에서 사용
 * function ProtectedLayout({ children }: { children: ReactNode }) {
 *   return (
 *     <ProtectedRoute>
 *       <MainLayout>{children}</MainLayout>
 *     </ProtectedRoute>
 *   );
 * }
 * ```
 */
export function ProtectedRoute({
  children,
  loginPath = '/login',
  loadingComponent,
}: ProtectedRouteProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { authState } = useAuthContext();

  /**
   * 인증 상태에 따라 리다이렉트
   */
  useEffect(() => {
    // 로딩 중에는 아무 것도 하지 않음
    if (authState.isLoading) return;

    // 미인증 상태면 로그인 페이지로 리다이렉트
    if (!authState.isAuthenticated) {
      // 현재 경로를 returnUrl로 저장하여 로그인 후 돌아올 수 있게 함
      const returnUrl = encodeURIComponent(pathname);
      router.replace(`${loginPath}?returnUrl=${returnUrl}`);
    }
  }, [authState.isLoading, authState.isAuthenticated, router, loginPath, pathname]);

  // 로딩 중
  if (authState.isLoading) {
    return <>{loadingComponent || <DefaultLoading />}</>;
  }

  // 미인증 상태 (리다이렉트 중)
  if (!authState.isAuthenticated) {
    return <>{loadingComponent || <DefaultLoading />}</>;
  }

  // 인증됨 - 자식 컴포넌트 렌더링
  return <>{children}</>;
}

/**
 * 인증 상태에 따라 조건부 렌더링하는 컴포넌트
 *
 * 리다이렉트 없이 인증 상태에 따라 다른 컨텐츠를 표시합니다.
 *
 * @example
 * ```tsx
 * function Header() {
 *   return (
 *     <AuthGuard
 *       authenticated={<UserMenu />}
 *       unauthenticated={<LoginButton />}
 *     />
 *   );
 * }
 * ```
 */
export function AuthGuard({
  authenticated,
  unauthenticated,
  loading,
}: {
  /** 인증된 경우 렌더링할 컴포넌트 */
  authenticated: ReactNode;
  /** 미인증인 경우 렌더링할 컴포넌트 */
  unauthenticated: ReactNode;
  /** 로딩 중 렌더링할 컴포넌트 (선택사항) */
  loading?: ReactNode;
}) {
  const { authState } = useAuthContext();

  if (authState.isLoading) {
    return <>{loading || null}</>;
  }

  return <>{authState.isAuthenticated ? authenticated : unauthenticated}</>;
}

export default ProtectedRoute;
