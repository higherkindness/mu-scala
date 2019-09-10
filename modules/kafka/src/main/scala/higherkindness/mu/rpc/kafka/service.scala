/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.rpc.kafka

import higherkindness.mu.rpc.protocol.{service, Empty}
import org.apache.kafka.common.{
  ConsumerGroupState => KConsumerGroupState,
  Node => KNode,
  TopicPartition => KTopicPartition,
  TopicPartitionInfo => KTopicPartitionInfo
}
import org.apache.kafka.common.acl.{AclOperation => KAclOperation}
import org.apache.kafka.common.config.{ConfigResource => KConfigResource}
import org.apache.kafka.clients.admin.{
  ConfigEntry => KConfigEntry,
  ConsumerGroupDescription => KConsumerGroupDescription,
  ConsumerGroupListing => KConsumerGroupListing,
  MemberAssignment => KMemberAssignment,
  MemberDescription => KMemberDescription,
  TopicDescription => KTopicDescription
}
import org.apache.kafka.clients.consumer.{OffsetAndMetadata => KOffsetAndMetadata}

import scala.collection.JavaConverters._

object KafkaManagementService {
  final case class CreatePartitionsRequest(ps: Map[String, Int])

  final case class CreateTopicRequest(name: String, numPartitions: Int, replicationFactor: Short)

  final case class Node(id: Int, host: String, port: Int, rack: Option[String])
  object Node {
    def fromJava(n: KNode): Node = Node(n.id(), n.host(), n.port(), Option(n.rack()))
  }
  final case class Cluster(nodes: List[Node], controller: Node, clusterId: String)

  sealed trait ConfigType
  object ConfigType {
    final case object TopicConfigType extends ConfigType
    final case object BrokerConfigType extends ConfigType
    final case object UnknownConfigType extends ConfigType

    def toKafkaConfigType(ct: ConfigType): KConfigResource.Type = ct match {
      case TopicConfigType => KConfigResource.Type.TOPIC
      case BrokerConfigType => KConfigResource.Type.BROKER
      case UnknownConfigType => KConfigResource.Type.UNKNOWN
    }
    def fromJava(kct: KConfigResource.Type): ConfigType = kct match {
      case KConfigResource.Type.TOPIC => TopicConfigType
      case KConfigResource.Type.BROKER => BrokerConfigType
      case KConfigResource.Type.UNKNOWN => UnknownConfigType
    }
  }
  final case class ConfigResource(typ: ConfigType, name: String)
  object ConfigResource {
    def toKafkaConfigResource(cr: ConfigResource): KConfigResource =
      new KConfigResource(ConfigType.toKafkaConfigType(cr.typ), cr.name)
    def fromJava(kcr: KConfigResource): ConfigResource =
      ConfigResource(ConfigType.fromJava(kcr.`type`()), kcr.name())
  }
  sealed trait ConfigSource
  object ConfigSource {
    final case object DynamicTopicConfig extends ConfigSource
    final case object DynamicBrokerConfig extends ConfigSource
    final case object DynamicDefaultBrokerConfig extends ConfigSource
    final case object StaticBrokerConfig extends ConfigSource
    final case object DefaultConfig extends ConfigSource
    final case object UnknownConfig extends ConfigSource

    def fromJava(kcs: KConfigEntry.ConfigSource): ConfigSource = kcs match {
      case KConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG => DynamicTopicConfig
      case KConfigEntry.ConfigSource.DYNAMIC_BROKER_CONFIG => DynamicBrokerConfig
      case KConfigEntry.ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG => DynamicDefaultBrokerConfig
      case KConfigEntry.ConfigSource.STATIC_BROKER_CONFIG => StaticBrokerConfig
      case KConfigEntry.ConfigSource.DEFAULT_CONFIG => DefaultConfig
      case KConfigEntry.ConfigSource.UNKNOWN => UnknownConfig
    }
  }
  final case class ConfigSynonym(name: String, value: String, source: ConfigSource)
  object ConfigSynonym {
    def fromJava(kcs: KConfigEntry.ConfigSynonym): ConfigSynonym =
      ConfigSynonym(kcs.name(), kcs.value(), ConfigSource.fromJava(kcs.source()))
  }
  final case class ConfigEntry(
    name: String,
    value: String,
    source: ConfigSource,
    isSensitive: Boolean,
    isReadOnly: Boolean,
    synonyms: List[ConfigSynonym]
  )
  object ConfigEntry {
    def fromJava(kce: KConfigEntry): ConfigEntry = ConfigEntry(
      kce.name(),
      kce.value(),
      ConfigSource.fromJava(kce.source()),
      kce.isSensitive(),
      kce.isReadOnly(),
      kce.synonyms().asScala.map(ConfigSynonym.fromJava).toList
    )
  }
  final case class Configs(configs: Map[ConfigResource, List[ConfigEntry]])

  final case class TopicPartition(topic: String, partition: Int)
  object TopicPartition {
    def fromJava(ktp: KTopicPartition): TopicPartition =
      TopicPartition(ktp.topic(), ktp.partition())
  }
  final case class MemberAssignment(topicPartitions: List[TopicPartition])
  object MemberAssignment {
    def fromJava(kma: KMemberAssignment): MemberAssignment =
      MemberAssignment(
        kma.topicPartitions().asScala.map(TopicPartition.fromJava).toList)
  }
  final case class MemberDescription(
    consumerId: String,
    clientId: String,
    host: String,
    assignment: MemberAssignment
  )
  object MemberDescription {
    def fromJava(kmd: KMemberDescription): MemberDescription = MemberDescription(
      kmd.consumerId(),
      kmd.clientId(),
      kmd.host(),
      MemberAssignment.fromJava(kmd.assignment())
    )
  }
  sealed trait ConsumerGroupState
  object ConsumerGroupState {
    final case object CompletingRebalance extends ConsumerGroupState
    final case object Dead extends ConsumerGroupState
    final case object Empty extends ConsumerGroupState
    final case object PreparingRebalance extends ConsumerGroupState
    final case object Stable extends ConsumerGroupState
    final case object Unknown extends ConsumerGroupState

    def fromJava(kcgs: KConsumerGroupState): ConsumerGroupState = kcgs match {
      case KConsumerGroupState.COMPLETING_REBALANCE => CompletingRebalance
      case KConsumerGroupState.DEAD => Dead
      case KConsumerGroupState.EMPTY => Empty
      case KConsumerGroupState.PREPARING_REBALANCE => PreparingRebalance
      case KConsumerGroupState.STABLE => Stable
      case KConsumerGroupState.UNKNOWN => Unknown
    }
  }
  sealed trait AclOperation
  object AclOperation {
    final case object All extends AclOperation
    final case object Alter extends AclOperation
    final case object AlterConfigs extends AclOperation
    final case object Any extends AclOperation
    final case object ClusterAction extends AclOperation
    final case object Create extends AclOperation
    final case object Delete extends AclOperation
    final case object Describe extends AclOperation
    final case object DescribeConfigs extends AclOperation
    final case object IdempotentWrite extends AclOperation
    final case object Read extends AclOperation
    final case object Unknown extends AclOperation
    final case object Write extends AclOperation

    def fromJava(kao: KAclOperation): AclOperation = kao match {
      case KAclOperation.ALL => All
      case KAclOperation.ALTER => Alter
      case KAclOperation.ALTER_CONFIGS => AlterConfigs
      case KAclOperation.ANY => Any
      case KAclOperation.CLUSTER_ACTION => ClusterAction
      case KAclOperation.CREATE => Create
      case KAclOperation.DELETE => Delete
      case KAclOperation.DESCRIBE => Describe
      case KAclOperation.DESCRIBE_CONFIGS => DescribeConfigs
      case KAclOperation.IDEMPOTENT_WRITE => IdempotentWrite
      case KAclOperation.READ => Read
      case KAclOperation.UNKNOWN => Unknown
      case KAclOperation.WRITE => Write
    }
  }
  final case class ConsumerGroupDescription(
    groupId: String,
    isSimpleConsumerGroup: Boolean,
    members: List[MemberDescription],
    partitionAssignor: String,
    state: ConsumerGroupState,
    coordinator: Node,
    authorizedOperations: List[AclOperation]
  )
  object ConsumerGroupDescription {
    def fromJava(kcgd: KConsumerGroupDescription): ConsumerGroupDescription =
      ConsumerGroupDescription(
        kcgd.groupId(),
        kcgd.isSimpleConsumerGroup(),
        kcgd.members().asScala.map(MemberDescription.fromJava).toList,
        kcgd.partitionAssignor(),
        ConsumerGroupState.fromJava(kcgd.state()),
        Node.fromJava(kcgd.coordinator()),
        kcgd.authorizedOperations().asScala.map(AclOperation.fromJava).toList
      )
  }
  final case class ConsumerGroups(consumerGroups: Map[String, ConsumerGroupDescription])

  final case class TopicPartitionInfo(
    partition: Int,
    leader: Node,
    replicats: List[Node],
    inSyncReplicas: List[Node]
  )
  object TopicPartitionInfo {
    def fromJava(ktpi: KTopicPartitionInfo): TopicPartitionInfo =
      TopicPartitionInfo(
        ktpi.partition(),
        Node.fromJava(ktpi.leader()),
        ktpi.replicas().asScala.map(Node.fromJava).toList,
        ktpi.isr().asScala.map(Node.fromJava).toList
      )
  }
  final case class TopicDescription(
    name: String,
    internal: Boolean,
    partitions: List[TopicPartitionInfo],
    authorizedOperations: List[AclOperation]
  )
  object TopicDescription {
    def fromJava(ktd: KTopicDescription): TopicDescription = TopicDescription(
      ktd.name(),
      ktd.isInternal(),
      ktd.partitions().asScala.map(TopicPartitionInfo.fromJava).toList,
      ktd.authorizedOperations().asScala.map(AclOperation.fromJava).toList
    )
  }
  final case class Topics(topics: Map[String, TopicDescription])

  final case class OffsetAndMetadata(
    offset: Long,
    metadata: String,
    leaderEpoch: Option[Int]
  )
  object OffsetAndMetadata {
    def fromJava(koam: KOffsetAndMetadata): OffsetAndMetadata =
      OffsetAndMetadata(
        koam.offset(),
        koam.metadata(),
        if (koam.leaderEpoch().isPresent()) Some(koam.leaderEpoch().get) else None
      )
  }
  final case class ConsumerGroupOffsets(offsets: Map[TopicPartition, OffsetAndMetadata])

  final case class ConsumerGroupListing(
    groupId: String,
    isSimpleConsumerGroup: Boolean
  )
  object ConsumerGroupListing {
    def fromJava(kcgl: KConsumerGroupListing): ConsumerGroupListing = ConsumerGroupListing(
      kcgl.groupId(),
      kcgl.isSimpleConsumerGroup()
    )
  }

  @service(Protobuf)
  trait KafkaManagement[F[_]] {
    def createPartitions(cpr: CreatePartitionsRequest): F[Unit]
    def createTopic(ctr: CreateTopicRequest): F[Unit]
    def createTopics(ctrs: List[CreateTopicRequest]): F[Unit]
    def deleteTopic(t: String): F[Unit]
    def deleteTopics(ts: List[String]): F[Unit]
    def describeCluster(request: Empty.type): F[Cluster]
    def describeConfigs(rs: List[ConfigResource]): F[Configs]
    def describeConsumerGroups(groupIds: List[String]): F[ConsumerGroups]
    def describeTopics(topics: List[String]): F[Topics]
    def listConsumerGroupOffsets(groupId: String): F[ConsumerGroupOffsets]
    def listConsumerGroups(request: Empty.type): F[List[ConsumerGroupListing]]
  }
}
