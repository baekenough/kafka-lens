package com.kafkalens.infrastructure.kafka;

import com.kafkalens.domain.cluster.Cluster;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.UUID;

/**
 * Kafka Consumer 팩토리.
 *
 * <p>메시지 조회를 위한 임시 KafkaConsumer를 생성합니다.
 * 각 조회 요청마다 고유한 그룹 ID를 사용하여 독립적인 Consumer를 생성합니다.</p>
 */
@Component("kafkaLensConsumerFactory")
public class KafkaConsumerFactory {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerFactory.class);

    @Value("${kafka.consumer.session-timeout-ms:30000}")
    private int sessionTimeoutMs;

    @Value("${kafka.consumer.request-timeout-ms:30000}")
    private int requestTimeoutMs;

    @Value("${kafka.consumer.max-poll-records:500}")
    private int maxPollRecords;

    /**
     * 클러스터에 대한 KafkaConsumer를 생성합니다.
     *
     * <p>임시 그룹 ID를 사용하여 다른 Consumer와 간섭 없이 메시지를 조회합니다.</p>
     *
     * @param cluster 클러스터 설정
     * @param groupIdSuffix 그룹 ID 접미사
     * @return KafkaConsumer 인스턴스
     */
    public KafkaConsumer<byte[], byte[]> createConsumer(Cluster cluster, String groupIdSuffix) {
        log.debug("Creating KafkaConsumer for cluster: {}", cluster.id());

        Properties props = buildConsumerProperties(cluster, groupIdSuffix);

        return new KafkaConsumer<>(props);
    }

    /**
     * 고유한 임시 그룹 ID를 생성합니다.
     *
     * @return 임시 그룹 ID
     */
    public String generateTemporaryGroupId() {
        return "kafka-lens-temp-" + UUID.randomUUID();
    }

    /**
     * Consumer 설정을 빌드합니다.
     */
    private Properties buildConsumerProperties(Cluster cluster, String groupIdSuffix) {
        Properties props = new Properties();

        // 기본 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-lens-" + groupIdSuffix);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // 자동 커밋 비활성화 (메시지 조회 전용)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 타임아웃 설정
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

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
        props.put("security.protocol", security.protocol());

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
