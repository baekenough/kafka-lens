package com.kafkalens.domain.cluster;

import com.kafkalens.common.ErrorCode;
import com.kafkalens.common.exception.KafkaLensException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * YAML 기반 클러스터 저장소 구현.
 *
 * <p>clusters.yml 파일에서 클러스터 설정을 읽어옵니다.</p>
 */
@Repository
public class YamlClusterRepository implements ClusterRepository {

    private static final Logger log = LoggerFactory.getLogger(YamlClusterRepository.class);

    private final ResourceLoader resourceLoader;
    private final String configPath;
    private final Map<String, Cluster> clusterCache = new ConcurrentHashMap<>();

    public YamlClusterRepository(
            ResourceLoader resourceLoader,
            @Value("${kafkalens.clusters.config-path:classpath:clusters.yml}") String configPath
    ) {
        this.resourceLoader = resourceLoader;
        this.configPath = configPath;
    }

    /**
     * 애플리케이션 시작 시 클러스터 설정을 로드합니다.
     */
    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public List<Cluster> findAll() {
        return new ArrayList<>(clusterCache.values());
    }

    @Override
    public Optional<Cluster> findById(String id) {
        return Optional.ofNullable(clusterCache.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return clusterCache.containsKey(id);
    }

    @Override
    public List<Cluster> findByEnvironment(String environment) {
        return clusterCache.values().stream()
                .filter(c -> environment.equalsIgnoreCase(c.environment()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void reload() {
        log.info("Loading cluster configuration from: {}", configPath);

        try {
            Resource resource = resourceLoader.getResource(configPath);

            if (!resource.exists()) {
                log.warn("Cluster configuration file not found: {}", configPath);
                clusterCache.clear();
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                loadFromInputStream(inputStream);
            }

            log.info("Loaded {} clusters from configuration", clusterCache.size());
        } catch (IOException e) {
            throw new KafkaLensException(
                    ErrorCode.CLUSTER_CONFIG_ERROR,
                    "Failed to load cluster configuration: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * InputStream에서 클러스터 설정을 로드합니다.
     * 테스트를 위해 패키지-프라이빗으로 설정.
     *
     * @param inputStream YAML 입력 스트림
     */
    void loadFromInputStream(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);

        clusterCache.clear();

        if (config == null || !config.containsKey("clusters")) {
            log.warn("No clusters defined in configuration");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusters = (List<Map<String, Object>>) config.get("clusters");

        @SuppressWarnings("unchecked")
        Map<String, Object> defaults = (Map<String, Object>) config.getOrDefault("defaults", Collections.emptyMap());

        for (Map<String, Object> clusterData : clusters) {
            try {
                Cluster cluster = parseCluster(clusterData, defaults);
                clusterCache.put(cluster.id(), cluster);
                log.debug("Loaded cluster: {} ({})", cluster.id(), cluster.name());
            } catch (Exception e) {
                log.error("Failed to parse cluster: {}", clusterData.get("id"), e);
            }
        }
    }

    /**
     * 클러스터 맵 데이터를 Cluster 객체로 변환합니다.
     */
    @SuppressWarnings("unchecked")
    private Cluster parseCluster(Map<String, Object> data, Map<String, Object> defaults) {
        String id = (String) data.get("id");
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String environment = (String) data.getOrDefault("environment", "development");

        // Bootstrap servers 파싱
        List<String> bootstrapServers = parseBootstrapServers(data.get("bootstrap-servers"));

        // Security 설정 파싱 (기본값 병합)
        Map<String, Object> defaultSecurity = (Map<String, Object>) defaults.getOrDefault("security", Collections.emptyMap());
        Map<String, Object> clusterSecurity = (Map<String, Object>) data.getOrDefault("security", Collections.emptyMap());
        Cluster.SecurityConfig security = parseSecurity(mergeMaps(defaultSecurity, clusterSecurity));

        // Properties 파싱 (기본값 병합)
        Map<String, Object> defaultProps = (Map<String, Object>) defaults.getOrDefault("properties", Collections.emptyMap());
        Map<String, Object> clusterProps = (Map<String, Object>) data.getOrDefault("properties", Collections.emptyMap());
        Map<String, String> properties = parseProperties(mergeMaps(defaultProps, clusterProps));

        return Cluster.builder()
                .id(id)
                .name(name)
                .description(description)
                .environment(environment)
                .bootstrapServers(bootstrapServers)
                .security(security)
                .properties(properties)
                .build();
    }

    /**
     * Bootstrap servers를 파싱합니다.
     */
    @SuppressWarnings("unchecked")
    private List<String> parseBootstrapServers(Object value) {
        if (value instanceof List) {
            return new ArrayList<>((List<String>) value);
        } else if (value instanceof String) {
            return List.of(((String) value).split(","));
        }
        throw new IllegalArgumentException("Invalid bootstrap-servers format");
    }

    /**
     * Security 설정을 파싱합니다.
     */
    @SuppressWarnings("unchecked")
    private Cluster.SecurityConfig parseSecurity(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Cluster.SecurityConfig.plaintext();
        }

        String protocol = (String) data.getOrDefault("protocol", "PLAINTEXT");

        // SASL 설정 파싱
        Cluster.SaslConfig saslConfig = null;
        Map<String, Object> saslData = (Map<String, Object>) data.get("sasl");
        if (saslData != null) {
            saslConfig = new Cluster.SaslConfig(
                    (String) saslData.get("mechanism"),
                    (String) saslData.get("username"),
                    (String) saslData.get("password")
            );
        }

        // SSL 설정 파싱
        Cluster.SslConfig sslConfig = null;
        Map<String, Object> sslData = (Map<String, Object>) data.get("ssl");
        if (sslData != null) {
            sslConfig = new Cluster.SslConfig(
                    (String) sslData.get("truststore-location"),
                    (String) sslData.get("truststore-password"),
                    (String) sslData.get("keystore-location"),
                    (String) sslData.get("keystore-password"),
                    (String) sslData.get("key-password")
            );
        }

        return new Cluster.SecurityConfig(protocol, saslConfig, sslConfig);
    }

    /**
     * Properties를 파싱합니다.
     */
    private Map<String, String> parseProperties(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 두 맵을 병합합니다 (클러스터 설정이 기본값을 덮어씁니다).
     */
    private Map<String, Object> mergeMaps(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> result = new HashMap<>(defaults);
        result.putAll(overrides);
        return result;
    }
}
