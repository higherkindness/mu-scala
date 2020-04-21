/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.benchmarks

import java.util.concurrent.TimeUnit

import cats.effect.IO
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.benchmarks.shared.Utils._
import higherkindness.mu.rpc.benchmarks.shared.models._
import higherkindness.mu.rpc.benchmarks.shared.protocols.PersonServiceAvroWithSchema
import higherkindness.mu.rpc.benchmarks.shared._
import higherkindness.mu.rpc.protocol.Empty
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AvroWithSchemaBenchmark extends ServerRuntime {

  @Setup
  def setup(): Unit = startServer

  @TearDown
  def shutdown(): Unit = stopServer

  def clientCall[B](f: PersonServiceAvroWithSchema[IO] => IO[B]): B =
    PersonServiceAvroWithSchema
      .client[IO](ChannelForAddress("localhost", grpcPort))
      .use(f)
      .unsafeRunTimed(defaultTimeOut)
      .get

  @Benchmark
  def listPersons: PersonList =
    clientCall(_.listPersons(Empty))

  @Benchmark
  def getPerson: Person =
    clientCall(_.getPerson(PersonId("1")))

  @Benchmark
  def getPersonLinks: PersonLinkList =
    clientCall(_.getPersonLinks(PersonId("1")))

  @Benchmark
  def createPerson: Person =
    clientCall(_.createPerson(person))

  @Benchmark
  def programComposition: PersonAggregation =
    clientCall { client =>
      for {
        personList <- client.listPersons(Empty)
        p1         <- client.getPerson(PersonId("1"))
        p2         <- client.getPerson(PersonId("2"))
        p3         <- client.getPerson(PersonId("3"))
        p4         <- client.getPerson(PersonId("4"))
        p1Links    <- client.getPersonLinks(PersonId(p1.id))
        p3Links    <- client.getPersonLinks(PersonId(p3.id))
        pNew       <- client.createPerson(person)
      } yield (p1, p2, p3, p4, p1Links, p3Links, personList.add(pNew))
    }

}
