# kafka-lens 설계 문서

> 작성일: 2026-01-22
> 상태: 승인 대기

## 1. 개요

### 1.1 프로젝트 목적
Maven Central 라이브러리만으로 구축하는 Kafka 모니터링 대시보드.
AKHQ, Grafana 등 외부 도구 사용이 제한된 환경을 위한 대안.

### 1.2 대상 사용자
- 개발자: 로컬/개발 환경 디버깅
- 운영팀: 프로덕션 클러스터 상시 모니터링

### 1.3 핵심 제약 조건
- **Maven Central 라이브러리만 사용** (보안심의 통과 요건)
- 테스트 코드 필수
- 가드레일 (입력 검증, 에러 처리) 적용
- 문서/주석/docstring은 한글로 작성

## 2. 기능 요구사항

### 2.1 MVP 기능 (전체 포함)

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 토픽 목록/상세 | 파티션 수, 복제 팩터, 설정 조회 | P0 |
| 컨슈머 그룹 | 그룹 목록, 멤버, 상태 조회 | P0 |
| 컨슈머 랙 | 파티션별 지연 현황 모니터링 | P0 |
| 브로커/클러스터 | 노드 정보, 컨트롤러 상태 | P0 |
| 메시지 조회 | 토픽 메시지 샘플링 (최신 N개) | P0 |
| 멀티 클러스터 | 여러 Kafka 클러스터 전환/관리 | P0 |

### 2.2 인증
- Basic Authentication (ID/PW)
- 설정 파일에서 계정 관리

## 3. 기술 스택

### 3.1 백엔드
| 항목 | 선택 | 버전 |
|------|------|------|
| 언어 | Java | 21 (LTS) |
| 프레임워크 | Spring Boot | 3.2.x |
| 빌드 | Maven | 3.9.x |
| Kafka 클라이언트 | kafka-clients | 3.6.x |
| 보안 | Spring Security | 6.x |

### 3.2 프론트엔드
| 항목 | 선택 | 버전 |
|------|------|------|
| 프레임워크 | Next.js | 14.x |
| 언어 | TypeScript | 5.x |
| 스타일 | Tailwind CSS | 3.x |
| 데이터 페칭 | SWR | 2.x |
| UI 컴포넌트 | shadcn/ui | latest |

### 3.3 핵심 의존성 (Maven Central)

```xml
<!-- 백엔드 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 4. 아키텍처

### 4.1 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│                      kafka-lens                          │
├────────────────────────┬────────────────────────────────┤
│   Frontend (Next.js)   │      Backend (Spring Boot)     │
│   - React 18           │      - Java 21                 │
│   - TypeScript         │      - kafka-clients           │
│   - Tailwind CSS       │      - spring-kafka            │
│   - SWR                │      - spring-security         │
└────────────┬───────────┴──────────────┬─────────────────┘
             │         REST API          │
             └───────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Kafka #1 │    │ Kafka #2 │    │ Kafka #N │
    └──────────┘    └──────────┘    └──────────┘
```

### 4.2 백엔드 패키지 구조

```
kafka-lens-backend/
├── src/main/java/com/kafkalens/
│   ├── KafkaLensApplication.java       # 메인 애플리케이션
│   ├── config/
│   │   ├── KafkaAdminConfig.java       # 멀티 클러스터 AdminClient
│   │   ├── SecurityConfig.java          # Basic Auth 설정
│   │   └── WebConfig.java               # CORS, 기타 웹 설정
│   ├── domain/
│   │   ├── cluster/
│   │   │   ├── Cluster.java             # 클러스터 엔티티
│   │   │   ├── ClusterService.java      # 클러스터 관리 로직
│   │   │   └── ClusterRepository.java   # 클러스터 설정 저장소
│   │   ├── topic/
│   │   │   ├── Topic.java
│   │   │   ├── TopicDetail.java
│   │   │   └── TopicService.java
│   │   ├── consumer/
│   │   │   ├── ConsumerGroup.java
│   │   │   ├── ConsumerLag.java
│   │   │   └── ConsumerService.java
│   │   ├── broker/
│   │   │   ├── Broker.java
│   │   │   └── BrokerService.java
│   │   └── message/
│   │       ├── KafkaMessage.java
│   │       └── MessageService.java
│   ├── api/
│   │   └── v1/
│   │       ├── ClusterController.java
│   │       ├── TopicController.java
│   │       ├── ConsumerController.java
│   │       ├── BrokerController.java
│   │       └── MessageController.java
│   └── infrastructure/
│       └── kafka/
│           ├── AdminClientFactory.java  # AdminClient 생성/캐싱
│           ├── AdminClientWrapper.java  # AdminClient 래퍼
│           └── KafkaConsumerFactory.java # 메시지 조회용
├── src/main/resources/
│   ├── application.yml                  # 기본 설정
│   └── clusters.yml                     # 클러스터 설정 (외부화)
└── src/test/java/
    └── com/kafkalens/
        ├── domain/                      # 단위 테스트
        ├── api/                         # 통합 테스트
        └── infrastructure/              # 인프라 테스트
```

### 4.3 프론트엔드 구조

```
kafka-lens-frontend/
├── src/
│   ├── app/                            # Next.js App Router
│   │   ├── layout.tsx
│   │   ├── page.tsx                    # 대시보드 홈
│   │   ├── clusters/
│   │   │   └── [id]/
│   │   │       ├── page.tsx            # 클러스터 상세
│   │   │       ├── topics/
│   │   │       ├── consumers/
│   │   │       ├── brokers/
│   │   │       └── messages/
│   │   └── login/
│   ├── components/
│   │   ├── ui/                         # shadcn/ui 컴포넌트
│   │   ├── layout/                     # 레이아웃 컴포넌트
│   │   ├── cluster/                    # 클러스터 관련
│   │   ├── topic/                      # 토픽 관련
│   │   ├── consumer/                   # 컨슈머 관련
│   │   └── message/                    # 메시지 관련
│   ├── hooks/                          # 커스텀 훅
│   ├── lib/                            # 유틸리티
│   └── types/                          # TypeScript 타입
├── public/
└── package.json
```

## 5. API 설계

### 5.1 REST API 엔드포인트

#### 클러스터 관리
```
GET    /api/v1/clusters                 # 클러스터 목록
GET    /api/v1/clusters/{id}            # 클러스터 상세
POST   /api/v1/clusters/{id}/test       # 연결 테스트
```

#### 토픽
```
GET    /api/v1/clusters/{id}/topics                    # 토픽 목록
GET    /api/v1/clusters/{id}/topics/{name}             # 토픽 상세
GET    /api/v1/clusters/{id}/topics/{name}/partitions  # 파티션 정보
```

#### 컨슈머 그룹
```
GET    /api/v1/clusters/{id}/consumer-groups                    # 그룹 목록
GET    /api/v1/clusters/{id}/consumer-groups/{groupId}          # 그룹 상세
GET    /api/v1/clusters/{id}/consumer-groups/{groupId}/lag      # 랙 정보
```

#### 브로커
```
GET    /api/v1/clusters/{id}/brokers              # 브로커 목록
GET    /api/v1/clusters/{id}/brokers/{brokerId}   # 브로커 상세
```

#### 메시지
```
GET    /api/v1/clusters/{id}/topics/{name}/messages   # 메시지 조회
       ?partition=0&offset=100&limit=50
```

### 5.2 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-01-22T10:00:00Z"
}
```

### 5.3 에러 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "CLUSTER_NOT_FOUND",
    "message": "클러스터를 찾을 수 없습니다",
    "details": { ... }
  },
  "timestamp": "2026-01-22T10:00:00Z"
}
```

## 6. 보안

### 6.1 인증
- Spring Security Basic Authentication
- 계정 정보는 application.yml에서 관리

```yaml
kafka-lens:
  security:
    users:
      - username: admin
        password: ${ADMIN_PASSWORD}
        roles: [ADMIN]
      - username: viewer
        password: ${VIEWER_PASSWORD}
        roles: [VIEWER]
```

### 6.2 권한

| 역할 | 권한 |
|------|------|
| ADMIN | 모든 기능 (조회 + 설정 변경) |
| VIEWER | 조회만 가능 |

### 6.3 가드레일

- 입력값 검증 (@Valid, Bean Validation)
- 클러스터 연결 타임아웃 (기본 5초)
- 메시지 조회 limit 제한 (최대 1000건)
- Rate limiting (선택적)

## 7. 설정

### 7.1 클러스터 설정 (clusters.yml)

```yaml
clusters:
  - id: dev-cluster
    name: "개발 클러스터"
    bootstrapServers: "localhost:9092"
    properties:
      security.protocol: PLAINTEXT

  - id: prod-cluster
    name: "운영 클러스터"
    bootstrapServers: "kafka-prod-1:9092,kafka-prod-2:9092"
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: "..."
```

### 7.2 애플리케이션 설정

```yaml
kafka-lens:
  admin-client:
    request-timeout-ms: 5000
    default-api-timeout-ms: 10000

  message:
    max-fetch-size: 1000
    default-fetch-size: 50
```

## 8. 테스트 전략

### 8.1 테스트 종류

| 종류 | 범위 | 도구 |
|------|------|------|
| 단위 테스트 | Service, Domain | JUnit 5, Mockito |
| 통합 테스트 | Controller, API | Spring Test, TestContainers |
| E2E 테스트 | 프론트엔드 흐름 | Playwright |

### 8.2 테스트 컨테이너

```java
@Testcontainers
class TopicServiceIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
}
```

## 9. 프로젝트 구조

```
kafka-lens/
├── kafka-lens-backend/        # Spring Boot 백엔드
├── kafka-lens-frontend/       # Next.js 프론트엔드
├── docs/
│   ├── plans/                 # 설계 문서
│   └── api/                   # API 문서
├── docker-compose.yml         # 개발 환경
├── README.md
└── CLAUDE.md                  # AI 에이전트 지침
```

## 10. 구현 순서 (제안)

### Phase 1: 백엔드 기반
1. 프로젝트 초기화 (Spring Boot, Maven)
2. 클러스터 연결 및 AdminClient 래퍼
3. 토픽 API
4. 컨슈머 그룹 API
5. 브로커 API
6. 메시지 조회 API
7. 인증 (Basic Auth)

### Phase 2: 프론트엔드
1. Next.js 프로젝트 초기화
2. 공통 컴포넌트 (레이아웃, UI)
3. 클러스터 선택/전환
4. 토픽 목록/상세
5. 컨슈머 그룹/랙 대시보드
6. 브로커 상태
7. 메시지 뷰어

### Phase 3: 통합 및 배포
1. 백엔드 + 프론트엔드 통합 빌드
2. Docker 이미지
3. 문서화

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-01-22 | 0.1 | 초안 작성 |
