# Implementation Plan: Kafka Monitoring Dashboard

**Branch**: `001-kafka-monitoring` | **Date**: 2026-01-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-kafka-monitoring/spec.md`

## Summary

Maven Central 라이브러리만 사용하여 Kafka 클러스터 모니터링 대시보드를 구축한다.
Spring Boot 백엔드와 Next.js 프론트엔드로 구성하며, kafka-clients의 AdminClient API를 활용한다.

## Technical Context

**Language/Version**:
- Backend: Java 21 (LTS)
- Frontend: TypeScript 5.x

**Primary Dependencies**:
- Backend: Spring Boot 3.2.x, spring-kafka, kafka-clients 3.6.x
- Frontend: Next.js 14.x, React 18, Tailwind CSS, SWR

**Storage**: 설정 파일 (YAML) - 클러스터 정보 저장, DB 불필요

**Testing**:
- Backend: JUnit 5, Mockito, TestContainers (Kafka)
- Frontend: Jest/Vitest, Playwright (E2E)

**Target Platform**:
- Backend: JVM (Linux/macOS/Windows)
- Frontend: 모던 브라우저 (Chrome, Firefox, Safari, Edge)

**Project Type**: Web Application (backend + frontend)

**Performance Goals**:
- 토픽 목록 조회: 5초 이내 (1000개 기준)
- 컨슈머 랙 조회: 3초 이내 (100 파티션 기준)
- 동시 사용자: 10명

**Constraints**:
- Maven Central 라이브러리만 사용 (보안심의 요건)
- 클러스터 연결 타임아웃: 5초
- 메시지 조회 제한: 최대 1000건

**Scale/Scope**:
- 멀티 클러스터 지원
- 개발자 + 운영팀 대상

## Constitution Check

*GATE: Constitution 1.0.0 준수 여부 확인*

| 원칙 | 상태 | 비고 |
|------|------|------|
| I. Maven Central Only | ✅ | 모든 의존성 Maven Central에서 제공 |
| II. Test-First Development | ✅ | JUnit 5 + TestContainers 사용 |
| III. Guardrails First | ✅ | Bean Validation, 타임아웃, 제한 적용 |
| IV. Korean Documentation | ✅ | 한글 주석/문서 |
| V. Dual Audience | ✅ | 개발자 + 운영팀 UI 고려 |
| VI. Simplicity Over Features | ✅ | 핵심 기능만 MVP |

## Project Structure

### Documentation (this feature)

```text
specs/001-kafka-monitoring/
├── spec.md              # 기능 명세
├── plan.md              # 이 파일 (기술 계획)
├── research.md          # 기술 조사
├── data-model.md        # 데이터 모델
├── quickstart.md        # 빠른 시작 가이드
├── contracts/           # API 스펙
│   └── api-v1.yaml
├── checklists/          # 품질 체크리스트
│   └── requirements.md
└── tasks.md             # 작업 목록
```

### Source Code (repository root)

```text
kafka-lens-backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/kafkalens/
│   │   │   ├── KafkaLensApplication.java
│   │   │   ├── config/
│   │   │   │   ├── KafkaAdminConfig.java       # AdminClient 설정
│   │   │   │   ├── SecurityConfig.java          # Basic Auth
│   │   │   │   └── WebConfig.java               # CORS
│   │   │   ├── domain/
│   │   │   │   ├── cluster/
│   │   │   │   │   ├── Cluster.java
│   │   │   │   │   ├── ClusterService.java
│   │   │   │   │   └── ClusterRepository.java
│   │   │   │   ├── topic/
│   │   │   │   │   ├── Topic.java
│   │   │   │   │   ├── TopicDetail.java
│   │   │   │   │   └── TopicService.java
│   │   │   │   ├── consumer/
│   │   │   │   │   ├── ConsumerGroup.java
│   │   │   │   │   ├── ConsumerLag.java
│   │   │   │   │   └── ConsumerService.java
│   │   │   │   ├── broker/
│   │   │   │   │   ├── Broker.java
│   │   │   │   │   └── BrokerService.java
│   │   │   │   └── message/
│   │   │   │       ├── KafkaMessage.java
│   │   │   │       └── MessageService.java
│   │   │   ├── api/
│   │   │   │   └── v1/
│   │   │   │       ├── ClusterController.java
│   │   │   │       ├── TopicController.java
│   │   │   │       ├── ConsumerController.java
│   │   │   │       ├── BrokerController.java
│   │   │   │       └── MessageController.java
│   │   │   └── infrastructure/
│   │   │       └── kafka/
│   │   │           ├── AdminClientFactory.java
│   │   │           └── AdminClientWrapper.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── clusters.yml
│   └── test/
│       └── java/com/kafkalens/
│           ├── domain/                          # 단위 테스트
│           ├── api/                             # 통합 테스트
│           └── infrastructure/                  # 인프라 테스트

kafka-lens-frontend/
├── package.json
├── next.config.js
├── tailwind.config.js
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx                            # 대시보드 홈
│   │   ├── login/
│   │   │   └── page.tsx
│   │   └── clusters/
│   │       └── [id]/
│   │           ├── page.tsx                    # 클러스터 상세
│   │           ├── topics/
│   │           │   └── page.tsx
│   │           ├── consumers/
│   │           │   └── page.tsx
│   │           ├── brokers/
│   │           │   └── page.tsx
│   │           └── messages/
│   │               └── page.tsx
│   ├── components/
│   │   ├── ui/                                 # shadcn/ui
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── MainLayout.tsx
│   │   ├── cluster/
│   │   │   ├── ClusterList.tsx
│   │   │   └── ClusterCard.tsx
│   │   ├── topic/
│   │   │   ├── TopicList.tsx
│   │   │   └── TopicDetail.tsx
│   │   ├── consumer/
│   │   │   ├── ConsumerGroupList.tsx
│   │   │   └── LagChart.tsx
│   │   ├── broker/
│   │   │   └── BrokerList.tsx
│   │   └── message/
│   │       └── MessageViewer.tsx
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useClusters.ts
│   │   ├── useTopics.ts
│   │   └── useConsumerGroups.ts
│   ├── lib/
│   │   ├── api.ts
│   │   └── utils.ts
│   └── types/
│       └── index.ts
└── tests/
    ├── components/
    └── e2e/
```

**Structure Decision**: Web Application 구조 선택. 백엔드(Spring Boot)와 프론트엔드(Next.js)를 분리하여 독립적 개발/배포 가능. 단일 JAR 배포 시 프론트엔드 빌드 결과를 백엔드 static 리소스로 포함.

## Complexity Tracking

> Constitution Check 통과 - 위반 사항 없음

| 항목 | 상태 | 비고 |
|------|------|------|
| 과도한 추상화 | ❌ 없음 | 직접적인 AdminClient 래퍼만 사용 |
| 불필요한 기능 | ❌ 없음 | MVP 범위 내 핵심 기능만 |
| 복잡한 패턴 | ❌ 없음 | 단순 레이어드 아키텍처 |

## API Design

### REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/clusters | 클러스터 목록 |
| GET | /api/v1/clusters/{id} | 클러스터 상세 |
| POST | /api/v1/clusters/{id}/test | 연결 테스트 |
| GET | /api/v1/clusters/{id}/topics | 토픽 목록 |
| GET | /api/v1/clusters/{id}/topics/{name} | 토픽 상세 |
| GET | /api/v1/clusters/{id}/consumer-groups | 컨슈머 그룹 목록 |
| GET | /api/v1/clusters/{id}/consumer-groups/{groupId} | 컨슈머 그룹 상세 |
| GET | /api/v1/clusters/{id}/consumer-groups/{groupId}/lag | 랙 정보 |
| GET | /api/v1/clusters/{id}/brokers | 브로커 목록 |
| GET | /api/v1/clusters/{id}/topics/{name}/messages | 메시지 조회 |

### Response Format

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-01-22T10:00:00Z"
}
```

## Implementation Phases

### Phase 1: 백엔드 기반 (P1 기능)
1. Spring Boot 프로젝트 초기화
2. AdminClient 래퍼 구현
3. 클러스터 관리 API
4. 토픽 API
5. 컨슈머 그룹/랙 API
6. 브로커 API

### Phase 2: 프론트엔드 (P1 기능)
1. Next.js 프로젝트 초기화
2. 공통 레이아웃/컴포넌트
3. 클러스터 선택 화면
4. 토픽 목록/상세
5. 컨슈머 그룹/랙 대시보드
6. 브로커 상태

### Phase 3: P2 기능 + 통합
1. 메시지 조회 API
2. 메시지 뷰어 UI
3. Basic Auth 구현
4. 로그인 화면
5. 빌드 통합 (단일 JAR)

## Dependencies (Maven Central)

### Backend
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Frontend
```json
{
  "dependencies": {
    "next": "^14.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "swr": "^2.2.0",
    "tailwindcss": "^3.4.0",
    "@radix-ui/react-*": "latest"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "@playwright/test": "^1.40.0"
  }
}
```
