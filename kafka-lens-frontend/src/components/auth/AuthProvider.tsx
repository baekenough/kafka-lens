'use client';

/**
 * 인증 프로바이더 컴포넌트
 *
 * 애플리케이션 전역에서 인증 상태를 관리하는 Context Provider입니다.
 * - 인증 상태 전역 공유
 * - 로그인/로그아웃 함수 제공
 * - localStorage 기반 자격 증명 지속
 *
 * @module components/auth/AuthProvider
 */
import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  useMemo,
  type ReactNode,
} from 'react';
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
 * 인증 컨텍스트 값 타입
 */
export interface AuthContextValue {
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

/** 인증 컨텍스트 */
const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * AuthProvider Props
 */
interface AuthProviderProps {
  /** 자식 컴포넌트 */
  children: ReactNode;
}

/**
 * 인증 프로바이더 컴포넌트
 *
 * 애플리케이션 루트에서 감싸서 전역 인증 상태를 제공합니다.
 *
 * @param props - AuthProvider Props
 * @returns 인증 프로바이더
 *
 * @example
 * ```tsx
 * // app/layout.tsx
 * export default function RootLayout({ children }: { children: ReactNode }) {
 *   return (
 *     <html>
 *       <body>
 *         <AuthProvider>
 *           {children}
 *         </AuthProvider>
 *       </body>
 *     </html>
 *   );
 * }
 * ```
 */
export function AuthProvider({ children }: AuthProviderProps) {
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
   */
  const checkAuth = useCallback((): boolean => {
    return checkApiAuth();
  }, []);

  /** 컨텍스트 값 메모이제이션 */
  const contextValue = useMemo<AuthContextValue>(
    () => ({
      authState,
      login,
      logout,
      checkAuth,
    }),
    [authState, login, logout, checkAuth]
  );

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * 인증 컨텍스트 훅
 *
 * AuthProvider 내부에서만 사용 가능합니다.
 *
 * @returns 인증 컨텍스트 값
 * @throws AuthProvider 외부에서 호출 시 에러
 *
 * @example
 * ```tsx
 * function UserProfile() {
 *   const { authState, logout } = useAuthContext();
 *
 *   return (
 *     <div>
 *       <p>Welcome, {authState.username}</p>
 *       <button onClick={logout}>Logout</button>
 *     </div>
 *   );
 * }
 * ```
 */
export function useAuthContext(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }

  return context;
}

export default AuthProvider;
