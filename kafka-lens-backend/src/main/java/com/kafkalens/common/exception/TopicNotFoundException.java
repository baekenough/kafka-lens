package com.kafkalens.common.exception;

import com.kafkalens.common.ErrorCode;

import java.util.Map;

/**
 * 토픽을 찾을 수 없을 때 발생하는 예외.
 */
public class TopicNotFoundException extends KafkaLensException {

    /**
     * 클러스터 ID와 토픽 이름으로 예외를 생성합니다.
     *
     * @param clusterId 클러스터 ID
     * @param topicName 찾을 수 없는 토픽 이름
     */
    public TopicNotFoundException(String clusterId, String topicName) {
        super(
                ErrorCode.TOPIC_NOT_FOUND,
                String.format("Topic not found: %s in cluster %s", topicName, clusterId),
                Map.of("clusterId", clusterId, "topicName", topicName)
        );
    }
}
