/**
 * 유틸리티 함수 테스트
 */
import { cn, formatNumber, formatBytes, formatRelativeTime } from '@/lib/utils';

describe('cn', () => {
  it('단일 클래스를 반환한다', () => {
    expect(cn('px-2')).toBe('px-2');
  });

  it('여러 클래스를 병합한다', () => {
    expect(cn('px-2', 'py-1')).toBe('px-2 py-1');
  });

  it('조건부 클래스를 처리한다', () => {
    expect(cn('px-2', true && 'py-1')).toBe('px-2 py-1');
    expect(cn('px-2', false && 'py-1')).toBe('px-2');
  });

  it('충돌하는 Tailwind 클래스를 병합한다', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4');
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500');
  });
});

describe('formatNumber', () => {
  it('숫자를 콤마로 포맷팅한다', () => {
    expect(formatNumber(1000)).toBe('1,000');
    expect(formatNumber(1000000)).toBe('1,000,000');
  });

  it('compact 모드에서 축약형을 반환한다', () => {
    expect(formatNumber(1000, true)).toBe('1K');
    expect(formatNumber(1000000, true)).toBe('1M');
    expect(formatNumber(1500000, true)).toBe('1.5M');
  });

  it('작은 숫자는 그대로 반환한다', () => {
    expect(formatNumber(0)).toBe('0');
    expect(formatNumber(999)).toBe('999');
  });
});

describe('formatBytes', () => {
  it('0 바이트를 올바르게 처리한다', () => {
    expect(formatBytes(0)).toBe('0 Bytes');
  });

  it('바이트 단위를 올바르게 변환한다', () => {
    expect(formatBytes(1024)).toBe('1 KB');
    expect(formatBytes(1024 * 1024)).toBe('1 MB');
    expect(formatBytes(1024 * 1024 * 1024)).toBe('1 GB');
  });

  it('소수점을 올바르게 처리한다', () => {
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(1536, 1)).toBe('1.5 KB');
    expect(formatBytes(1536, 0)).toBe('2 KB');
  });
});

describe('formatRelativeTime', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2026-01-22T12:00:00Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('방금 전을 올바르게 표시한다', () => {
    const now = Date.now();
    expect(formatRelativeTime(now - 30 * 1000)).toBe('방금 전');
  });

  it('분 단위를 올바르게 표시한다', () => {
    const now = Date.now();
    expect(formatRelativeTime(now - 5 * 60 * 1000)).toBe('5분 전');
  });

  it('시간 단위를 올바르게 표시한다', () => {
    const now = Date.now();
    expect(formatRelativeTime(now - 2 * 60 * 60 * 1000)).toBe('2시간 전');
  });

  it('일 단위를 올바르게 표시한다', () => {
    const now = Date.now();
    expect(formatRelativeTime(now - 3 * 24 * 60 * 60 * 1000)).toBe('3일 전');
  });

  it('Date 객체를 처리한다', () => {
    const date = new Date('2026-01-22T11:00:00Z');
    expect(formatRelativeTime(date)).toBe('1시간 전');
  });
});
