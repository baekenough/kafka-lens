package com.kafkalens.domain.broker;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 브로커 서비스.
 *
 * <p>브로커 관련 비즈니스 로직을 처리합니다.
 * 브로커 목록 조회, 상태 확인 등의 기능을 제공합니다.</p>
 */
@Service
public class BrokerService {

    private static final Logger log = LoggerFactory.getLogger(BrokerService.class);

    private final AdminClientWrapper adminClientWrapper;
    private final ClusterService clusterService;

    /**
     * BrokerService 생성자.
     *
     * @param adminClientWrapper Kafka AdminClient 래퍼
     * @param clusterService     클러스터 서비스
     */
    public BrokerService(AdminClientWrapper adminClientWrapper, ClusterService clusterService) {
        this.adminClientWrapper = adminClientWrapper;
        this.clusterService = clusterService;
    }

    /**
     * 클러스터의 브로커 목록을 조회합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 브로커 목록 (ID 오름차순 정렬)
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     */
    public List<Broker> listBrokers(String clusterId) {
        log.debug("Listing brokers for cluster: {}", clusterId);

        validateClusterExists(clusterId);

        AdminClientWrapper.ClusterInfo clusterInfo = adminClientWrapper.describeCluster(clusterId);

        if (clusterInfo.nodes() == null || clusterInfo.nodes().isEmpty()) {
            log.debug("No brokers found for cluster: {}", clusterId);
            return List.of();
        }

        int controllerId = clusterInfo.controller() != null ? clusterInfo.controller().id() : -1;

        List<Broker> brokers = clusterInfo.nodes().stream()
                .map(node -> toBroker(node, controllerId))
                .sorted(Comparator.comparingInt(Broker::id))
                .collect(Collectors.toList());

        log.info("Found {} brokers for cluster: {}", brokers.size(), clusterId);
        return brokers;
    }

    // === Private Helper Methods ===

    /**
     * 클러스터 존재 여부를 검증합니다.
     */
    private void validateClusterExists(String clusterId) {
        if (!clusterService.existsById(clusterId)) {
            throw new ClusterNotFoundException(clusterId);
        }
    }

    /**
     * Kafka Node를 Broker 도메인 객체로 변환합니다.
     *
     * @param node         Kafka Node
     * @param controllerId 컨트롤러 브로커 ID
     * @return Broker 도메인 객체
     */
    private Broker toBroker(Node node, int controllerId) {
        return Broker.builder()
                .id(node.id())
                .host(node.host())
                .port(node.port())
                .rack(node.rack())
                .isController(node.id() == controllerId)
                .build();
    }
}
