import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Tailwind CSS 클래스 병합 유틸리티
 * clsx로 조건부 클래스를 처리하고, tailwind-merge로 충돌하는 클래스를 병합
 *
 * @param inputs - 병합할 클래스 값들
 * @returns 병합된 클래스 문자열
 *
 * @example
 * cn('px-2 py-1', condition && 'bg-red-500', 'px-4')
 * // => 'py-1 bg-red-500 px-4' (px-4가 px-2를 덮어씀)
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 숫자를 읽기 쉬운 형식으로 포맷팅
 * 예: 1000 -> 1,000, 1000000 -> 1M
 *
 * @param num - 포맷팅할 숫자
 * @param compact - true면 축약형 (1K, 1M 등)
 * @returns 포맷팅된 문자열
 */
export function formatNumber(num: number, compact = false): string {
  if (compact) {
    const formatter = new Intl.NumberFormat('en', {
      notation: 'compact',
      compactDisplay: 'short',
    });
    return formatter.format(num);
  }
  return new Intl.NumberFormat('en').format(num);
}

/**
 * 바이트를 읽기 쉬운 형식으로 변환
 *
 * @param bytes - 바이트 수
 * @param decimals - 소수점 자릿수
 * @returns 포맷팅된 문자열 (예: "1.5 GB")
 */
export function formatBytes(bytes: number, decimals = 2): string {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(decimals))} ${sizes[i]}`;
}

/**
 * 상대적 시간 포맷팅
 *
 * @param date - Date 객체 또는 타임스탬프
 * @returns 상대적 시간 문자열 (예: "5분 전")
 */
export function formatRelativeTime(date: Date | number): string {
  const now = Date.now();
  const timestamp = typeof date === 'number' ? date : date.getTime();
  const diff = now - timestamp;

  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}일 전`;
  if (hours > 0) return `${hours}시간 전`;
  if (minutes > 0) return `${minutes}분 전`;
  return '방금 전';
}
