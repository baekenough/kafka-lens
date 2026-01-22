# Quickstart Guide: kafka-lens

## 사전 요구사항

- Java 21+
- Node.js 18+
- Docker (테스트용 Kafka)
- Maven 3.9+

## 1. 로컬 Kafka 실행 (개발용)

```bash
# Docker Compose로 Kafka 실행
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

## 2. 백엔드 실행

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

## 3. 프론트엔드 실행

```bash
cd kafka-lens-frontend

# 의존성 설치
npm install

# 개발 서버
npm run dev
```

프론트엔드: http://localhost:3000

## 4. 클러스터 설정

`kafka-lens-backend/src/main/resources/clusters.yml`:

```yaml
clusters:
  - id: local
    name: "로컬 Kafka"
    bootstrapServers: "localhost:9092"
    properties:
      security.protocol: PLAINTEXT
```

## 5. 로그인

- URL: http://localhost:3000/login
- ID: `admin`
- PW: `admin` (개발 환경 기본값)

## 6. 테스트 데이터 생성

```bash
# 토픽 생성
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create --topic test-topic \
  --partitions 3 --replication-factor 1 \
  --bootstrap-server localhost:9092

# 메시지 발행
docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh \
  --topic test-topic \
  --bootstrap-server localhost:9092 <<EOF
{"id": 1, "message": "Hello"}
{"id": 2, "message": "World"}
EOF

# 컨슈머 그룹 생성
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --topic test-topic \
  --group test-group \
  --from-beginning \
  --max-messages 1 \
  --bootstrap-server localhost:9092
```

## 7. API 테스트

```bash
# 클러스터 목록
curl -u admin:admin http://localhost:8080/api/v1/clusters

# 토픽 목록
curl -u admin:admin http://localhost:8080/api/v1/clusters/local/topics

# 컨슈머 그룹
curl -u admin:admin http://localhost:8080/api/v1/clusters/local/consumer-groups

# 컨슈머 랙
curl -u admin:admin http://localhost:8080/api/v1/clusters/local/consumer-groups/test-group/lag
```

## 8. 프로덕션 빌드

```bash
# 프론트엔드 빌드
cd kafka-lens-frontend
npm run build

# 빌드 결과를 백엔드 static으로 복사
cp -r out/* ../kafka-lens-backend/src/main/resources/static/

# 단일 JAR 빌드
cd ../kafka-lens-backend
mvn clean package

# 실행
java -jar target/kafka-lens-backend-0.0.1-SNAPSHOT.jar
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `ADMIN_PASSWORD` | admin 계정 비밀번호 | admin |
| `KAFKA_USER` | Kafka SASL 사용자 | - |
| `KAFKA_PASSWORD` | Kafka SASL 비밀번호 | - |

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
