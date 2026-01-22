package com.kafkalens.api.v1;

import com.kafkalens.api.v1.dto.ClusterResponse;
import com.kafkalens.api.v1.dto.ConnectionTestResult;
import com.kafkalens.common.ApiResponse;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클러스터 API 컨트롤러.
 *
 * <p>클러스터 관련 REST API 엔드포인트를 제공합니다.</p>
 *
 * <ul>
 *   <li>GET /api/v1/clusters - 클러스터 목록 조회</li>
 *   <li>GET /api/v1/clusters/{id} - 클러스터 상세 조회</li>
 *   <li>POST /api/v1/clusters/{id}/test - 연결 테스트</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clusters")
public class ClusterController {

    private static final Logger log = LoggerFactory.getLogger(ClusterController.class);

    private final ClusterService clusterService;

    /**
     * ClusterController 생성자.
     *
     * @param clusterService 클러스터 서비스
     */
    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    /**
     * 모든 클러스터 목록을 조회합니다.
     *
     * @return 클러스터 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClusterResponse>>> getAllClusters() {
        log.debug("GET /api/v1/clusters");

        List<Cluster> clusters = clusterService.findAll();
        List<ClusterResponse> response = ClusterResponse.fromList(clusters);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * ID로 클러스터를 조회합니다.
     *
     * @param id 클러스터 ID
     * @return 클러스터 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClusterResponse>> getClusterById(@PathVariable String id) {
        log.debug("GET /api/v1/clusters/{}", id);

        Cluster cluster = clusterService.findById(id);
        ClusterResponse response = ClusterResponse.from(cluster);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 클러스터 연결을 테스트합니다.
     *
     * @param id 클러스터 ID
     * @return 연결 테스트 결과
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<ConnectionTestResult>> testConnection(@PathVariable String id) {
        log.info("POST /api/v1/clusters/{}/test", id);

        ConnectionTestResult result = clusterService.testConnection(id);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 환경별 클러스터 목록을 조회합니다.
     *
     * @param environment 환경 (development, staging, production)
     * @return 해당 환경의 클러스터 목록
     */
    @GetMapping(params = "environment")
    public ResponseEntity<ApiResponse<List<ClusterResponse>>> getClustersByEnvironment(
            @RequestParam String environment) {
        log.debug("GET /api/v1/clusters?environment={}", environment);

        List<Cluster> clusters = clusterService.findByEnvironment(environment);
        List<ClusterResponse> response = ClusterResponse.fromList(clusters);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
