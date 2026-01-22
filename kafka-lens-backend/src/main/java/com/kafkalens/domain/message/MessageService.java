package com.kafkalens.domain.message;

import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.KafkaConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

/**
 * 메시지 서비스.
 *
 * <p>Kafka 토픽에서 메시지를 조회하는 비즈니스 로직을 처리합니다.
 * Constitution에 따라 최대 1000건의 메시지만 조회할 수 있습니다.</p>
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    /**
     * 최대 메시지 조회 제한 (Constitution).
     */
    public static final int MAX_MESSAGE_LIMIT = 1000;

    /**
     * 기본 메시지 조회 제한.
     */
    public static final int DEFAULT_MESSAGE_LIMIT = 100;

    /**
     * 메시지 폴링 타임아웃.
     */
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private final ClusterService clusterService;
    private final KafkaConsumerFactory consumerFactory;

    /**
     * MessageService 생성자.
     *
     * @param clusterService  클러스터 서비스
     * @param consumerFactory Kafka Consumer 팩토리
     */
    public MessageService(ClusterService clusterService, KafkaConsumerFactory consumerFactory) {
        this.clusterService = clusterService;
        this.consumerFactory = consumerFactory;
    }

    /**
     * 토픽에서 메시지를 조회합니다.
     *
     * <p>지정된 파티션과 오프셋에서 메시지를 조회합니다.
     * 최대 조회 제한은 1000건입니다 (Constitution).</p>
     *
     * @param clusterId 클러스터 ID
     * @param request   메시지 조회 요청
     * @return 메시지 목록
     */
    public List<KafkaMessage> fetchMessages(String clusterId, MessageFetchRequest request) {
        log.debug("Fetching messages from cluster: {}, topic: {}, partition: {}, offset: {}, limit: {}",
                clusterId, request.topicName(), request.partition(), request.offset(), request.limit());

        Cluster cluster = clusterService.findById(clusterId);
        int effectiveLimit = calculateEffectiveLimit(request.limit());

        String groupId = consumerFactory.generateTemporaryGroupId();

        try (KafkaConsumer<byte[], byte[]> consumer = consumerFactory.createConsumer(cluster, groupId)) {
            return fetchMessagesFromPartition(consumer, request, effectiveLimit);
        }
    }

    /**
     * 파티션에서 메시지를 조회합니다.
     */
    private List<KafkaMessage> fetchMessagesFromPartition(
            KafkaConsumer<byte[], byte[]> consumer,
            MessageFetchRequest request,
            int limit) {

        TopicPartition tp = new TopicPartition(request.topicName(), request.partition());
        consumer.assign(List.of(tp));

        // 오프셋 설정
        long startOffset = request.offset() != null ? request.offset() : 0L;
        consumer.seek(tp, startOffset);

        // 끝 오프셋 조회
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(List.of(tp));
        long endOffset = endOffsets.getOrDefault(tp, 0L);

        // 조회할 메시지가 없으면 빈 목록 반환
        if (startOffset >= endOffset) {
            return List.of();
        }

        List<KafkaMessage> messages = new ArrayList<>();
        int remainingMessages = limit;
        int emptyPollCount = 0;
        int maxEmptyPolls = 3;

        while (remainingMessages > 0 && emptyPollCount < maxEmptyPolls) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(POLL_TIMEOUT);

            if (records.isEmpty()) {
                emptyPollCount++;
                continue;
            }

            emptyPollCount = 0;

            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (remainingMessages <= 0) {
                    break;
                }

                messages.add(convertToKafkaMessage(record));
                remainingMessages--;
            }
        }

        log.debug("Fetched {} messages from topic: {}, partition: {}",
                messages.size(), request.topicName(), request.partition());

        return messages;
    }

    /**
     * ConsumerRecord를 KafkaMessage로 변환합니다.
     */
    private KafkaMessage convertToKafkaMessage(ConsumerRecord<byte[], byte[]> record) {
        return KafkaMessage.builder()
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .timestamp(record.timestamp())
                .timestampType(formatTimestampType(record.timestampType()))
                .key(convertToString(record.key()))
                .value(convertToString(record.value()))
                .headers(convertHeaders(record.headers()))
                .build();
    }

    /**
     * 바이트 배열을 문자열로 변환합니다.
     * UTF-8로 디코딩할 수 없으면 Base64로 인코딩합니다.
     */
    private String convertToString(byte[] data) {
        if (data == null) {
            return null;
        }

        if (isValidUtf8(data)) {
            return new String(data, StandardCharsets.UTF_8);
        } else {
            return Base64.getEncoder().encodeToString(data);
        }
    }

    /**
     * 바이트 배열이 유효한 UTF-8인지 확인합니다.
     */
    private boolean isValidUtf8(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 헤더를 Map으로 변환합니다.
     */
    private Map<String, String> convertHeaders(Iterable<Header> headers) {
        Map<String, String> headerMap = new LinkedHashMap<>();

        for (Header header : headers) {
            String value = header.value() != null
                    ? convertToString(header.value())
                    : null;
            headerMap.put(header.key(), value);
        }

        return headerMap;
    }

    /**
     * TimestampType을 문자열로 변환합니다.
     */
    private String formatTimestampType(TimestampType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case CREATE_TIME -> "CreateTime";
            case LOG_APPEND_TIME -> "LogAppendTime";
            case NO_TIMESTAMP_TYPE -> "NoTimestampType";
        };
    }

    /**
     * 유효한 제한값을 계산합니다.
     * Constitution: 최대 1000건
     */
    private int calculateEffectiveLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_MESSAGE_LIMIT;
        }

        return Math.min(requestedLimit, MAX_MESSAGE_LIMIT);
    }
}
