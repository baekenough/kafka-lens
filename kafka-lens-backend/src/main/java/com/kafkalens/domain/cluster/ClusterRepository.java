package com.kafkalens.domain.cluster;

import java.util.List;
import java.util.Optional;

/**
 * 클러스터 저장소 인터페이스.
 *
 * <p>클러스터 설정을 조회하는 메서드를 정의합니다.</p>
 */
public interface ClusterRepository {

    /**
     * 모든 클러스터를 조회합니다.
     *
     * @return 클러스터 목록
     */
    List<Cluster> findAll();

    /**
     * ID로 클러스터를 조회합니다.
     *
     * @param id 클러스터 ID
     * @return 클러스터 (없으면 빈 Optional)
     */
    Optional<Cluster> findById(String id);

    /**
     * 클러스터가 존재하는지 확인합니다.
     *
     * @param id 클러스터 ID
     * @return 존재하면 true
     */
    boolean existsById(String id);

    /**
     * 환경별 클러스터를 조회합니다.
     *
     * @param environment 환경 (development, staging, production)
     * @return 해당 환경의 클러스터 목록
     */
    List<Cluster> findByEnvironment(String environment);

    /**
     * 설정을 다시 로드합니다.
     */
    void reload();
}
