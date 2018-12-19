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

package higherkindness.mu.rpc
package testing

import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType
import io.grpc.testing.TestMethodDescriptors

object methods {

  def voidMethod(methodName: Option[String] = None): MethodDescriptor[Void, Void] =
    MethodDescriptor
      .newBuilder[Void, Void]()
      .setType(MethodType.UNARY)
      .setFullMethodName(
        methodName.getOrElse(MethodDescriptor.generateFullMethodName("service_foo", "method_bar")))
      .setRequestMarshaller(TestMethodDescriptors.voidMarshaller())
      .setResponseMarshaller(TestMethodDescriptors.voidMarshaller())
      .build()

}
