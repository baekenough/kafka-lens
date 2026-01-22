# Technical Research: Kafka Monitoring Dashboard

**Branch**: `001-kafka-monitoring` | **Date**: 2026-01-22

## 1. Kafka AdminClient API 조사

### 1.1 사용 가능한 API 메서드

| 메서드 | 용도 | 반환값 |
|--------|------|--------|
| `describeCluster()` | 클러스터 정보 | 브로커 목록, 컨트롤러 |
| `listTopics()` | 토픽 목록 | 토픽 이름 Set |
| `describeTopics()` | 토픽 상세 | 파티션, 복제 정보 |
| `describeConfigs()` | 토픽 설정 | 설정 맵 |
| `listConsumerGroups()` | 컨슈머 그룹 목록 | 그룹 ID, 상태 |
| `describeConsumerGroups()` | 그룹 상세 | 멤버, 할당 정보 |
| `listConsumerGroupOffsets()` | 오프셋 조회 | 파티션별 오프셋 |
| `listOffsets()` | 최신 오프셋 | 파티션별 end offset |

### 1.2 컨슈머 랙 계산 방법

```java
// 1. 컨슈머 그룹의 커밋된 오프셋 조회
Map<TopicPartition, OffsetAndMetadata> committedOffsets =
    admin.listConsumerGroupOffsets(groupId)
         .partitionsToOffsetAndMetadata().get();

// 2. 각 파티션의 최신 오프셋 조회
Map<TopicPartition, OffsetSpec> request = new HashMap<>();
for (TopicPartition tp : committedOffsets.keySet()) {
    request.put(tp, OffsetSpec.latest());
}
Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
    admin.listOffsets(request).all().get();

// 3. 랙 계산
for (TopicPartition tp : committedOffsets.keySet()) {
    long committed = committedOffsets.get(tp).offset();
    long end = endOffsets.get(tp).offset();
    long lag = end - committed;
}
```

### 1.3 메시지 조회

AdminClient로는 메시지 조회 불가. KafkaConsumer 필요.

```java
// 임시 컨슈머로 메시지 조회
Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-lens-temp-" + UUID.randomUUID());
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props)) {
    TopicPartition tp = new TopicPartition(topic, partition);
    consumer.assign(List.of(tp));
    consumer.seek(tp, offset);

    ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(5));
    // 메시지 처리...
}
```

## 2. Spring Boot + Kafka 통합

### 2.1 의존성 (Maven Central)

```xml
<!-- Spring Kafka (AdminClient 포함) -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Kafka Clients (직접 사용 시) -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
</dependency>
```

### 2.2 AdminClient Bean 설정

```java
@Configuration
public class KafkaAdminConfig {

    @Bean
    public AdminClient adminClient(ClusterProperties cluster) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                    cluster.getBootstrapServers());
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

        // 추가 보안 설정
        configs.putAll(cluster.getProperties());

        return AdminClient.create(configs);
    }
}
```

### 2.3 멀티 클러스터 지원

```java
@Component
public class AdminClientFactory {
    private final Map<String, AdminClient> clients = new ConcurrentHashMap<>();

    public AdminClient getClient(String clusterId) {
        return clients.computeIfAbsent(clusterId, this::createClient);
    }

    private AdminClient createClient(String clusterId) {
        ClusterConfig config = clusterRepository.findById(clusterId)
            .orElseThrow(() -> new ClusterNotFoundException(clusterId));
        return AdminClient.create(config.toProperties());
    }
}
```

## 3. 프론트엔드 기술 스택

### 3.1 Next.js 14 App Router

- Server Components 기본
- Client Components 필요 시 `"use client"` 선언
- API Routes → `/app/api/` 디렉토리

### 3.2 SWR 데이터 페칭

```typescript
// hooks/useTopics.ts
import useSWR from 'swr';

const fetcher = (url: string) => fetch(url).then(res => res.json());

export function useTopics(clusterId: string) {
    const { data, error, isLoading, mutate } = useSWR(
        `/api/v1/clusters/${clusterId}/topics`,
        fetcher,
        { refreshInterval: 30000 } // 30초마다 갱신
    );

    return {
        topics: data?.data ?? [],
        isLoading,
        isError: error,
        refresh: mutate
    };
}
```

### 3.3 shadcn/ui 컴포넌트

```bash
# 필요한 컴포넌트 설치
npx shadcn-ui@latest add button
npx shadcn-ui@latest add table
npx shadcn-ui@latest add card
npx shadcn-ui@latest add tabs
npx shadcn-ui@latest add badge
npx shadcn-ui@latest add dialog
```

## 4. 테스트 전략

### 4.1 백엔드 테스트

**단위 테스트 (Mockito)**
```java
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {
    @Mock
    private AdminClientWrapper adminClient;

    @InjectMocks
    private TopicService topicService;

    @Test
    void listTopics_returnsTopicList() {
        // given
        when(adminClient.listTopics()).thenReturn(Set.of("topic1", "topic2"));

        // when
        List<Topic> topics = topicService.listTopics();

        // then
        assertThat(topics).hasSize(2);
    }
}
```

**통합 테스트 (TestContainers)**
```java
@SpringBootTest
@Testcontainers
class TopicControllerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getTopics_returnsEmptyList() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/clusters/test/topics",
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 4.2 프론트엔드 테스트

**컴포넌트 테스트 (Vitest)**
```typescript
import { render, screen } from '@testing-library/react';
import { TopicList } from './TopicList';

test('renders topic list', () => {
    const topics = [
        { name: 'topic1', partitionCount: 3 },
        { name: 'topic2', partitionCount: 5 }
    ];

    render(<TopicList topics={topics} />);

    expect(screen.getByText('topic1')).toBeInTheDocument();
    expect(screen.getByText('topic2')).toBeInTheDocument();
});
```

**E2E 테스트 (Playwright)**
```typescript
import { test, expect } from '@playwright/test';

test('login and view topics', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('/');
    await page.click('text=dev-cluster');
    await page.click('text=Topics');

    await expect(page.locator('table')).toBeVisible();
});
```

## 5. 보안 고려사항

### 5.1 Basic Authentication

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties props) {
        List<UserDetails> users = props.getUsers().stream()
            .map(u -> User.withUsername(u.getUsername())
                         .password(passwordEncoder().encode(u.getPassword()))
                         .roles(u.getRoles().toArray(new String[0]))
                         .build())
            .toList();

        return new InMemoryUserDetailsManager(users);
    }
}
```

### 5.2 Kafka 보안 설정

```yaml
# clusters.yml - SASL/SSL 예시
clusters:
  - id: prod-cluster
    bootstrapServers: "kafka-prod:9093"
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USER}"
        password="${KAFKA_PASSWORD}";
      ssl.truststore.location: "/path/to/truststore.jks"
      ssl.truststore.password: "${TRUSTSTORE_PASSWORD}"
```

## 6. 성능 최적화

### 6.1 AdminClient 캐싱

- 클러스터당 하나의 AdminClient 인스턴스 유지
- 연결 재사용으로 오버헤드 감소

### 6.2 응답 캐싱

```java
@Cacheable(value = "topics", key = "#clusterId")
public List<Topic> listTopics(String clusterId) {
    // ...
}
```

### 6.3 페이지네이션

- 토픽 목록: 서버 사이드 페이지네이션
- 메시지 조회: limit 파라미터 (최대 1000)

## References

- [Kafka AdminClient JavaDoc](https://kafka.apache.org/36/javadoc/org/apache/kafka/clients/admin/Admin.html)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Baeldung - Monitor Consumer Lag](https://www.baeldung.com/java-kafka-consumer-lag)
- [Next.js 14 Documentation](https://nextjs.org/docs)
- [SWR Documentation](https://swr.vercel.app/)
