package com.kafkalens.infrastructure.kafka;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.common.exception.KafkaConnectionException;
import com.kafkalens.domain.cluster.Cluster;
import com.kafkalens.domain.cluster.ClusterRepository;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka AdminClient 팩토리.
 *
 * <p>클러스터별 AdminClient 인스턴스를 생성하고 캐싱합니다.
 * 연결 실패 시 적절한 예외를 발생시킵니다.</p>
 */
@Component
public class AdminClientFactory {

    private static final Logger log = LoggerFactory.getLogger(AdminClientFactory.class);

    private final ClusterRepository clusterRepository;
    private final Map<String, AdminClient> clientCache = new ConcurrentHashMap<>();

    @Value("${kafka.admin.request-timeout-ms:30000}")
    private int requestTimeoutMs;

    @Value("${kafka.admin.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${kafka.admin.default-api-timeout-ms:60000}")
    private int defaultApiTimeoutMs;

    @Value("${kafka.admin.retries:3}")
    private int retries;

    @Value("${kafka.admin.retry-backoff-ms:1000}")
    private int retryBackoffMs;

    public AdminClientFactory(ClusterRepository clusterRepository) {
        this.clusterRepository = clusterRepository;
    }

    /**
     * 클러스터 ID로 AdminClient를 가져옵니다.
     * 캐시된 인스턴스가 있으면 반환하고, 없으면 새로 생성합니다.
     *
     * @param clusterId 클러스터 ID
     * @return AdminClient 인스턴스
     * @throws ClusterNotFoundException 클러스터를 찾을 수 없는 경우
     * @throws KafkaConnectionException 연결에 실패한 경우
     */
    public AdminClient getOrCreate(String clusterId) {
        return clientCache.computeIfAbsent(clusterId, this::createAdminClient);
    }

    /**
     * 클러스터 ID로 새 AdminClient를 생성합니다.
     * 기존 캐시된 인스턴스가 있으면 닫고 새로 생성합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 새 AdminClient 인스턴스
     */
    public AdminClient recreate(String clusterId) {
        closeClient(clusterId);
        return getOrCreate(clusterId);
    }

    /**
     * 특정 클러스터의 AdminClient를 닫습니다.
     *
     * @param clusterId 클러스터 ID
     */
    public void closeClient(String clusterId) {
        AdminClient client = clientCache.remove(clusterId);
        if (client != null) {
            log.info("Closing AdminClient for cluster: {}", clusterId);
            try {
                client.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.warn("Error closing AdminClient for cluster {}: {}", clusterId, e.getMessage());
            }
        }
    }

    /**
     * 특정 클러스터에 연결 테스트를 수행합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 연결 성공 시 true
     */
    public boolean testConnection(String clusterId) {
        try {
            AdminClient client = getOrCreate(clusterId);
            // 클러스터 ID 조회로 연결 테스트
            client.describeCluster().clusterId().get();
            return true;
        } catch (Exception e) {
            log.warn("Connection test failed for cluster {}: {}", clusterId, e.getMessage());
            closeClient(clusterId);
            return false;
        }
    }

    /**
     * 모든 캐시된 AdminClient를 닫습니다.
     */
    @PreDestroy
    public void closeAll() {
        log.info("Closing all AdminClient instances");
        clientCache.keySet().forEach(this::closeClient);
    }

    /**
     * 캐시된 클라이언트 수를 반환합니다.
     *
     * @return 캐시된 클라이언트 수
     */
    public int getCachedClientCount() {
        return clientCache.size();
    }

    /**
     * 클러스터가 캐시되어 있는지 확인합니다.
     *
     * @param clusterId 클러스터 ID
     * @return 캐시되어 있으면 true
     */
    public boolean isCached(String clusterId) {
        return clientCache.containsKey(clusterId);
    }

    // === Private Methods ===

    /**
     * 클러스터 ID로 AdminClient를 생성합니다.
     */
    private AdminClient createAdminClient(String clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ClusterNotFoundException(clusterId));

        return createAdminClient(cluster);
    }

    /**
     * Cluster 객체로 AdminClient를 생성합니다.
     */
    private AdminClient createAdminClient(Cluster cluster) {
        log.info("Creating AdminClient for cluster: {} ({})", cluster.id(), cluster.name());

        Properties props = buildProperties(cluster);

        try {
            return AdminClient.create(props);
        } catch (Exception e) {
            log.error("Failed to create AdminClient for cluster {}: {}", cluster.id(), e.getMessage());
            throw new KafkaConnectionException(cluster.id(), e);
        }
    }

    /**
     * 클러스터 설정으로 Properties를 빌드합니다.
     */
    private Properties buildProperties(Cluster cluster) {
        Properties props = new Properties();

        // 기본 설정
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServersAsString());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, defaultApiTimeoutMs);
        props.put(AdminClientConfig.RETRIES_CONFIG, retries);
        props.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);

        // 연결 설정
        props.put(AdminClientConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, connectionTimeoutMs);

        // 보안 설정
        configureSecurityProperties(props, cluster);

        // 클러스터별 추가 설정
        cluster.properties().forEach(props::put);

        return props;
    }

    /**
     * 보안 관련 Properties를 설정합니다.
     */
    private void configureSecurityProperties(Properties props, Cluster cluster) {
        Cluster.SecurityConfig security = cluster.security();
        if (security == null) {
            return;
        }

        // 보안 프로토콜
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, security.protocol());

        // SASL 설정
        if (security.usesSasl() && security.sasl() != null) {
            Cluster.SaslConfig sasl = security.sasl();
            props.put(SaslConfigs.SASL_MECHANISM, sasl.mechanism());

            String jaasConfig = buildJaasConfig(sasl);
            props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
        }

        // SSL 설정
        if (security.usesSsl() && security.ssl() != null) {
            Cluster.SslConfig ssl = security.ssl();

            if (ssl.truststoreLocation() != null) {
                props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ssl.truststoreLocation());
            }
            if (ssl.truststorePassword() != null) {
                props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.truststorePassword());
            }
            if (ssl.keystoreLocation() != null) {
                props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ssl.keystoreLocation());
            }
            if (ssl.keystorePassword() != null) {
                props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.keystorePassword());
            }
            if (ssl.keyPassword() != null) {
                props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, ssl.keyPassword());
            }
        }
    }

    /**
     * SASL JAAS 설정 문자열을 생성합니다.
     */
    private String buildJaasConfig(Cluster.SaslConfig sasl) {
        String mechanism = sasl.mechanism();

        if ("PLAIN".equalsIgnoreCase(mechanism)) {
            return String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    sasl.username(),
                    sasl.password()
            );
        } else if (mechanism != null && mechanism.toUpperCase().startsWith("SCRAM")) {
            return String.format(
                    "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                    sasl.username(),
                    sasl.password()
            );
        }

        throw new IllegalArgumentException("Unsupported SASL mechanism: " + mechanism);
    }
}
