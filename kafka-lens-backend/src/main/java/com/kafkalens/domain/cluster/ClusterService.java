package com.kafkalens.domain.cluster;

import com.kafkalens.api.v1.dto.ConnectionTestResult;
import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.infrastructure.kafka.AdminClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 클러스터 서비스.
 *
 * <p>클러스터 관련 비즈니스 로직을 처리합니다.
 * 클러스터 조회, 연결 테스트 등의 기능을 제공합니다.</p>
 */
@Service
public class ClusterService {

    private static final Logger log = LoggerFactory.getLogger(ClusterService.class);

    private final ClusterRepository clusterRepository;
    private final AdminClientFactory adminClientFactory;

    /**
     * ClusterService 생성자.
     *
     * @param clusterRepository  클러스터 저장소
     * @param adminClientFactory AdminClient 팩토리
     */
    public ClusterService(ClusterRepository clusterRepository, AdminClientFactory adminClientFactory) {
        this.clusterRepository = clusterRepository;
        this.adminClientFactory = adminClientFactory;
    }

    /**
     * 모든 클러스터를 조회합니다.
     *
     * @return 클러스터 목록
     */
    public List<Cluster> findAll() {
        log.debug("Finding all clusters");
        return clusterRepository.findAll();
    }

    /**
     * ID로 클러스터를 조회합니다.
     *
     * @param id 클러스터 ID
     * @return 클러스터
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     */
    public Cluster findById(String id) {
        log.debug("Finding cluster by id: {}", id);
        return clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));
    }

    /**
     * 클러스터가 존재하는지 확인합니다.
     *
     * @param id 클러스터 ID
     * @return 존재하면 true
     */
    public boolean existsById(String id) {
        return clusterRepository.existsById(id);
    }

    /**
     * 환경별 클러스터를 조회합니다.
     *
     * @param environment 환경 (development, staging, production)
     * @return 해당 환경의 클러스터 목록
     */
    public List<Cluster> findByEnvironment(String environment) {
        log.debug("Finding clusters by environment: {}", environment);
        return clusterRepository.findByEnvironment(environment);
    }

    /**
     * 클러스터 연결을 테스트합니다.
     *
     * <p>Kafka 브로커에 연결을 시도하고 결과를 반환합니다.
     * 응답 시간도 측정하여 결과에 포함합니다.</p>
     *
     * @param id 클러스터 ID
     * @return 연결 테스트 결과
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     */
    public ConnectionTestResult testConnection(String id) {
        log.info("Testing connection to cluster: {}", id);

        // 클러스터 존재 확인
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));

        long startTime = System.currentTimeMillis();

        try {
            boolean connected = adminClientFactory.testConnection(id);
            long responseTime = System.currentTimeMillis() - startTime;

            if (connected) {
                log.info("Connection test succeeded for cluster: {} ({}ms)", id, responseTime);
                return ConnectionTestResult.success(
                        cluster.id(),
                        cluster.name(),
                        responseTime
                );
            } else {
                log.warn("Connection test failed for cluster: {}", id);
                return ConnectionTestResult.failure(
                        cluster.id(),
                        cluster.name(),
                        responseTime,
                        "Connection test failed - unable to connect to Kafka brokers"
                );
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Connection test error for cluster {}: {}", id, e.getMessage());
            return ConnectionTestResult.failure(
                    cluster.id(),
                    cluster.name(),
                    responseTime,
                    "Connection error: " + e.getMessage()
            );
        }
    }

    /**
     * 클러스터 설정을 다시 로드합니다.
     */
    public void reloadClusters() {
        log.info("Reloading cluster configuration");
        clusterRepository.reload();
    }
}
