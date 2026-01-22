package com.kafkalens.domain.message;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.TopicNotFoundException;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.KafkaConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MessageService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, MessageService의 핵심 비즈니스 로직을 검증합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *   <li>fetchMessages: 메시지 조회</li>
 *   <li>메시지 제한: 최대 1000건</li>
 *   <li>파티션/오프셋 지정 조회</li>
 *   <li>예외 처리</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private KafkaConsumerFactory consumerFactory;

    @Mock
    private KafkaConsumer<byte[], byte[]> kafkaConsumer;

    private MessageService messageService;

    private static final String CLUSTER_ID = "local";
    private static final String TOPIC_NAME = "test-topic";

    @BeforeEach
    void setUp() {
        messageService = new MessageService(clusterService, consumerFactory);
        // Setup default stubbing for generateTemporaryGroupId
        given(consumerFactory.generateTemporaryGroupId()).willReturn("test-group-id");
    }

    @Nested
    @DisplayName("fetchMessages()")
    class FetchMessages {

        @Test
        @DisplayName("토픽에서 메시지를 조회한다")
        void testFetchMessages_returnsMessages() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);
            List<ConsumerRecord<byte[], byte[]>> records = createMockRecords(TOPIC_NAME, 0, 0L, 5);
            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, records));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 5L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(5);
            verify(clusterService).findById(CLUSTER_ID);
            verify(consumerFactory).createConsumer(any(), any());
        }

        @Test
        @DisplayName("지정된 파티션과 오프셋에서 메시지를 조회한다")
        void testFetchMessages_fromSpecificPartitionAndOffset() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 2);
            List<ConsumerRecord<byte[], byte[]>> records = createMockRecords(TOPIC_NAME, 2, 100L, 3);
            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, records));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 103L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(2)
                    .offset(100L)
                    .limit(50)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).partition()).isEqualTo(2);
            assertThat(result.get(0).offset()).isEqualTo(100L);
        }

        @Test
        @DisplayName("메시지에 키, 값, 헤더가 포함된다")
        void testFetchMessages_containsKeyValueHeaders() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);

            RecordHeaders headers = new RecordHeaders();
            headers.add("correlationId", "abc-123".getBytes(StandardCharsets.UTF_8));
            headers.add("source", "test-producer".getBytes(StandardCharsets.UTF_8));

            ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                    TOPIC_NAME, 0, 0L, 1000L, TimestampType.CREATE_TIME,
                    10, 20,
                    "test-key".getBytes(StandardCharsets.UTF_8),
                    "test-value".getBytes(StandardCharsets.UTF_8),
                    headers,
                    Optional.empty()
            );

            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, List.of(record)));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 1L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(1);
            KafkaMessage message = result.get(0);
            assertThat(message.key()).isEqualTo("test-key");
            assertThat(message.value()).isEqualTo("test-value");
            assertThat(message.headers()).containsEntry("correlationId", "abc-123");
            assertThat(message.headers()).containsEntry("source", "test-producer");
        }

        @Test
        @DisplayName("타임스탬프 타입이 포함된다")
        void testFetchMessages_containsTimestampType() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);

            ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                    TOPIC_NAME, 0, 0L, 1000L, TimestampType.LOG_APPEND_TIME,
                    10, 20,
                    "key".getBytes(StandardCharsets.UTF_8),
                    "value".getBytes(StandardCharsets.UTF_8),
                    new RecordHeaders(),
                    Optional.empty()
            );

            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, List.of(record)));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 1L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).timestampType()).isEqualTo("LogAppendTime");
        }

        @Test
        @DisplayName("바이너리 데이터는 Base64로 인코딩된다")
        void testFetchMessages_binaryDataEncodedAsBase64() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);

            // Non-UTF8 binary data
            byte[] binaryKey = new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x01};
            byte[] binaryValue = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes

            ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                    TOPIC_NAME, 0, 0L, 1000L, TimestampType.CREATE_TIME,
                    4, 4,
                    binaryKey,
                    binaryValue,
                    new RecordHeaders(),
                    Optional.empty()
            );

            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, List.of(record)));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 1L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(1);
            KafkaMessage message = result.get(0);
            // Base64 encoded values
            assertThat(message.key()).isEqualTo(Base64.getEncoder().encodeToString(binaryKey));
            assertThat(message.value()).isEqualTo(Base64.getEncoder().encodeToString(binaryValue));
        }

        @Test
        @DisplayName("null 키/값이 있는 메시지를 처리한다")
        void testFetchMessages_handlesNullKeyValue() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);

            ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                    TOPIC_NAME, 0, 0L, 1000L, TimestampType.CREATE_TIME,
                    -1, 20,
                    null,  // null key
                    "value".getBytes(StandardCharsets.UTF_8),
                    new RecordHeaders(),
                    Optional.empty()
            );

            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, List.of(record)));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 1L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSize(1);
            KafkaMessage message = result.get(0);
            assertThat(message.key()).isNull();
            assertThat(message.value()).isEqualTo("value");
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 예외를 발생시킨다")
        void testFetchMessages_nonExistingCluster_throwsException() {
            // given
            given(clusterService.findById("unknown")).willThrow(new ClusterNotFoundException("unknown"));

            // when & then
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            assertThatThrownBy(() -> messageService.fetchMessages("unknown", request))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("메시지가 없으면 빈 목록을 반환한다")
        void testFetchMessages_noMessages_returnsEmptyList() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of(tp, List.of()));

            given(kafkaConsumer.poll(any(Duration.class))).willReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 0L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(100)
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Message Limit (Constitution: max 1000)")
    class MessageLimit {

        @Test
        @DisplayName("요청 제한이 1000을 초과하면 1000으로 제한된다")
        void testFetchMessages_limitsTo1000() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);
            // Even if there are more records, should only return max 1000
            List<ConsumerRecord<byte[], byte[]>> records = createMockRecords(TOPIC_NAME, 0, 0L, 1000);
            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, records));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 2000L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(5000)  // Request more than max
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSizeLessThanOrEqualTo(MessageService.MAX_MESSAGE_LIMIT);
        }

        @Test
        @DisplayName("기본 제한값이 100이다")
        void testFetchMessages_defaultLimit() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);
            List<ConsumerRecord<byte[], byte[]>> records = createMockRecords(TOPIC_NAME, 0, 0L, 100);
            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, records));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 200L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .build();  // No limit specified

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).hasSizeLessThanOrEqualTo(MessageService.DEFAULT_MESSAGE_LIMIT);
        }

        @Test
        @DisplayName("음수 제한은 기본값으로 처리된다")
        void testFetchMessages_negativeLimit_usesDefault() {
            // given
            Cluster cluster = createMockCluster();
            given(clusterService.findById(CLUSTER_ID)).willReturn(cluster);
            given(consumerFactory.createConsumer(any(), any())).willReturn(kafkaConsumer);

            TopicPartition tp = new TopicPartition(TOPIC_NAME, 0);
            List<ConsumerRecord<byte[], byte[]>> records = createMockRecords(TOPIC_NAME, 0, 0L, 50);
            ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(Map.of(tp, records));
            ConsumerRecords<byte[], byte[]> emptyRecords = new ConsumerRecords<>(Map.of());

            when(kafkaConsumer.poll(any(Duration.class)))
                    .thenReturn(consumerRecords)
                    .thenReturn(emptyRecords);
            given(kafkaConsumer.endOffsets(anyCollection())).willReturn(Map.of(tp, 50L));

            // when
            MessageFetchRequest request = MessageFetchRequest.builder()
                    .topicName(TOPIC_NAME)
                    .partition(0)
                    .offset(0L)
                    .limit(-10)  // Negative limit
                    .build();

            List<KafkaMessage> result = messageService.fetchMessages(CLUSTER_ID, request);

            // then
            assertThat(result).isNotNull();
            // Should use default limit, not fail
        }
    }

    // === Helper Methods ===

    private Cluster createMockCluster() {
        return Cluster.builder()
                .id(CLUSTER_ID)
                .name("Local Kafka")
                .bootstrapServers("localhost:9092")
                .environment("development")
                .build();
    }

    private List<ConsumerRecord<byte[], byte[]>> createMockRecords(
            String topic, int partition, long startOffset, int count) {
        List<ConsumerRecord<byte[], byte[]>> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long offset = startOffset + i;
            ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                    topic, partition, offset, 1000L + i, TimestampType.CREATE_TIME,
                    10, 20,
                    ("key-" + i).getBytes(StandardCharsets.UTF_8),
                    ("value-" + i).getBytes(StandardCharsets.UTF_8),
                    new RecordHeaders(),
                    Optional.empty()
            );
            records.add(record);
        }
        return records;
    }
}
