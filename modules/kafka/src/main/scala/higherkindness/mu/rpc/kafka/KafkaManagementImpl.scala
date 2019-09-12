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

import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import fs2.kafka._
import higherkindness.mu.rpc.protocol.Empty
import org.apache.kafka.clients.admin.{NewPartitions, NewTopic}

import kafkaManagementService._

class KafkaManagementImpl[F[_]: ContextShift: Concurrent] private[kafka] (
    adminClient: KafkaAdminClient[F]
) extends KafkaManagement[F] {
  override def createPartitions(cpr: CreatePartitionsRequest): F[Unit] =
    adminClient.createPartitions(cpr.ps.mapValues(NewPartitions.increaseTo))

  override def createTopic(ctr: CreateTopicRequest): F[Unit] =
    adminClient.createTopic(new NewTopic(ctr.name, ctr.numPartitions, ctr.replicationFactor))

  override def createTopics(ctrs: List[CreateTopicRequest]): F[Unit] =
    for {
      newTopics <- ctrs
        .map(ctr => new NewTopic(ctr.name, ctr.numPartitions, ctr.replicationFactor))
        .pure[F]
      _ <- adminClient.createTopics(newTopics)
    } yield ()

  override def deleteTopic(t: String): F[Unit] = adminClient.deleteTopic(t)

  override def deleteTopics(ts: List[String]): F[Unit] = adminClient.deleteTopics(ts)

  override def describeCluster(r: Empty.type): F[Cluster] = {
    val dc = adminClient.describeCluster
    (dc.clusterId, dc.controller, dc.nodes).mapN { (id, c, ns) =>
      Cluster(ns.map(Node.fromJava).toList, Node.fromJava(c), id)
    }
  }

  override def describeConfigs(rs: List[ConfigResource]): F[Configs] =
    for {
      kConfigs <- adminClient.describeConfigs(rs.map(ConfigResource.toKafkaConfigResource))
      configs = kConfigs.map {
        case (cr, ces) =>
          ConfigResource.fromJava(cr) -> ces.map(ConfigEntry.fromJava)
      }
    } yield Configs(configs)

  override def describeConsumerGroups(groupIds: List[String]): F[ConsumerGroups] =
    for {
      kGroups <- adminClient.describeConsumerGroups(groupIds)
      groups = kGroups.map { case (gid, cgd) => gid -> ConsumerGroupDescription.fromJava(cgd) }
    } yield ConsumerGroups(groups)

  override def describeTopics(topics: List[String]): F[Topics] =
    for {
      kTopics <- adminClient.describeTopics(topics)
      topics = kTopics.map { case (topic, desc) => topic -> TopicDescription.fromJava(desc) }
    } yield Topics(topics)

  override def listConsumerGroupOffsets(groupId: String): F[ConsumerGroupOffsets] =
    for {
      kOffsets <- adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata
      offsets = kOffsets.map {
        case (topic, offset) =>
          TopicPartition.fromJava(topic) -> OffsetAndMetadata.fromJava(offset)
      }
    } yield ConsumerGroupOffsets(offsets)

  override def listConsumerGroups(r: Empty.type): F[List[ConsumerGroupListing]] =
    for {
      kListings <- adminClient.listConsumerGroups.listings
      listings = kListings.map(ConsumerGroupListing.fromJava)
    } yield listings

  override def listTopics(r: Empty.type): F[List[TopicListing]] =
    for {
      kListings <- adminClient.listTopics.includeInternal.listings
      listings = kListings.map(TopicListing.fromJava)
    } yield listings
}
