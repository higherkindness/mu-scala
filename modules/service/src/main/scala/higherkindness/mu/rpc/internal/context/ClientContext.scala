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

package higherkindness.mu.rpc.internal.context

import cats.Monad
import cats.effect.Resource
import cats.syntax.all._
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}

final case class ClientContextMetaData[C](context: C, metadata: Metadata)

trait ClientContext[F[_], C] {

  def apply[Req, Res](
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions,
      current: C
  ): Resource[F, ClientContextMetaData[C]]

}

object ClientContext {
  def impl[F[_]: Monad, C](f: (C, Metadata) => F[Unit]): ClientContext[F, C] =
    new ClientContext[F, C] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          channel: Channel,
          options: CallOptions,
          current: C
      ): Resource[F, ClientContextMetaData[C]] = Resource.eval {
        new Metadata().pure[F].flatTap(f(current, _)).map(ClientContextMetaData(current, _))
      }
    }
}
