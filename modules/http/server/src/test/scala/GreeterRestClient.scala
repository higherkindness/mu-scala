/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc.http

import cats.effect._
import fs2.Stream
import io.circe._
import io.circe.generic.auto._
import io.circe.jawn.CirceSupportParser.facade
import io.circe.syntax._
import jawnfs2._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.dsl.io._
import org.http4s._

class GreeterRestClient[F[_]: Effect](uri: Uri) {

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def getHello()(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.GET, uri / "getHello")
    client.expect[HelloResponse](request)
  }

  def sayHello(arg: HelloRequest)(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHello")
    client.expect[HelloResponse](request.withBody(arg.asJson))
  }

  def sayHellos(arg: Stream[F, HelloRequest])(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellos")
    client.expect[HelloResponse](request.withBody(arg.map(_.asJson)))
  }

  def sayHelloAll(arg: HelloRequest)(implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHelloAll")
    client.streaming(request.withBody(arg.asJson))(responseStream[HelloResponse])
  }

  def sayHellosAll(arg: Stream[F, HelloRequest])(
      implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellosAll")
    client.streaming(request.withBody(arg.map(_.asJson)))(responseStream[HelloResponse])
  }

  private def responseStream[A](response: Response[F])(implicit decoder: Decoder[A]): Stream[F, A] =
    if (response.status.code != 200) throw UnexpectedStatus(response.status)
    else response.body.chunks.parseJsonStream.map(_.as[A]).rethrow

}
