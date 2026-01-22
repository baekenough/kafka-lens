package com.kafkalens.domain.broker;

import com.kafkalens.common.exception.ClusterNotFoundException;
import com.kafkalens.domain.cluster.ClusterService;
import com.kafkalens.infrastructure.kafka.AdminClientWrapper;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * BrokerService 단위 테스트.
 *
 * <p>TDD 방식으로 작성된 테스트로, BrokerService의 핵심 비즈니스 로직을 검증합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *   <li>listBrokers: 브로커 목록 조회</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BrokerServiceTest {

    @Mock
    private AdminClientWrapper adminClientWrapper;

    @Mock
    private ClusterService clusterService;

    private BrokerService brokerService;

    private static final String CLUSTER_ID = "local";

    @BeforeEach
    void setUp() {
        brokerService = new BrokerService(adminClientWrapper, clusterService);
    }

    @Nested
    @DisplayName("listBrokers()")
    class ListBrokers {

        @Test
        @DisplayName("클러스터의 모든 브로커 목록을 반환한다")
        void testListBrokers_returnsAllBrokers() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Collection<Node> nodes = List.of(
                    new Node(0, "broker-0", 9092, "rack-0"),
                    new Node(1, "broker-1", 9092, "rack-1"),
                    new Node(2, "broker-2", 9092, "rack-2")
            );
            Node controller = nodes.iterator().next(); // broker-0 is controller

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", controller, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Broker::id).containsExactlyInAnyOrder(0, 1, 2);
            verify(clusterService).existsById(CLUSTER_ID);
            verify(adminClientWrapper).describeCluster(CLUSTER_ID);
        }

        @Test
        @DisplayName("브로커 정보에 호스트, 포트, 랙이 포함된다")
        void testListBrokers_containsHostPortRack() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Collection<Node> nodes = List.of(
                    new Node(0, "broker-0.kafka.svc", 9092, "us-east-1a")
            );
            Node controller = nodes.iterator().next();

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", controller, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).hasSize(1);
            Broker broker = result.get(0);
            assertThat(broker.id()).isEqualTo(0);
            assertThat(broker.host()).isEqualTo("broker-0.kafka.svc");
            assertThat(broker.port()).isEqualTo(9092);
            assertThat(broker.rack()).isEqualTo("us-east-1a");
        }

        @Test
        @DisplayName("컨트롤러 브로커를 올바르게 식별한다")
        void testListBrokers_identifiesController() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Node broker0 = new Node(0, "broker-0", 9092, null);
            Node broker1 = new Node(1, "broker-1", 9092, null);
            Node broker2 = new Node(2, "broker-2", 9092, null);
            Collection<Node> nodes = List.of(broker0, broker1, broker2);
            Node controller = broker1; // broker-1 is controller

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", controller, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).hasSize(3);

            Broker controllerBroker = result.stream()
                    .filter(b -> b.id() == 1)
                    .findFirst()
                    .orElseThrow();
            assertThat(controllerBroker.isController()).isTrue();

            // 나머지 브로커들은 컨트롤러가 아님
            List<Broker> nonControllers = result.stream()
                    .filter(b -> b.id() != 1)
                    .toList();
            assertThat(nonControllers).allMatch(b -> !b.isController());
        }

        @Test
        @DisplayName("랙이 없는 브로커는 rack이 null이다")
        void testListBrokers_nullRackWhenNotConfigured() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Node broker = new Node(0, "broker-0", 9092, null); // no rack
            Collection<Node> nodes = List.of(broker);

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", broker, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).rack()).isNull();
        }

        @Test
        @DisplayName("브로커 목록이 ID 순으로 정렬된다")
        void testListBrokers_sortedById() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Collection<Node> nodes = List.of(
                    new Node(2, "broker-2", 9092, null),
                    new Node(0, "broker-0", 9092, null),
                    new Node(1, "broker-1", 9092, null)
            );
            Node controller = new Node(0, "broker-0", 9092, null);

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", controller, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Broker::id).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("브로커가 없으면 빈 목록을 반환한다")
        void testListBrokers_noBrokers_returnsEmptyList() {
            // given
            given(clusterService.existsById(CLUSTER_ID)).willReturn(true);

            Collection<Node> nodes = List.of();

            AdminClientWrapper.ClusterInfo clusterInfo =
                    new AdminClientWrapper.ClusterInfo("kafka-cluster-id", null, nodes);
            given(adminClientWrapper.describeCluster(CLUSTER_ID)).willReturn(clusterInfo);

            // when
            List<Broker> result = brokerService.listBrokers(CLUSTER_ID);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 클러스터로 조회하면 예외를 발생시킨다")
        void testListBrokers_nonExistingCluster_throwsException() {
            // given
            given(clusterService.existsById("unknown")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> brokerService.listBrokers("unknown"))
                    .isInstanceOf(ClusterNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }
}
