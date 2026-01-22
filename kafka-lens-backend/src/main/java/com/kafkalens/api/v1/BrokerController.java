package com.kafkalens.api.v1;

import com.kafkalens.common.ApiResponse;
import com.kafkalens.domain.broker.Broker;
import com.kafkalens.domain.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 브로커 API 컨트롤러.
 *
 * <p>브로커 관련 REST API 엔드포인트를 제공합니다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/clusters/{clusterId}/brokers - 브로커 목록 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/brokers")
public class BrokerController {

    private static final Logger log = LoggerFactory.getLogger(BrokerController.class);

    private final BrokerService brokerService;

    /**
     * BrokerController 생성자.
     *
     * @param brokerService 브로커 서비스
     */
    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    /**
     * 클러스터의 모든 브로커 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 브로커 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Broker>>> getBrokers(@PathVariable String clusterId) {
        log.debug("GET /api/v1/clusters/{}/brokers", clusterId);

        List<Broker> brokers = brokerService.listBrokers(clusterId);

        return ResponseEntity.ok(ApiResponse.ok(brokers));
    }
}
