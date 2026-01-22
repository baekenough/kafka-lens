'use client';

/**
 * 로그인 페이지
 *
 * Basic Auth 기반 로그인 페이지입니다.
 * - 사용자명/비밀번호 입력
 * - localStorage에 자격 증명 저장 (Base64 인코딩)
 * - 로그인 성공 시 메인 페이지로 리다이렉트
 *
 * @module app/login/page
 */
import { useState, useCallback, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { LogIn, Eye, EyeOff, AlertCircle, Loader2, Server } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  CardFooter,
} from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/hooks/useAuth';

/**
 * 로그인 폼 상태
 */
interface LoginFormState {
  /** 사용자명 */
  username: string;
  /** 비밀번호 */
  password: string;
}

/**
 * 로그인 페이지 컴포넌트
 *
 * @returns 로그인 페이지
 */
export default function LoginPage() {
  const router = useRouter();
  const { authState, login } = useAuth();

  /** 폼 상태 */
  const [formState, setFormState] = useState<LoginFormState>({
    username: '',
    password: '',
  });

  /** 비밀번호 표시 여부 */
  const [showPassword, setShowPassword] = useState(false);

  /** 로그인 중 상태 */
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  /** 에러 메시지 */
  const [error, setError] = useState<string | null>(null);

  /**
   * 이미 로그인된 경우 메인 페이지로 리다이렉트
   */
  useEffect(() => {
    if (!authState.isLoading && authState.isAuthenticated) {
      router.replace('/');
    }
  }, [authState.isLoading, authState.isAuthenticated, router]);

  /**
   * 폼 값 변경 핸들러
   */
  const handleInputChange = useCallback(
    (field: keyof LoginFormState) => (e: React.ChangeEvent<HTMLInputElement>) => {
      setFormState((prev) => ({ ...prev, [field]: e.target.value }));
      setError(null);
    },
    []
  );

  /**
   * 비밀번호 표시 토글
   */
  const togglePasswordVisibility = useCallback(() => {
    setShowPassword((prev) => !prev);
  }, []);

  /**
   * 로그인 제출 핸들러
   */
  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);

      // 유효성 검사
      if (!formState.username.trim()) {
        setError('Please enter your username');
        return;
      }

      if (!formState.password) {
        setError('Please enter your password');
        return;
      }

      setIsLoggingIn(true);

      try {
        const success = await login({
          username: formState.username.trim(),
          password: formState.password,
        });

        if (success) {
          router.push('/');
        } else {
          setError('Login failed. Please check your credentials.');
        }
      } catch {
        setError('An unexpected error occurred. Please try again.');
      } finally {
        setIsLoggingIn(false);
      }
    },
    [formState, login, router]
  );

  // 인증 상태 로딩 중
  if (authState.isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-muted/30">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
            <Server className="h-8 w-8 text-primary" />
          </div>
          <CardTitle className="text-2xl">Kafka Lens</CardTitle>
          <CardDescription>
            Sign in to access your Kafka clusters
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            {/* 에러 메시지 */}
            {error && (
              <div className="flex items-center gap-2 p-3 rounded-md bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                <span className="text-sm">{error}</span>
              </div>
            )}

            {/* 사용자명 입력 */}
            <div className="space-y-2">
              <Label htmlFor="username">Username</Label>
              <input
                id="username"
                type="text"
                value={formState.username}
                onChange={handleInputChange('username')}
                placeholder="Enter your username"
                autoComplete="username"
                autoFocus
                disabled={isLoggingIn}
                className="w-full px-3 py-2 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
              />
            </div>

            {/* 비밀번호 입력 */}
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formState.password}
                  onChange={handleInputChange('password')}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  disabled={isLoggingIn}
                  className="w-full px-3 py-2 pr-10 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                />
                <button
                  type="button"
                  onClick={togglePasswordVisibility}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>
          </CardContent>

          <CardFooter>
            <Button
              type="submit"
              className="w-full"
              disabled={isLoggingIn}
            >
              {isLoggingIn ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Signing in...
                </>
              ) : (
                <>
                  <LogIn className="mr-2 h-4 w-4" />
                  Sign In
                </>
              )}
            </Button>
          </CardFooter>
        </form>

        {/* 안내 메시지 */}
        <div className="px-6 pb-6 text-center">
          <p className="text-xs text-muted-foreground">
            Enter the credentials configured for your Kafka Lens backend.
          </p>
        </div>
      </Card>
    </div>
  );
}
