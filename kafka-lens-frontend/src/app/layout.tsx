import type { Metadata, Viewport } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { MainLayout } from '@/components/layout';

/**
 * Inter 폰트 설정
 * 가변 폰트로 성능 최적화
 */
const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
});

/**
 * 페이지 메타데이터
 *
 * Kafka Lens 애플리케이션의 기본 메타데이터를 정의합니다.
 * 각 페이지에서 필요시 오버라이드 가능합니다.
 */
export const metadata: Metadata = {
  title: {
    default: 'Kafka Lens',
    template: '%s | Kafka Lens',
  },
  description: 'Kafka 클러스터 모니터링 및 관리 도구',
  keywords: [
    'kafka',
    'monitoring',
    'cluster',
    'consumer',
    'topic',
    'broker',
    'message',
    'kafka-ui',
  ],
  authors: [{ name: 'Kafka Lens Team' }],
  creator: 'Kafka Lens',
  applicationName: 'Kafka Lens',
};

/**
 * 뷰포트 설정
 *
 * 반응형 디자인 및 모바일 최적화를 위한 뷰포트 설정입니다.
 */
export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
    { media: '(prefers-color-scheme: dark)', color: '#0a0a0a' },
  ],
};

/**
 * 루트 레이아웃 컴포넌트
 *
 * 모든 페이지에 공통 적용되는 최상위 레이아웃입니다.
 * - HTML/Body 태그 설정
 * - 폰트 적용
 * - 전역 프로바이더 래핑
 * - MainLayout 적용 (Header, Sidebar, Content)
 *
 * @param props - 자식 컴포넌트를 포함하는 props
 * @returns 루트 레이아웃 컴포넌트
 */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body
        className={`${inter.variable} font-sans antialiased min-h-screen bg-background`}
      >
        {/*
          TODO: 전역 프로바이더 추가 예정
          - ThemeProvider (다크모드)
          - QueryClientProvider (React Query)
          - ToastProvider (알림)
        */}
        <Providers>
          <MainLayout>{children}</MainLayout>
        </Providers>
      </body>
    </html>
  );
}

/**
 * 전역 프로바이더 래퍼 컴포넌트
 *
 * 현재는 단순 래퍼이지만, 추후 다음 프로바이더들을 추가할 예정입니다:
 * - ThemeProvider: 테마 (라이트/다크 모드) 관리
 * - QueryClientProvider: React Query 상태 관리
 * - AuthProvider: 인증 상태 관리
 *
 * @param props - 자식 컴포넌트
 * @returns 프로바이더 래퍼
 */
function Providers({ children }: { children: React.ReactNode }) {
  // TODO: 프로바이더 구현 후 래핑
  // return (
  //   <ThemeProvider>
  //     <QueryClientProvider client={queryClient}>
  //       <AuthProvider>
  //         {children}
  //       </AuthProvider>
  //     </QueryClientProvider>
  //   </ThemeProvider>
  // );

  return <>{children}</>;
}
