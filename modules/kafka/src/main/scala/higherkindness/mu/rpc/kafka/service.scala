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
import org.apache.kafka.common.{Node => KNode}
import org.apache.kafka.common.config.{ConfigResource => KConfigResource}
import org.apache.kafka.clients.admin.{ConfigEntry => KConfigEntry}

import scala.collection.JavaConverters._

object KafkaManagementService {
  final case class CreatePartitionsRequest(ps: Map[String, Int])
  final case class CreateTopicRequest(name: String, numPartitions: Int, replicationFactor: Short)
  final case class Node(id: Int, host: String, port: Int, rack: Option[String])
  object Node {
    def fromKafkaNode(n: KNode): Node = Node(n.id(), n.host(), n.port(), Option(n.rack()))
  }
  final case class Cluster(nodes: List[Node], controller: Node, clusterId: String)
  sealed trait ConfigType
  object ConfigType {
    case object TopicConfigType extends ConfigType
    case object BrokerConfigType extends ConfigType
    case object UnknownConfigType extends ConfigType

    def toKafkaConfigType(ct: ConfigType): KConfigResource.Type = ct match {
      case TopicConfigType => KConfigResource.Type.TOPIC
      case BrokerConfigType => KConfigResource.Type.BROKER
      case UnknownConfigType => KConfigResource.Type.UNKNOWN
    }
    def fromKafkaConfigType(kct: KConfigResource.Type): ConfigType = kct match {
      case KConfigResource.Type.TOPIC => TopicConfigType
      case KConfigResource.Type.BROKER => BrokerConfigType
      case KConfigResource.Type.UNKNOWN => UnknownConfigType
    }
  }
  final case class ConfigResource(typ: ConfigType, name: String)
  object ConfigResource {
    def toKafkaConfigResource(cr: ConfigResource): KConfigResource =
      new KConfigResource(ConfigType.toKafkaConfigType(cr.typ), cr.name)
    def fromKafkaConfigResource(kcr: KConfigResource): ConfigResource =
      ConfigResource(ConfigType.fromKafkaConfigType(kcr.`type`()), kcr.name())
  }
  sealed trait ConfigSource
  object ConfigSource {
    case object DynamicTopicConfig extends ConfigSource
    case object DynamicBrokerConfig extends ConfigSource
    case object DynamicDefaultBrokerConfig extends ConfigSource
    case object StaticBrokerConfig extends ConfigSource
    case object DefaultConfig extends ConfigSource
    case object UnknownConfig extends ConfigSource

    def fromKafkaConfigSource(kcs: KConfigEntry.ConfigSource): ConfigSource = kcs match {
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
    def fromKafkaConfigSynonym(kcs: KConfigEntry.ConfigSynonym): ConfigSynonym =
      ConfigSynonym(kcs.name(), kcs.value(), ConfigSource.fromKafkaConfigSource(kcs.source()))
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
    def fromKafkaConfigEntry(kce: KConfigEntry): ConfigEntry = ConfigEntry(
      kce.name(),
      kce.value(),
      ConfigSource.fromKafkaConfigSource(kce.source()),
      kce.isSensitive(),
      kce.isReadOnly(),
      kce.synonyms().asScala.map(ConfigSynonym.fromKafkaConfigSynonym).toList
    )
  }
  final case class Configs(configs: Map[ConfigResource, List[ConfigEntry]])

  @service(Protobuf)
  trait KafkaManagement[F[_]] {
    def createPartitions(cpr: CreatePartitionsRequest): F[Unit]
    def createTopic(ctr: CreateTopicRequest): F[Unit]
    def createTopics(ctrs: List[CreateTopicRequest]): F[Unit]
    def deleteTopic(t: String): F[Unit]
    def deleteTopics(ts: List[String]): F[Unit]
    def describeCluster(request: Empty.type): F[Cluster]
    def describeConfigs(rs: List[ConfigResource]): F[Configs]
  }
}
