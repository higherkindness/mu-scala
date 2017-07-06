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
package rpc

import scala.annotation.StaticAnnotation

package object protocol {

  class service extends StaticAnnotation

  class rpc extends StaticAnnotation

  class stream[S <: StreamingType] extends StaticAnnotation

  class message extends StaticAnnotation

  class option(val name: String, val value: String, val quote: Boolean) extends StaticAnnotation

  sealed trait StreamingType         extends Product with Serializable
  case object RequestStreaming       extends StreamingType
  case object ResponseStreaming      extends StreamingType
  case object BidirectionalStreaming extends StreamingType

}
