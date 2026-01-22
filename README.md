# kafka-lens

Kafka 클러스터 모니터링 대시보드 - Maven Central 라이브러리만 사용

## 개요

kafka-lens는 보안 심의 요건으로 AKHQ, Grafana 등을 사용할 수 없는 환경에서 Kafka 클러스터를 모니터링하기 위한 웹 애플리케이션입니다. Maven Central에 등록된 라이브러리만 사용하여 구현되었습니다.

## 아이디어(카카오 오픈채팅에서 발췌)
카프카 모니터링이 필요한데.. akhq나 그라파나 등등은 다 보안심의같은거에 걸린다고 해서 무조건 메이븐 센트럴에 있는 것만 된다고 그러는데 아이디어가 없을까요?

## 주요 기능

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 클러스터 관리 | 멀티 클러스터 전환 및 연결 테스트 | P1 |
| 토픽 조회 | 토픽 목록, 파티션, 복제 정보 | P1 |
| 컨슈머 그룹 | 그룹 목록, 멤버, 할당 정보 | P1 |
| 컨슈머 랙 | 파티션별 랙 모니터링 및 시각화 | P1 |
| 브로커 상태 | 브로커 목록 및 컨트롤러 정보 | P2 |
| 메시지 조회 | 토픽 메시지 샘플링 (최대 1000건) | P2 |
| 인증 | Basic Authentication | P2 |

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.2.x
- spring-kafka
- kafka-clients 3.6.x
- Maven

### Frontend
- Next.js 14
- React 18
- TypeScript 5
- Tailwind CSS
- shadcn/ui
- SWR

## 빠른 시작

### 사전 요구사항

- Java 21+
- Node.js 18+
- Docker (테스트용 Kafka)
- Maven 3.9+

### 1. 로컬 Kafka 실행

```bash
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:3.6.0
```

### 2. 백엔드 실행

```bash
cd kafka-lens-backend

# 빌드
mvn clean package -DskipTests

# 실행
java -jar target/kafka-lens-backend-0.0.1-SNAPSHOT.jar

# 또는 개발 모드
mvn spring-boot:run
```

백엔드: http://localhost:8080

### 3. 프론트엔드 실행

```bash
cd kafka-lens-frontend

# 의존성 설치
npm install

# 개발 서버
npm run dev
```

프론트엔드: http://localhost:3000

### 4. 로그인

- URL: http://localhost:3000/login
- ID: `admin`
- PW: `admin` (개발 환경 기본값)

## 클러스터 설정

`kafka-lens-backend/src/main/resources/clusters.yml`:

```yaml
clusters:
  - id: local
    name: "로컬 Kafka"
    bootstrapServers: "localhost:9092"
    properties:
      security.protocol: PLAINTEXT

  - id: prod
    name: "운영 클러스터"
    bootstrapServers: "kafka-prod:9093"
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USER}"
        password="${KAFKA_PASSWORD}";
```

## API 엔드포인트

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

### API 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-01-22T10:00:00Z"
}
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `ADMIN_PASSWORD` | admin 계정 비밀번호 | admin |
| `KAFKA_USER` | Kafka SASL 사용자 | - |
| `KAFKA_PASSWORD` | Kafka SASL 비밀번호 | - |

## 프로젝트 구조

```
kafka-lens/
├── kafka-lens-backend/          # Spring Boot 백엔드
│   ├── src/main/java/com/kafkalens/
│   │   ├── api/v1/              # REST 컨트롤러
│   │   ├── config/              # 설정 (Security, CORS)
│   │   ├── common/              # 공통 (ApiResponse, Exception)
│   │   ├── domain/              # 도메인 (cluster, topic, consumer, broker, message)
│   │   └── infrastructure/      # 인프라 (AdminClient)
│   └── src/main/resources/
│       ├── application.yml
│       └── clusters.yml
│
├── kafka-lens-frontend/         # Next.js 프론트엔드
│   └── src/
│       ├── app/                 # 페이지 (App Router)
│       ├── components/          # 컴포넌트
│       ├── hooks/               # SWR 훅
│       ├── lib/                 # 유틸리티
│       └── types/               # TypeScript 타입
│
└── specs/                       # 설계 문서 (speckit)
    └── 001-kafka-monitoring/
        ├── spec.md              # 기능 명세
        ├── plan.md              # 기술 계획
        ├── data-model.md        # 데이터 모델
        ├── research.md          # 기술 조사
        ├── quickstart.md        # 빠른 시작 가이드
        └── tasks.md             # 작업 목록
```

## 빌드

### 빌드 스크립트 (권장)

```bash
# Linux/macOS
./build.sh

# Windows
build.bat

# 실행
java -jar kafka-lens-backend/target/kafka-lens-backend-0.0.1-SNAPSHOT.jar
```

### Docker Compose

```bash
# 빌드 및 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build

# 중지
docker-compose down
```

## 테스트

### 백엔드

```bash
cd kafka-lens-backend
mvn test
```

### 프론트엔드

```bash
cd kafka-lens-frontend
npm test
```

## 문제 해결

### Kafka 연결 실패

```bash
# Kafka 상태 확인
docker logs kafka

# 포트 확인
netstat -an | grep 9092
```

### 인증 오류

```bash
# Basic Auth 헤더 확인
curl -v -u admin:admin http://localhost:8080/api/v1/clusters
```

## 제약 사항

- Maven Central 라이브러리만 사용 (보안 심의 요건)
- 메시지 조회 최대 1000건
- 클러스터 연결 타임아웃 5초
- 동시 사용자 10명 기준
