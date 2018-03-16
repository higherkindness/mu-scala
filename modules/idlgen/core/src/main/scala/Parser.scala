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

package freestyle.rpc.idlgen

import freestyle.rpc.internal.ServiceAlg
import freestyle.rpc.internal.util.ScalametaUtil._
import freestyle.rpc.internal.util.StringUtil._
import scala.meta._

object Parser {

  def parse(input: Source, inputName: String): RpcDefinitions = {
    val definitions = input.collect { case defn: Defn => defn }

    // format: OFF
    def annotationValue(name: String): Option[String] = (for {
      defn <- definitions
      annotation <- defn.annotationsNamed(name)
      firstArg <- annotation.firstArg
    } yield firstArg).headOption.map(_.toString.unquoted)

    val outputName = annotationValue("outputName").getOrElse(inputName)
    val outputPackage = annotationValue("outputPackage")

    val options: Seq[RpcOption] = for {
      defn  <- definitions
      option <- defn.annotationsNamed("option")
      Seq(name, value) <- option.withArgsNamed("name", "value")
    } yield RpcOption(name.toString.unquoted, value.toString) // keep value quoting as-is

    val messages: Seq[RpcMessage] = definitions.filter(_.hasAnnotation("message")).map { defn =>
      RpcMessage(defn.name, defn.params)
    }

    val services: Seq[RpcService] = definitions.filter(_.hasAnnotation("service")).map { defn =>
      RpcService(defn.name, ServiceAlg(defn).requests.map(req =>
        RpcRequest(req.serialization, req.name.value, req.requestType, req.responseType, req.streamingType)))
    }
    // format: ON

    RpcDefinitions(outputName, outputPackage, options, messages, services)
  }
}
