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

package freestyle.rpc
package http

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.reflect.macros.blackbox

object protocol {

  @compileTimeOnly("enable macro paradise to expand @deriveHttp macro annotations")
  class deriveHttp extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro deriveHttp_impl
  }

  def deriveHttp_impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    require(annottees.length == 2, "@deriveHttp annotation should come AFTER @service annotation")

    val serviceDef = annottees.head.tree
      .collect({
        case x: ClassDef if x.mods.hasFlag(TRAIT) || x.mods.hasFlag(ABSTRACT) => x
      })
      .head

    val F_ : TypeDef = serviceDef.tparams.head
    val F: TypeName  = F_.name

    val defs: List[Tree] = serviceDef.impl.body

    val (rpcDefs, nonRpcDefs) = defs.collect {
      case d: DefDef => d
    } partition (_.rhs.isEmpty)

    def findAnnotation(mods: Modifiers, name: String): Option[Tree] =
      mods.annotations find {
        case Apply(Select(New(Ident(TypeName(`name`))), _), _)     => true
        case Apply(Select(New(Select(_, TypeName(`name`))), _), _) => true
        case _                                                     => false
      }

    def requestExecution(responseType: Tree, methodResponseType: Tree): Tree =
      methodResponseType match {
        case tq"Observable[..$tpts]" =>
          q"Observable.fromReactivePublisher(client.streaming(request)(_.body.chunks.parseJsonStream.map(_.as[$responseType]).rethrow).toUnicastPublisher)"
        case tq"Stream[$carrier, ..$tpts]" =>
          q"client.streaming(request)(_.body.chunks.parseJsonStream.map(_.as[$responseType]).rethrow)"
        case tq"$carrier[..$tpts]" =>
          q"client.expect[$responseType](request)"
      }

    val toHttpRequest: ((TermName, String, TermName, Tree, Tree, Tree)) => DefDef = {
      case (method, path, name, requestType, responseType, methodResponseType) =>
        q"""
        def $name(req: $requestType)(implicit
          client: _root_.org.http4s.client.Client[F],
          requestEncoder: EntityEncoder[F, $requestType],
          responseDecoder: EntityDecoder[F, $responseType]
        ): $methodResponseType = {
          val request = Request[F](Method.$method, uri / $path).withBody(req)
          ${requestExecution(responseType, methodResponseType)}
        }
        """
    }

    val requests = for {
      d      <- rpcDefs.collect { case x if findAnnotation(x.mods, "http").isDefined => x }
      args   <- findAnnotation(d.mods, "http").collect({ case Apply(_, args) => args }).toList
      params <- d.vparamss
      _ = require(params.length == 1, s"RPC call ${d.name} has more than one request parameter")
      p <- params.headOption.toList
    } yield {
      val method = TermName(args(0).toString) // TODO: fix direct index access
      val uri    = args(1).toString // TODO: fix direct index access

      val responseType: Tree = d.tpt match {
        case tq"Observable[..$tpts]"       => tpts.head
        case tq"Stream[$carrier, ..$tpts]" => tpts.head
        case tq"$carrier[..$tpts]"         => tpts.head
        case _                             => throw new Exception("asdf") //TODO: sh*t
      }

      (method, uri, d.name, p.tpt, responseType, d.tpt)
    }

    val httpRequests    = requests.map(toHttpRequest)
    val HttpClient      = TypeName("HttpClient")
    val httpClientClass = q"""
        class $HttpClient[$F_](uri: Uri)(implicit Sync: _root_.cats.effect.Effect[F], ec: scala.concurrent.ExecutionContext) {
          ..$httpRequests
        }
        """

    println(httpClientClass)

    val http = q"""
        object http {

          import _root_.fs2.interop.reactivestreams._
          import _root_.org.http4s._
          import _root_.jawnfs2._
          import _root_.io.circe.jawn.CirceSupportParser.facade

          $httpClientClass
        }
      """

    val List(companion) = annottees.map(_.tree).collect({ case x: ModuleDef => x })

    val result: List[Tree] = List(
      annottees.head.tree, // the original trait definition
      ModuleDef(
        companion.mods,
        companion.name,
        Template(
          companion.impl.parents,
          companion.impl.self,
          companion.impl.body
        )
      )
    )

    c.Expr(Block(result, Literal(Constant(()))))
  }

}
