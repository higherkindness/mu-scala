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
package channel.utils

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.ByteStreams
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

import io.grpc.MethodDescriptor

class StringMarshaller extends MethodDescriptor.Marshaller[String] {

  override def stream(value: String) = new ByteArrayInputStream(value.getBytes(UTF_8))

  override def parse(stream: InputStream): String =
    try new String(ByteStreams.toByteArray(stream), UTF_8)
    catch {
      case ex: IOException =>
        throw new RuntimeException(ex)
    }
}
