/**
 * 인증 관련 커스텀 훅
 *
 * Basic Auth 기반 인증 상태 관리 훅
 * @module hooks/useAuth
 */
import { useState, useCallback, useEffect } from 'react';
import { setCredentials as setApiCredentials, isAuthenticated as checkApiAuth } from '@/lib/api';

/**
 * 인증 자격 증명 타입
 */
export interface AuthCredentials {
  /** 사용자명 */
  username: string;
  /** 비밀번호 */
  password: string;
}

/**
 * 인증 상태 타입
 */
export interface AuthState {
  /** 인증 여부 */
  isAuthenticated: boolean;
  /** 현재 사용자명 */
  username: string | null;
  /** 로딩 상태 (초기화 중) */
  isLoading: boolean;
}

/**
 * useAuth 훅 반환 타입
 */
export interface UseAuthReturn {
  /** 인증 상태 */
  authState: AuthState;
  /** 로그인 함수 */
  login: (credentials: AuthCredentials) => Promise<boolean>;
  /** 로그아웃 함수 */
  logout: () => void;
  /** 인증 확인 함수 */
  checkAuth: () => boolean;
}

/** localStorage 키 */
const AUTH_STORAGE_KEY = 'kafka-lens-auth';

/**
 * Base64 인코딩 함수
 */
function encodeCredentials(credentials: AuthCredentials): string {
  const jsonStr = JSON.stringify(credentials);
  return btoa(jsonStr);
}

/**
 * Base64 디코딩 함수
 */
function decodeCredentials(encoded: string): AuthCredentials | null {
  try {
    const jsonStr = atob(encoded);
    return JSON.parse(jsonStr) as AuthCredentials;
  } catch {
    return null;
  }
}

/**
 * localStorage에서 인증 정보 로드
 */
function loadStoredCredentials(): AuthCredentials | null {
  if (typeof window === 'undefined') return null;

  try {
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!stored) return null;
    return decodeCredentials(stored);
  } catch {
    return null;
  }
}

/**
 * localStorage에 인증 정보 저장
 */
function saveCredentials(credentials: AuthCredentials): void {
  if (typeof window === 'undefined') return;

  try {
    const encoded = encodeCredentials(credentials);
    localStorage.setItem(AUTH_STORAGE_KEY, encoded);
  } catch {
    console.error('Failed to save credentials to localStorage');
  }
}

/**
 * localStorage에서 인증 정보 삭제
 */
function clearStoredCredentials(): void {
  if (typeof window === 'undefined') return;

  try {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  } catch {
    console.error('Failed to clear credentials from localStorage');
  }
}

/**
 * 인증 관리 훅
 *
 * Basic Auth 기반 인증 상태를 관리하고 localStorage에 자격 증명을 저장합니다.
 * 보안 주의: 실제 프로덕션에서는 더 안전한 인증 방식 사용을 권장합니다.
 *
 * @returns 인증 상태 및 함수
 *
 * @example
 * ```tsx
 * function LoginPage() {
 *   const { authState, login, logout } = useAuth();
 *
 *   const handleLogin = async (e: FormEvent) => {
 *     e.preventDefault();
 *     const success = await login({ username, password });
 *     if (success) {
 *       router.push('/');
 *     }
 *   };
 *
 *   return (
 *     <form onSubmit={handleLogin}>
 *       <input value={username} onChange={(e) => setUsername(e.target.value)} />
 *       <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
 *       <button type="submit">Login</button>
 *     </form>
 *   );
 * }
 * ```
 */
export function useAuth(): UseAuthReturn {
  /** 인증 상태 */
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    username: null,
    isLoading: true,
  });

  /**
   * 초기 인증 상태 로드
   */
  useEffect(() => {
    const stored = loadStoredCredentials();
    if (stored) {
      // API 클라이언트에 자격 증명 설정
      setApiCredentials(stored);
      setAuthState({
        isAuthenticated: true,
        username: stored.username,
        isLoading: false,
      });
    } else {
      setAuthState((prev) => ({ ...prev, isLoading: false }));
    }
  }, []);

  /**
   * 로그인 함수
   *
   * 자격 증명을 검증하고 저장합니다.
   * 현재는 서버 검증 없이 저장만 수행합니다.
   * 실제 프로덕션에서는 서버에서 자격 증명 검증 필요.
   *
   * @param credentials - 로그인 자격 증명
   * @returns 로그인 성공 여부
   */
  const login = useCallback(async (credentials: AuthCredentials): Promise<boolean> => {
    try {
      // 유효성 검사
      if (!credentials.username || !credentials.password) {
        return false;
      }

      // API 클라이언트에 자격 증명 설정
      setApiCredentials(credentials);

      // localStorage에 저장
      saveCredentials(credentials);

      // 상태 업데이트
      setAuthState({
        isAuthenticated: true,
        username: credentials.username,
        isLoading: false,
      });

      return true;
    } catch {
      return false;
    }
  }, []);

  /**
   * 로그아웃 함수
   *
   * 자격 증명을 제거하고 인증 상태를 초기화합니다.
   */
  const logout = useCallback(() => {
    // API 클라이언트에서 자격 증명 제거
    setApiCredentials(null);

    // localStorage에서 제거
    clearStoredCredentials();

    // 상태 초기화
    setAuthState({
      isAuthenticated: false,
      username: null,
      isLoading: false,
    });
  }, []);

  /**
   * 인증 상태 확인 함수
   *
   * @returns 현재 인증 상태
   */
  const checkAuth = useCallback((): boolean => {
    return checkApiAuth();
  }, []);

  return {
    authState,
    login,
    logout,
    checkAuth,
  };
}

export default useAuth;
