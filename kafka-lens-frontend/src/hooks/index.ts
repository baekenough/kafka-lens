/**
 * 커스텀 훅 모듈 엔트리포인트
 *
 * @module hooks
 */
export {
  useClusters,
  useCluster,
  testConnection,
  invalidateClusters,
  invalidateCluster,
  CLUSTERS_KEY,
  getClusterKey,
  type UseClustersReturn,
  type UseClusterReturn,
  type ConnectionTestResult,
} from './useClusters';

export {
  useConsumerGroups,
  useConsumerGroup,
  invalidateConsumerGroups,
  invalidateConsumerGroup,
  getConsumerGroupsKey,
  getConsumerGroupKey,
  type UseConsumerGroupsReturn,
  type UseConsumerGroupReturn,
} from './useConsumerGroups';

export {
  useTopics,
  useTopic,
  invalidateTopics,
  invalidateTopic,
  getTopicsKey,
  getTopicKey,
  type UseTopicsReturn,
  type UseTopicReturn,
  type TopicDetail,
  type PartitionInfo,
} from './useTopics';

export {
  useLag,
  getLagKey,
  getLagStatus,
  formatLag,
  LAG_THRESHOLDS,
  type UseLagReturn,
  type LagStatus,
} from './useLag';

export {
  useBrokers,
  useBroker,
  invalidateBrokers,
  invalidateBroker,
  getBrokersKey,
  getBrokerKey,
  type UseBrokersReturn,
  type UseBrokerReturn,
} from './useBrokers';

export {
  useMessages,
  useMessage,
  type UseMessagesReturn,
  type MessageSearchState,
  type MessagePublishState,
} from './useMessages';

export {
  useAuth,
  type UseAuthReturn,
  type AuthCredentials,
  type AuthState,
} from './useAuth';
