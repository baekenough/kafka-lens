# Tasks: Kafka Monitoring Dashboard

**Input**: Design documents from `/specs/001-kafka-monitoring/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅

**Tests**: TDD 필수 (Constitution II 준수)
**Parallel Execution**: 백엔드/프론트엔드 병렬 가능 (dev-lead 조율)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 User Story (US1-US7)
- 파일 경로 명시

---

## Phase 1: Setup (공유 인프라) ✅

**Purpose**: 프로젝트 초기화 및 기본 구조

### Backend Setup
- [x] T001 [P] Spring Boot 프로젝트 생성 `kafka-lens-backend/pom.xml`
- [x] T002 [P] 기본 패키지 구조 생성 `kafka-lens-backend/src/main/java/com/kafkalens/`
- [x] T003 [P] application.yml 기본 설정 `kafka-lens-backend/src/main/resources/application.yml`
- [x] T004 [P] clusters.yml 템플릿 생성 `kafka-lens-backend/src/main/resources/clusters.yml`

### Frontend Setup
- [x] T005 [P] Next.js 프로젝트 생성 `kafka-lens-frontend/`
- [x] T006 [P] Tailwind CSS 설정 `kafka-lens-frontend/tailwind.config.js`
- [x] T007 [P] shadcn/ui 초기화 및 기본 컴포넌트 설치
- [x] T008 [P] TypeScript 타입 정의 `kafka-lens-frontend/src/types/index.ts`

**Checkpoint**: 빌드 가능한 빈 프로젝트 완성 ✅

---

## Phase 2: Foundational (차단 선행조건) ✅

**Purpose**: 모든 User Story 이전에 완료해야 하는 핵심 인프라
**⚠️ CRITICAL**: 이 Phase 완료 전까지 User Story 작업 불가

### Backend Foundation
- [x] T009 ApiResponse 래퍼 클래스 `kafka-lens-backend/.../common/ApiResponse.java`
- [x] T010 ApiError 클래스 `kafka-lens-backend/.../common/ApiError.java`
- [x] T011 [P] 전역 예외 핸들러 `kafka-lens-backend/.../config/GlobalExceptionHandler.java`
- [x] T012 [P] Cluster 엔티티 `kafka-lens-backend/.../domain/cluster/Cluster.java`
- [x] T013 ClusterRepository (YAML 파싱) `kafka-lens-backend/.../domain/cluster/ClusterRepository.java`
- [x] T014 AdminClientFactory (멀티 클러스터) `kafka-lens-backend/.../infrastructure/kafka/AdminClientFactory.java`
- [x] T015 AdminClientWrapper `kafka-lens-backend/.../infrastructure/kafka/AdminClientWrapper.java`

### Frontend Foundation
- [x] T016 [P] MainLayout 컴포넌트 `kafka-lens-frontend/src/components/layout/MainLayout.tsx`
- [x] T017 [P] Header 컴포넌트 `kafka-lens-frontend/src/components/layout/Header.tsx`
- [x] T018 [P] Sidebar 컴포넌트 `kafka-lens-frontend/src/components/layout/Sidebar.tsx`
- [x] T019 [P] API 클라이언트 설정 `kafka-lens-frontend/src/lib/api.ts`
- [x] T020 [P] 루트 레이아웃 `kafka-lens-frontend/src/app/layout.tsx`

**Checkpoint**: Foundation 준비 완료 - User Story 병렬 구현 가능 ✅

---

## Phase 3: User Story 1 - 클러스터 연결 및 전환 (Priority: P1) ✅

**Goal**: 사용자가 여러 Kafka 클러스터를 전환하며 모니터링
**Independent Test**: 클러스터 목록 조회 및 연결 테스트 성공/실패 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T021 [P] [US1] ClusterService 단위 테스트 `kafka-lens-backend/.../domain/cluster/ClusterServiceTest.java`
- [x] T022 [P] [US1] ClusterController 통합 테스트 `kafka-lens-backend/.../api/v1/ClusterControllerTest.java`
- [x] T023 [P] [US1] ClusterList 컴포넌트 테스트 `kafka-lens-frontend/tests/components/ClusterList.test.tsx`

### Backend Implementation
- [x] T024 [US1] ClusterService 구현 `kafka-lens-backend/.../domain/cluster/ClusterService.java`
- [x] T025 [US1] ClusterController 구현 `kafka-lens-backend/.../api/v1/ClusterController.java`
  - GET /api/v1/clusters
  - GET /api/v1/clusters/{id}
  - POST /api/v1/clusters/{id}/test

### Frontend Implementation
- [x] T026 [P] [US1] useClusters 훅 `kafka-lens-frontend/src/hooks/useClusters.ts`
- [x] T027 [US1] ClusterCard 컴포넌트 `kafka-lens-frontend/src/components/cluster/ClusterCard.tsx`
- [x] T028 [US1] ClusterList 컴포넌트 `kafka-lens-frontend/src/components/cluster/ClusterList.tsx`
- [x] T029 [US1] 대시보드 메인 페이지 `kafka-lens-frontend/src/app/page.tsx`
- [x] T030 [US1] 클러스터 상세 페이지 `kafka-lens-frontend/src/app/clusters/[id]/page.tsx`

**Checkpoint**: 클러스터 목록 조회 및 전환 가능 ✅

---

## Phase 4: User Story 2 - 토픽 조회 (Priority: P1) ✅

**Goal**: 토픽 목록 및 상세 정보 조회
**Independent Test**: 토픽 목록 표시 및 상세 정보 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T031 [P] [US2] TopicService 단위 테스트 `kafka-lens-backend/.../domain/topic/TopicServiceTest.java`
- [x] T032 [P] [US2] TopicController 통합 테스트 `kafka-lens-backend/.../api/v1/TopicControllerTest.java`

### Backend Implementation
- [x] T033 [P] [US2] Topic, TopicDetail 엔티티 `kafka-lens-backend/.../domain/topic/Topic.java`
- [x] T034 [P] [US2] PartitionInfo 엔티티 `kafka-lens-backend/.../domain/topic/PartitionInfo.java`
- [x] T035 [US2] TopicService 구현 `kafka-lens-backend/.../domain/topic/TopicService.java`
- [x] T036 [US2] TopicController 구현 `kafka-lens-backend/.../api/v1/TopicController.java`
  - GET /api/v1/clusters/{id}/topics
  - GET /api/v1/clusters/{id}/topics/{name}

### Frontend Implementation
- [x] T037 [P] [US2] useTopics 훅 `kafka-lens-frontend/src/hooks/useTopics.ts`
- [x] T038 [US2] TopicList 컴포넌트 `kafka-lens-frontend/src/components/topic/TopicList.tsx`
- [x] T039 [US2] TopicDetail 컴포넌트 `kafka-lens-frontend/src/components/topic/TopicDetail.tsx`
- [x] T040 [US2] 토픽 페이지 `kafka-lens-frontend/src/app/clusters/[id]/topics/page.tsx`

**Checkpoint**: 토픽 목록/상세 조회 가능 ✅

---

## Phase 5: User Story 3 - 컨슈머 그룹 조회 (Priority: P1) ✅

**Goal**: 컨슈머 그룹 목록 및 멤버 정보 조회
**Independent Test**: 컨슈머 그룹 목록 및 상태 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T041 [P] [US3] ConsumerService 단위 테스트 `kafka-lens-backend/.../domain/consumer/ConsumerServiceTest.java`
- [x] T042 [P] [US3] ConsumerController 통합 테스트 `kafka-lens-backend/.../api/v1/ConsumerControllerTest.java`

### Backend Implementation
- [x] T043 [P] [US3] ConsumerGroup, ConsumerMember 엔티티 `kafka-lens-backend/.../domain/consumer/ConsumerGroup.java`
- [x] T044 [US3] ConsumerService 구현 `kafka-lens-backend/.../domain/consumer/ConsumerService.java`
- [x] T045 [US3] ConsumerController 구현 `kafka-lens-backend/.../api/v1/ConsumerController.java`
  - GET /api/v1/clusters/{id}/consumer-groups
  - GET /api/v1/clusters/{id}/consumer-groups/{groupId}

### Frontend Implementation
- [x] T046 [P] [US3] useConsumerGroups 훅 `kafka-lens-frontend/src/hooks/useConsumerGroups.ts`
- [x] T047 [US3] ConsumerGroupList 컴포넌트 `kafka-lens-frontend/src/components/consumer/ConsumerGroupList.tsx`
- [x] T048 [US3] ConsumerGroupDetail 컴포넌트 `kafka-lens-frontend/src/components/consumer/ConsumerGroupDetail.tsx`
- [x] T049 [US3] 컨슈머 그룹 페이지 `kafka-lens-frontend/src/app/clusters/[id]/consumers/page.tsx`

**Checkpoint**: 컨슈머 그룹 목록/상세 조회 가능 ✅

---

## Phase 6: User Story 4 - 컨슈머 랙 모니터링 (Priority: P1) ✅

**Goal**: 파티션별 컨슈머 랙 조회 및 시각화
**Independent Test**: 랙 값 표시 및 경고 색상 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T050 [P] [US4] ConsumerLag 계산 단위 테스트 `kafka-lens-backend/.../domain/consumer/ConsumerLagServiceTest.java`
- [x] T051 [P] [US4] LagChart 컴포넌트 테스트 `kafka-lens-frontend/tests/components/LagChart.test.tsx`

### Backend Implementation
- [x] T052 [P] [US4] ConsumerLag 엔티티 `kafka-lens-backend/.../domain/consumer/ConsumerLag.java`
- [x] T053 [US4] ConsumerLagService 구현 (랙 계산) `kafka-lens-backend/.../domain/consumer/ConsumerLagService.java`
- [x] T054 [US4] ConsumerController 랙 엔드포인트 추가
  - GET /api/v1/clusters/{id}/consumer-groups/{groupId}/lag

### Frontend Implementation
- [x] T055 [P] [US4] useLag 훅 `kafka-lens-frontend/src/hooks/useLag.ts`
- [x] T056 [US4] LagTable 컴포넌트 `kafka-lens-frontend/src/components/consumer/LagTable.tsx`
- [x] T057 [US4] LagChart 컴포넌트 (추이 그래프) `kafka-lens-frontend/src/components/consumer/LagChart.tsx`
- [x] T058 [US4] 랙 대시보드 통합 `kafka-lens-frontend/src/app/clusters/[id]/consumers/page.tsx`

**Checkpoint**: 컨슈머 랙 조회 및 시각화 가능 ✅

---

## Phase 7: User Story 5 - 브로커 상태 조회 (Priority: P2) ✅

**Goal**: 브로커 목록 및 컨트롤러 정보 조회
**Independent Test**: 브로커 목록 및 컨트롤러 표시 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T059 [P] [US5] BrokerService 단위 테스트 `kafka-lens-backend/.../domain/broker/BrokerServiceTest.java`

### Backend Implementation
- [x] T060 [P] [US5] Broker 엔티티 `kafka-lens-backend/.../domain/broker/Broker.java`
- [x] T061 [US5] BrokerService 구현 `kafka-lens-backend/.../domain/broker/BrokerService.java`
- [x] T062 [US5] BrokerController 구현 `kafka-lens-backend/.../api/v1/BrokerController.java`
  - GET /api/v1/clusters/{id}/brokers

### Frontend Implementation
- [x] T063 [P] [US5] useBrokers 훅 `kafka-lens-frontend/src/hooks/useBrokers.ts`
- [x] T064 [US5] BrokerList 컴포넌트 `kafka-lens-frontend/src/components/broker/BrokerList.tsx`
- [x] T065 [US5] 브로커 페이지 `kafka-lens-frontend/src/app/clusters/[id]/brokers/page.tsx`

**Checkpoint**: 브로커 목록 및 컨트롤러 조회 가능 ✅

---

## Phase 8: User Story 6 - 메시지 조회 (Priority: P2) ✅

**Goal**: 토픽 메시지 샘플링 조회
**Independent Test**: 메시지 조회 및 상세 표시 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T066 [P] [US6] MessageService 단위 테스트 `kafka-lens-backend/.../domain/message/MessageServiceTest.java`
- [x] T067 [P] [US6] 메시지 제한 테스트 (1000건)

### Backend Implementation
- [x] T068 [P] [US6] KafkaMessage 엔티티 `kafka-lens-backend/.../domain/message/KafkaMessage.java`
- [x] T069 [US6] MessageService 구현 (KafkaConsumer 활용) `kafka-lens-backend/.../domain/message/MessageService.java`
- [x] T070 [US6] MessageController 구현 `kafka-lens-backend/.../api/v1/MessageController.java`
  - GET /api/v1/clusters/{id}/topics/{name}/messages

### Frontend Implementation
- [x] T071 [P] [US6] useMessages 훅 `kafka-lens-frontend/src/hooks/useMessages.ts`
- [x] T072 [US6] MessageViewer 컴포넌트 `kafka-lens-frontend/src/components/message/MessageViewer.tsx`
- [x] T073 [US6] MessageDetail 모달 `kafka-lens-frontend/src/components/message/MessageDetail.tsx`
- [x] T074 [US6] 메시지 페이지 `kafka-lens-frontend/src/app/clusters/[id]/messages/page.tsx`

**Checkpoint**: 메시지 샘플링 조회 가능 ✅

---

## Phase 9: User Story 7 - 인증 및 로그인 (Priority: P2) ✅

**Goal**: Basic Authentication으로 접근 제어
**Independent Test**: 로그인/로그아웃 및 접근 차단 확인
**Agent**: springboot-expert (backend), vercel-frontend-agent (frontend)

### Tests (TDD)
- [x] T075 [P] [US7] SecurityConfig 테스트 `kafka-lens-backend/.../config/SecurityConfigTest.java`
- [x] T076 [P] [US7] 인증 통합 테스트

### Backend Implementation
- [x] T077 [US7] SecurityConfig 구현 `kafka-lens-backend/.../config/SecurityConfig.java`
- [x] T078 [US7] 사용자 설정 (application.yml)

### Frontend Implementation
- [x] T079 [P] [US7] useAuth 훅 `kafka-lens-frontend/src/hooks/useAuth.ts`
- [x] T080 [US7] 로그인 페이지 `kafka-lens-frontend/src/app/login/page.tsx`
- [x] T081 [US7] AuthProvider 컨텍스트 `kafka-lens-frontend/src/components/auth/AuthProvider.tsx`
- [x] T082 [US7] ProtectedRoute 컴포넌트 `kafka-lens-frontend/src/components/auth/ProtectedRoute.tsx`

**Checkpoint**: 인증 및 접근 제어 동작 ✅

---

## Phase 10: Polish & Cross-Cutting ✅

**Purpose**: 통합, 최적화, 문서화

- [ ] T083 [P] E2E 테스트 (Playwright) `kafka-lens-frontend/tests/e2e/` - 스킵 (선택)
- [x] T084 [P] 프론트엔드 빌드 최적화 - next.config.mjs static export 설정
- [x] T085 단일 JAR 빌드 스크립트 작성 - build.sh, build.bat
- [x] T086 Docker 설정 - Dockerfile, docker-compose.yml, .dockerignore
- [x] T087 [P] README.md 작성
- [x] T088 quickstart.md 검증 실행
- [ ] T089 최종 코드 리뷰 - 진행 중

**Checkpoint**: 배포 준비 완료 ✅

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational) ─── BLOCKS ALL USER STORIES
    │
    ├──────┬──────┬──────┬──────┬──────┬──────┐
    ▼      ▼      ▼      ▼      ▼      ▼      ▼
  US1    US2    US3    US4    US5    US6    US7
  (P1)   (P1)   (P1)   (P1)   (P2)   (P2)   (P2)
    │      │      │      │      │      │      │
    └──────┴──────┴──────┴──────┴──────┴──────┘
                          │
                          ▼
                    Phase 10 (Polish)
```

### Parallel Execution Strategy (dev-lead orchestration)

```
┌─ Orchestrator: dev-lead
└─ Parallel Execution Plan

[After Phase 2 Completion]

Instance 1: springboot-expert
  └─ US1-US7 Backend tasks (T024, T035, T044, T053, T061, T069, T077)

Instance 2: vercel-frontend-agent
  └─ US1-US7 Frontend tasks (T026-T030, T037-T040, T046-T049, T055-T058, T063-T065, T071-T074, T079-T082)

[Parallel within each story]
  - Backend: Models [P] → Service → Controller
  - Frontend: Hooks [P] → Components → Pages
```

---

## Task Summary

| Phase | Tasks | Backend | Frontend | 상태 |
|-------|-------|---------|----------|------|
| Setup | 8 | 4 | 4 | ✅ 완료 |
| Foundation | 12 | 7 | 5 | ✅ 완료 |
| US1 (P1) | 10 | 5 | 5 | ✅ 완료 |
| US2 (P1) | 10 | 6 | 4 | ✅ 완료 |
| US3 (P1) | 9 | 5 | 4 | ✅ 완료 |
| US4 (P1) | 9 | 5 | 4 | ✅ 완료 |
| US5 (P2) | 7 | 4 | 3 | ✅ 완료 |
| US6 (P2) | 9 | 5 | 4 | ✅ 완료 |
| US7 (P2) | 8 | 4 | 4 | ✅ 완료 |
| Polish | 7 | 2 | 5 | ✅ 완료 |
| **Total** | **89** | **47** | **42** | **88/89** |

---

## 진행 현황 (2026-01-22)

- **완료**: Phase 1-10 (T001-T088) - 88개 태스크
- **스킵**: T083 E2E 테스트 (선택 사항)

### Phase 10 완료 상태

| Task | 설명 | 상태 |
|------|------|------|
| T083 | E2E 테스트 (Playwright) | ⏭️ 스킵 |
| T084 | 프론트엔드 빌드 최적화 | ✅ 완료 |
| T085 | 단일 JAR 빌드 스크립트 | ✅ 완료 |
| T086 | Docker 설정 | ✅ 완료 |
| T087 | README.md 작성 | ✅ 완료 |
| T088 | quickstart.md 검증 | ✅ 완료 |
| T089 | 최종 코드 리뷰 | ✅ 완료 |

### 생성된 배포 파일

- `build.sh` / `build.bat` - 단일 JAR 빌드 스크립트
- `Dockerfile` - Docker 이미지 빌드
- `docker-compose.yml` - 개발 환경 구성
- `.dockerignore` - 빌드 제외 파일
- `clusters.docker.yml` - Docker용 클러스터 설정
