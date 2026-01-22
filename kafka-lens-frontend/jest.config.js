const nextJest = require('next/jest');

/**
 * Next.js Jest 설정 생성기
 * Next.js 설정을 자동으로 로드
 */
const createJestConfig = nextJest({
  // Next.js 앱 경로
  dir: './',
});

/**
 * Jest 커스텀 설정
 * @type {import('jest').Config}
 */
const config = {
  // 커버리지 수집 설정
  coverageProvider: 'v8',

  // 테스트 환경
  testEnvironment: 'jsdom',

  // 테스트 셋업 파일
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],

  // 모듈 별칭 매핑
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },

  // 테스트 파일 패턴
  testMatch: [
    '<rootDir>/tests/**/*.test.{ts,tsx}',
    '<rootDir>/src/**/*.test.{ts,tsx}',
  ],

  // 커버리지 수집 대상
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/**/index.ts',
    '!src/app/layout.tsx',
  ],

  // 커버리지 임계값
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70,
    },
  },
};

module.exports = createJestConfig(config);
