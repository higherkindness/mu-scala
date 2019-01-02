/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc
package testing

import java.net.URI

import io.grpc.Attributes
import io.grpc.NameResolver

object client {

  case class FakeNameResolverFactory(expectedScheme: String) extends NameResolver.Factory {
    def newNameResolver(targetUri: URI, params: Attributes): NameResolver =
      if (expectedScheme == targetUri.getScheme) FakeNameResolver(targetUri) else null

    override def getDefaultScheme: String = expectedScheme
  }

  case class FakeNameResolver(uri: URI) extends NameResolver {
    override def getServiceAuthority: String = uri.getAuthority

    override def start(listener: NameResolver.Listener): Unit = {}

    override def shutdown(): Unit = {}
  }

}
