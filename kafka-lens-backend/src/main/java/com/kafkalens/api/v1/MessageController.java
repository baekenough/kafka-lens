package com.kafkalens.api.v1;

import com.kafkalens.common.ApiResponse;
import com.kafkalens.domain.message.KafkaMessage;
import com.kafkalens.domain.message.MessageFetchRequest;
import com.kafkalens.domain.message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 메시지 API 컨트롤러.
 *
 * <p>Kafka 토픽 메시지 조회 REST API 엔드포인트를 제공합니다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/topics/{topicName}/messages - 메시지 조회</li>
 * </ul>
 *
 * <p>Constitution: 메시지 조회는 최대 1000건으로 제한됩니다.</p>
 */
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/topics/{topicName}/messages")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;

    /**
     * MessageController 생성자.
     *
     * @param messageService 메시지 서비스
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 토픽에서 메시지를 조회합니다.
     *
     * <p>지정된 파티션과 오프셋에서 메시지를 조회합니다.
     * 최대 조회 제한은 1000건입니다 (Constitution).</p>
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @param partition 파티션 번호 (기본값: 0)
     * @param offset    시작 오프셋 (기본값: 0)
     * @param limit     조회 제한 (기본값: 100, 최대: 1000)
     * @return 메시지 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KafkaMessage>>> getMessages(
            @PathVariable String clusterId,
            @PathVariable String topicName,
            @RequestParam(defaultValue = "0") Integer partition,
            @RequestParam(defaultValue = "0") Long offset,
            @RequestParam(defaultValue = "100") Integer limit) {

        log.debug("GET /api/v1/clusters/{}/topics/{}/messages?partition={}&offset={}&limit={}",
                clusterId, topicName, partition, offset, limit);

        MessageFetchRequest request = MessageFetchRequest.builder()
                .topicName(topicName)
                .partition(partition)
                .offset(offset)
                .limit(limit)
                .build();

        List<KafkaMessage> messages = messageService.fetchMessages(clusterId, request);

        return ResponseEntity.ok(ApiResponse.ok(messages));
    }
}
