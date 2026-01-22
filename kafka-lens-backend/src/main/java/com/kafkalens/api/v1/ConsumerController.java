package com.kafkalens.api.v1;

import com.kafkalens.api.v1.dto.ConsumerLagResponse;
import com.kafkalens.common.ApiResponse;
import com.kafkalens.domain.consumer.ConsumerGroup;
import com.kafkalens.domain.consumer.ConsumerLagService;
import com.kafkalens.domain.consumer.ConsumerLagSummary;
import com.kafkalens.domain.consumer.ConsumerMember;
import com.kafkalens.domain.consumer.ConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 컨슈머 그룹 API 컨트롤러.
 *
 * <p>컨슈머 그룹 관련 REST API 엔드포인트를 제공합니다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/consumer-groups - 컨슈머 그룹 목록 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/consumer-groups/{groupId} - 컨슈머 그룹 상세 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/consumer-groups/{groupId}/members - 멤버 목록 조회</li>
 *   <li>GET /api/v1/clusters/{clusterId}/consumer-groups/{groupId}/lag - Lag 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/consumer-groups")
public class ConsumerController {

    private static final Logger log = LoggerFactory.getLogger(ConsumerController.class);

    private final ConsumerService consumerService;
    private final ConsumerLagService consumerLagService;

    /**
     * ConsumerController 생성자.
     *
     * @param consumerService    컨슈머 서비스
     * @param consumerLagService 컨슈머 Lag 서비스
     */
    public ConsumerController(ConsumerService consumerService, ConsumerLagService consumerLagService) {
        this.consumerService = consumerService;
        this.consumerLagService = consumerLagService;
    }

    /**
     * 클러스터의 모든 컨슈머 그룹 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 컨슈머 그룹 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumerGroup>>> getAllConsumerGroups(
            @PathVariable String clusterId) {
        log.debug("GET /api/v1/clusters/{}/consumer-groups", clusterId);

        List<ConsumerGroup> groups = consumerService.listGroups(clusterId);

        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    /**
     * 특정 컨슈머 그룹의 상세 정보를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   그룹 ID
     * @return 컨슈머 그룹 상세 정보
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<ConsumerGroup>> getConsumerGroupById(
            @PathVariable String clusterId,
            @PathVariable String groupId) {
        log.debug("GET /api/v1/clusters/{}/consumer-groups/{}", clusterId, groupId);

        ConsumerGroup group = consumerService.getGroup(clusterId, groupId);

        return ResponseEntity.ok(ApiResponse.ok(group));
    }

    /**
     * 컨슈머 그룹의 멤버 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   그룹 ID
     * @return 멤버 목록
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<ConsumerMember>>> getConsumerGroupMembers(
            @PathVariable String clusterId,
            @PathVariable String groupId) {
        log.debug("GET /api/v1/clusters/{}/consumer-groups/{}/members", clusterId, groupId);

        List<ConsumerMember> members = consumerService.getMembers(clusterId, groupId);

        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    /**
     * 컨슈머 그룹의 Lag를 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @param groupId   컨슈머 그룹 ID
     * @return Lag 정보
     */
    @GetMapping("/{groupId}/lag")
    public ResponseEntity<ApiResponse<ConsumerLagResponse>> getLag(
            @PathVariable String clusterId,
            @PathVariable String groupId
    ) {
        log.debug("GET /api/v1/clusters/{}/consumer-groups/{}/lag", clusterId, groupId);

        ConsumerLagSummary summary = consumerLagService.getLagSummary(clusterId, groupId);
        ConsumerLagResponse response = ConsumerLagResponse.from(summary);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
