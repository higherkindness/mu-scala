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
package avro

import freestyle.rpc.common._
import freestyle.rpc.protocol._
import cats.effect.Effect
import shapeless.{:+:, CNil, Coproduct}

object Utils extends CommonUtils {

  case class Request(a: String = "foo", b: Int = 123)
  case class RequestAddedBoolean(a: String, b: Int, c: Boolean = true)
  case class RequestAddedString(a: String, b: Int, c: String = "bar")
  case class RequestAddedInt(a: String, b: Int, c: Int = 456)
  case class RequestAddedNestedRequest(a: String, b: Int, c: Request = Request("bar", 456))
  case class RequestDroppedField(a: String)
  case class RequestCoproduct[A](a: A :+: CNil)

  case class Response(a: String, b: Int = 123)
  case class ResponseAddedBoolean(a: String, b: Int, c: Boolean)
  case class ResponseAddedString(a: String, b: Int, c: String)
  case class ResponseAddedInt(a: String, b: Int, c: Int)
  case class ResponseAddedNestedResponse(a: String, b: Int, c: Response)
  case class ResponseDroppedField(a: String)
  case class ResponseCoproduct[A](a: A :+: CNil)

  val request                   = Request("foo", 123)
  def requestCoproduct[A](a: A) = RequestCoproduct(Coproduct[A :+: CNil](a))

  val response                    = Response("foo", 123)
  val responseAddedBoolean        = ResponseAddedBoolean(response.a, response.b, true)
  val responseAddedString         = ResponseAddedString(response.a, response.b, "bar")
  val responseAddedInt            = ResponseAddedInt(response.a, response.b, 456)
  val responseAddedNestedResponse = ResponseAddedNestedResponse(response.a, response.b, response)
  val responseDroppedField        = ResponseDroppedField(response.a)
  def responseCoproduct[A](a: A)  = ResponseCoproduct(Coproduct[A :+: CNil](a))

  object service {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedBoolean {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: RequestAddedBoolean): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[RequestAddedBoolean]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedString {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: RequestAddedString): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[RequestAddedString]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedInt {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: RequestAddedInt): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[RequestAddedInt]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedNestedRequest {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: RequestAddedNestedRequest): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[RequestAddedNestedRequest]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestDroppedField {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: RequestDroppedField): F[Response]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[RequestDroppedField]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceResponseAddedBoolean {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[ResponseAddedBoolean]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedBoolean]]
    }
  }

  object serviceResponseAddedString {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[ResponseAddedString]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedString]]
    }
  }

  object serviceResponseAddedInt {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[ResponseAddedInt]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedInt]]
    }
  }

  object serviceResponseAddedNestedResponse {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[ResponseAddedNestedResponse]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedNestedResponse]]
    }
  }

  object serviceResponseDroppedField {
    @service
    trait RPCService[F[_]] {
      @rpc(AvroWithSchema) def get(a: Request): F[ResponseDroppedField]

      @rpc(AvroWithSchema) def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseDroppedField]]
    }
  }

  object handlers {

    class RPCServiceHandler[F[_]: Effect] extends service.RPCService[F] {
      def get(a: Request): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(ResponseCoproduct(Coproduct[Response :+: CNil](response)))
    }

    class RequestAddedBooleanRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedBoolean.RPCService[F] {
      def get(a: RequestAddedBoolean): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestAddedBoolean]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestAddedStringRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedString.RPCService[F] {
      def get(a: RequestAddedString): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestAddedString]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestAddedIntRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedInt.RPCService[F] {
      def get(a: RequestAddedInt): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestAddedInt]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestAddedNestedRequestRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedNestedRequest.RPCService[F] {
      def get(a: RequestAddedNestedRequest): F[Response] = Effect[F].delay(response)
      def getCoproduct(
          a: RequestCoproduct[RequestAddedNestedRequest]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestDroppedFieldRPCServiceHandler[F[_]: Effect]
        extends serviceRequestDroppedField.RPCService[F] {
      def get(a: RequestDroppedField): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestDroppedField]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class ResponseAddedBooleanRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedBoolean.RPCService[F] {
      def get(a: Request): F[ResponseAddedBoolean] = Effect[F].delay(responseAddedBoolean)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedBoolean]] =
        Effect[F].delay(responseCoproduct(responseAddedBoolean))
    }

    class ResponseAddedStringRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedString.RPCService[F] {
      def get(a: Request): F[ResponseAddedString] = Effect[F].delay(responseAddedString)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedString]] =
        Effect[F].delay(responseCoproduct(responseAddedString))
    }

    class ResponseAddedIntRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedInt.RPCService[F] {
      def get(a: Request): F[ResponseAddedInt] = Effect[F].delay(responseAddedInt)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedInt]] =
        Effect[F].delay(responseCoproduct(responseAddedInt))
    }

    class ResponseAddedNestedResponseRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedNestedResponse.RPCService[F] {
      def get(a: Request): F[ResponseAddedNestedResponse] =
        Effect[F].delay(responseAddedNestedResponse)
      def getCoproduct(
          a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedNestedResponse]] =
        Effect[F].delay(responseCoproduct(responseAddedNestedResponse))
    }

    class ResponseDroppedFieldRPCServiceHandler[F[_]: Effect]
        extends serviceResponseDroppedField.RPCService[F] {
      def get(a: Request): F[ResponseDroppedField] = Effect[F].delay(responseDroppedField)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseDroppedField]] =
        Effect[F].delay(responseCoproduct(responseDroppedField))
    }

  }

  trait FreesRuntime {

    import handlers._
    import freestyle.rpc.server._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val rpcServiceHandler: service.RPCService[ConcurrentMonad] =
      new RPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedBooleanRPCServiceHandler: serviceRequestAddedBoolean.RPCService[
      ConcurrentMonad] =
      new RequestAddedBooleanRPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedStringRPCServiceHandler: serviceRequestAddedString.RPCService[
      ConcurrentMonad] =
      new RequestAddedStringRPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedIntRPCServiceHandler: serviceRequestAddedInt.RPCService[
      ConcurrentMonad] =
      new RequestAddedIntRPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedNestedRequestRPCServiceHandler: serviceRequestAddedNestedRequest.RPCService[
      ConcurrentMonad] =
      new RequestAddedNestedRequestRPCServiceHandler[ConcurrentMonad]

    implicit val requestDroppedFieldRPCServiceHandler: serviceRequestDroppedField.RPCService[
      ConcurrentMonad] =
      new RequestDroppedFieldRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedBooleanRPCServiceHandler: serviceResponseAddedBoolean.RPCService[
      ConcurrentMonad] =
      new ResponseAddedBooleanRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedStringRPCServiceHandler: serviceResponseAddedString.RPCService[
      ConcurrentMonad] =
      new ResponseAddedStringRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedIntRPCServiceHandler: serviceResponseAddedInt.RPCService[
      ConcurrentMonad] =
      new ResponseAddedIntRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedNestedResponseRPCServiceHandler: serviceResponseAddedNestedResponse.RPCService[
      ConcurrentMonad] =
      new ResponseAddedNestedResponseRPCServiceHandler[ConcurrentMonad]

    implicit val responseDroppedFieldRPCServiceHandler: serviceResponseDroppedField.RPCService[
      ConcurrentMonad] =
      new ResponseDroppedFieldRPCServiceHandler[ConcurrentMonad]

    val rpcServiceConfigs: List[GrpcConfig] = List(
      AddService(service.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceRequestAddedBooleanConfigs: List[GrpcConfig] = List(
      AddService(serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceRequestAddedStringConfigs: List[GrpcConfig] = List(
      AddService(serviceRequestAddedString.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceRequestAddedIntConfigs: List[GrpcConfig] = List(
      AddService(serviceRequestAddedInt.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceRequestAddedNestedRequestConfigs: List[GrpcConfig] = List(
      AddService(serviceRequestAddedNestedRequest.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceRequestDroppedFieldConfigs: List[GrpcConfig] = List(
      AddService(serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceResponseAddedBooleanConfigs: List[GrpcConfig] = List(
      AddService(serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceResponseAddedStringConfigs: List[GrpcConfig] = List(
      AddService(serviceResponseAddedString.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceResponseAddedIntConfigs: List[GrpcConfig] = List(
      AddService(serviceResponseAddedInt.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceResponseAddedNestedResponseConfigs: List[GrpcConfig] = List(
      AddService(serviceResponseAddedNestedResponse.RPCService.bindService[ConcurrentMonad])
    )

    val rpcServiceResponseDroppedFieldConfigs: List[GrpcConfig] = List(
      AddService(serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad])
    )

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCServiceClient: service.RPCService.Client[ConcurrentMonad] =
      service.RPCService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
