/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle
package http
package h2akka

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.Printer
import cats._
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import io.circe.jawn

import scala.concurrent.Future
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import freestyle.http.core._

private[h2akka] final class HydrateAkkaHttpRoutes[H <: HydrationMacros](h0: H)
    extends Hydration(h0) {
  import h._
  import c.universe._

  type Out[F[_]] = (F ~> Future) => Route

  override final def hydrate(
      F: Symbol,
      nodes: List[NodeInfo]
  ): Tree =
    q"""
     import cats.arrow.FunctionK
     import akka.http.scaladsl.server.Directives._
     import akka.http.scaladsl.server.RouteConcatenation.concat
     import akka.http.scaladsl.model.HttpEntity
     import scala.concurrent.Future
     import freestyle.http.h2akka.HydrateAkkaHttpRoutesSupport._

     (eval: FunctionK[$F, Future]) => concat(
       ..${nodes.map(info => makeRoute(info.name, info.in, info.out))})
     """

  private[this] final def makeRoute(opName: String, FA: Type, A: Type): Tree = {

    val needsDecoding = !isSingleton(FA)
    val needsEncoding = isUnitType(A)

    val encode =
      if (needsEncoding)
        q"""
          implicit val m = makeMarshaller[$A]
          complete(a)
        """
      else
        q"""complete("{}")"""

    val complete =
      q"""
        val a = eval(op)
        $encode
      """

    val decode =
      if (needsDecoding)
        q"""
          implicit val um = makeUnmarshaller[$FA]
          entity(as[$FA]) { op => $complete }
        """
      else
        q"""
          val op = ${FA.termSymbol}
          $complete
         """

    q""" path($opName) { get { $decode } } """
  }

}

object HydrateAkkaHttpRoutesSupport {

  private[this] final val jsonMarshaller: ToEntityMarshaller[Json] =
    Marshaller.withFixedContentType(`application/json`) { json =>
      HttpEntity(`application/json`, Printer.spaces2.pretty(json))
    }

  private[this] final val jsonUnmarshaller: FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .map {
        case ByteString.empty => throw Unmarshaller.NoContentException
        case data             => jawn.parseByteBuffer(data.asByteBuffer).fold(throw _, v => v)
      }

  final def makeMarshaller[A: Encoder]: ToEntityMarshaller[A] =
    jsonMarshaller.compose(Encoder[A].apply)

  final def makeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    def decode(json: Json) = Decoder[A].decodeJson(json).fold(throw _, v => v)
    jsonUnmarshaller.map(decode)
  }
}
