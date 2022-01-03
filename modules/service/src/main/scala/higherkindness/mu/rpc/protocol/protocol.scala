/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.protocol

sealed trait StreamingType         extends Product with Serializable
case object RequestStreaming       extends StreamingType
case object ResponseStreaming      extends StreamingType
case object BidirectionalStreaming extends StreamingType

sealed trait SerializationType extends Product with Serializable
case object Protobuf           extends SerializationType
case object Avro               extends SerializationType
case object AvroWithSchema     extends SerializationType
case object Custom             extends SerializationType

sealed abstract class CompressionType extends Product with Serializable
case object Identity                  extends CompressionType
case object Gzip                      extends CompressionType

sealed abstract class MethodNameStyle extends Product with Serializable
case object Unchanged                 extends MethodNameStyle
case object Capitalize                extends MethodNameStyle

object Empty
