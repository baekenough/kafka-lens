package com.kafkalens.api.v1;

import com.kafkalens.common.ApiResponse;
import com.kafkalens.domain.topic.PartitionInfo;
import com.kafkalens.domain.topic.Topic;
import com.kafkalens.domain.topic.TopicDetail;
import com.kafkalens.domain.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 토픽 API 컨트롤러.
 *
 * <p>토픽 관련 REST API 엔드포인트를 제공합니다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/topics - 토픽 목록 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/topics/{topicName} - 토픽 상세 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/topics/{topicName}/partitions - 파티션 목록 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/topics")
public class TopicController {

    private static final Logger log = LoggerFactory.getLogger(TopicController.class);

    private final TopicService topicService;

    /**
     * TopicController 생성자.
     *
     * @param topicService 토픽 서비스
     */
    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * 클러스터의 모든 토픽 목록을 조회합니다.
     *
     * @param clusterId       클러스터 ID
     * @param includeInternal 내부 토픽 포함 여부 (기본값: false)
     * @return 토픽 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Topic>>> getTopics(
            @PathVariable String clusterId,
            @RequestParam(defaultValue = "false") boolean includeInternal) {
        log.debug("GET /api/v1/clusters/{}/topics?includeInternal={}", clusterId, includeInternal);

        List<Topic> topics = topicService.listTopics(clusterId, includeInternal);

        return ResponseEntity.ok(ApiResponse.ok(topics));
    }

    /**
     * 토픽 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @return 토픽 상세 정보
     */
    @GetMapping("/{topicName}")
    public ResponseEntity<ApiResponse<TopicDetail>> getTopic(
            @PathVariable String clusterId,
            @PathVariable String topicName) {
        log.debug("GET /api/v1/clusters/{}/topics/{}", clusterId, topicName);

        TopicDetail topic = topicService.getTopic(clusterId, topicName);

        return ResponseEntity.ok(ApiResponse.ok(topic));
    }

    /**
     * 토픽의 파티션 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 토픽 이름
     * @return 파티션 목록
     */
    @GetMapping("/{topicName}/partitions")
    public ResponseEntity<ApiResponse<List<PartitionInfo>>> getTopicPartitions(
            @PathVariable String clusterId,
            @PathVariable String topicName) {
        log.debug("GET /api/v1/clusters/{}/topics/{}/partitions", clusterId, topicName);

        List<PartitionInfo> partitions = topicService.getTopicPartitions(clusterId, topicName);

        return ResponseEntity.ok(ApiResponse.ok(partitions));
    }
}
