/* -
 * Mezzo [core]
 */

package mezzo
package h2akka

import io.circe.Decoder
import io.circe.Encoder
import io.circe.parser.{ parse => parseJSON }

import cats._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpProtocols
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait AkkaHttpClient extends Dehydrated {
  type H = HydrateAkkaHttpClient[_]
}

class HydrateAkkaHttpClient[H <: HydrationMacros](h0: H) extends Hydration(h0) {
  import h._
  import c.universe._

  type Out[F[_]] = AkkaClientRequestHandler => (F ~> Future)

  override def hydrate(
    F: Symbol,
    nodes: List[NodeInfo]
  ): Tree = {
    val (cases, methods) = nodes
      .map { info =>
      val methodName = TermName("handle_" + info.in.typeSymbol.name)
      val case_ = cq"""op: ${info.in} => $methodName(op)"""
      val method = hydrateMethod(methodName, info.name, info.in, info.out)
      (case_, method)
    }.unzip(v => v)

    q"""

      import akka.http.scaladsl.model.HttpMethods.GET
      import cats.arrow.FunctionK
      import scala.concurrent.Future

      class HydratedFunctionK(
        handler: AkkaClientRequestHandler
      ) extends FunctionK[$F, Future] {

        override final def apply[A](rawOp: $F[A]): Future[A] = rawOp match {
          case ..$cases
        }

        ..$methods
      }

      (handler: AkkaClientRequestHandler) => new HydratedFunctionK(handler)
      """
  }

  def hydrateMethod(
    methodName: TermName,
    opName: String,
    FA: Type,
    A: Type
  ): Tree = {

    val needsEncoding = FA.typeSymbol.companion.isModule
    val makeRequest =
      if (needsEncoding)
        q"handler.request[$FA, $A](GET, $opName, op)"
      else
        q"handler.request[$A](GET, $opName)"

    q"""
      def $methodName(op: $FA): Future[$A] = {
        $makeRequest
      }
    """
  }

}

case class AkkaClientRequestHandler(
  system : ActorSystem,
  baseUri: String = ""
)(implicit ec: ExecutionContext, mat: Materializer = ActorMaterializer()(system)) {

  private[this] lazy val http: HttpExt = Http(system)

  def request[B: Decoder](
    method: HttpMethod,
    uri   : String
  ): Future[B] =
    request(method, uri,
      HttpEntity(ContentTypes.`application/json`, ""))

  def request[A, B: Decoder](
    method: HttpMethod,
    uri   : String,
    body  : A
  )(implicit encoder: Encoder[A]): Future[B] =
    request(method, uri,
      HttpEntity(ContentTypes.`application/json`, encoder(body).spaces2))

  def request[B](
    method: HttpMethod,
    uri   : String,
    entity: RequestEntity
  )(implicit decoder: Decoder[B]): Future[B] = {
    val request = HttpRequest(
      method, s"$baseUri$uri", headers = Nil, entity, HttpProtocols.`HTTP/1.1`)
    try {
      val responseFuture: Future[HttpResponse] = http.singleRequest(request)
      responseFuture
        .flatMap(response => Unmarshal(response.entity).to[String])
        .map    (data     => {
          parseJSON(data).flatMap(decoder.decodeJson)
            .fold(error => throw error, b => b) })
    } catch {
      case e: Throwable => Future.failed(e)
    }

  }

}
