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
package idlgen

import freestyle.rpc.internal.util.StringUtil._
import freestyle.rpc.internal.util.{AstOptics, Toolbox}
import freestyle.rpc.protocol._

object ScalaParser {

  import Toolbox.u._
  import AstOptics._
  import Model._

  def parse(
      input: Tree,
      inputName: String
  ): RpcDefinitions = {

    val definitions = input.collect { case defs: ModuleDef => defs }

    def annotationValue(name: String): Option[String] =
      (for {
        defn       <- definitions
        annotation <- annotationsNamed(name).getAll(defn)
        firstArg   <- annotation.firstArg
      } yield firstArg).headOption.map(_.toString.unquoted)

    val outputName    = annotationValue("outputName").getOrElse(inputName)
    val outputPackage = annotationValue("outputPackage")

    val options: Seq[RpcOption] = for {
      defn             <- definitions
      option           <- annotationsNamed("option").getAll(defn)
      Seq(name, value) <- option.withArgsNamed("name", "value")
    } yield RpcOption(name.toString.unquoted, value.toString) // keep value quoting as-is

    val messages: Seq[RpcMessage] = definitions.head.collect {
      case ast._CaseClassDef(mod) if hasAnnotation("message")(mod) => mod
    } map { defn =>
      RpcMessage(defn.name.toString, params.getOption(defn).get) // TODO: wat
    }

    def getRequestsFromService(defn: Tree): List[RpcRequest] = {
      val rpcMethods = ast._AnnotatedDefDef("rpc")

      defn.collect({ case rpcMethods(x) => x }).map { x =>
        val serializationType = idlType.getAll(x).head
        val name              = x.name.toString
        val requestType       = firstParamForRpc.getOption(x).get
        val responseType      = returnTypeAsString.getOption(x).get
        val streamingType = (requestStreaming.getOption(x), responseStreaming.getOption(x)) match {
          case (None, None)       => None
          case (Some(_), None)    => Some(RequestStreaming)
          case (None, Some(_))    => Some(ResponseStreaming)
          case (Some(_), Some(_)) => Some(BidirectionalStreaming)
        }

        RpcRequest(serializationType, name, requestType, responseType, streamingType)
      }
    }

    val services: Seq[RpcService] = definitions.head.collect {
      case ast._ClassDef(mod) if hasAnnotation("service")(mod) => mod
    } map { defn =>
      RpcService(
        defn.name.toString,
        getRequestsFromService(defn)
      )
    }

    RpcDefinitions(outputName, outputPackage, options, messages, services)
  }
}
