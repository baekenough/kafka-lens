<!--
Sync Impact Report
==================
Version change: N/A → 1.0.0
Added sections: All (initial creation)
Removed sections: None
Templates requiring updates: N/A (initial setup)
Follow-up TODOs: None
-->

# kafka-lens Constitution

## Core Principles

### I. Maven Central Only

모든 의존성은 Maven Central에 등록된 라이브러리만 사용한다.
- 보안심의 통과 요건으로 외부 도구(AKHQ, Grafana 등) 사용 불가
- Spring Boot, kafka-clients, spring-kafka 등 공식 라이브러리 활용
- 사내 Nexus 미러링 가능한 라이브러리만 허용

### II. Test-First Development (NON-NEGOTIABLE)

TDD 방식으로 개발한다.
- 테스트 코드 → 실패 확인 → 구현 → 통과 확인 순서 준수
- 단위 테스트: JUnit 5 + Mockito (백엔드), Jest/Vitest (프론트엔드)
- 통합 테스트: TestContainers를 활용한 Kafka 환경 테스트
- E2E 테스트: Playwright (프론트엔드 흐름)

### III. Guardrails First

입력 검증과 에러 처리를 최우선으로 한다.
- 모든 API 입력에 Bean Validation (@Valid) 적용
- 클러스터 연결 타임아웃 설정 (기본 5초)
- 메시지 조회 limit 제한 (최대 1000건)
- 명확한 에러 메시지와 에러 코드 체계

### IV. Korean Documentation

문서, 주석, docstring은 한글로 작성한다.
- 코드 주석: 한글
- Javadoc/TSDoc: 한글
- API 응답 메시지: 한글
- 커밋 메시지 본문: 한글 (제목은 영문)
- 코드(변수명, 함수명): 영문

### V. Dual Audience

개발자와 운영팀 모두가 사용하는 도구로 설계한다.
- 개발 환경: 빠른 디버깅, 상세 정보 조회
- 운영 환경: 대시보드, 알람 연동 가능한 구조
- 멀티 클러스터 지원으로 환경 전환 용이

### VI. Simplicity Over Features

단순함을 우선한다.
- YAGNI: 필요하지 않은 기능은 구현하지 않음
- 과도한 추상화 금지
- 명확한 코드 > 영리한 코드

## Technical Constraints

### 백엔드 스택
- Java 21 (LTS)
- Spring Boot 3.2.x
- Maven (빌드 도구)
- kafka-clients + spring-kafka

### 프론트엔드 스택
- Next.js 14.x
- TypeScript 5.x
- Tailwind CSS
- SWR (데이터 페칭)

### 보안
- Basic Authentication
- 설정 파일 기반 계정 관리

## Quality Gates

### 코드 리뷰
- 모든 PR은 리뷰 필수
- 테스트 커버리지 80% 이상 유지
- Constitution 준수 여부 확인

### 테스트
- 빌드 전 모든 테스트 통과 필수
- 통합 테스트는 CI에서 실행

### 문서화
- 공개 API는 반드시 문서화
- 주요 설계 결정은 ADR로 기록

## Governance

- Constitution은 모든 개발 관행보다 우선함
- 수정 시 버전 증가, 문서화, 마이그레이션 계획 필요
- MAJOR: 원칙 삭제/재정의
- MINOR: 원칙 추가/확장
- PATCH: 명확화, 오타 수정

**Version**: 1.0.0 | **Ratified**: 2026-01-22 | **Last Amended**: 2026-01-22
