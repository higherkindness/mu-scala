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
  case class RequestAddedOptionalBoolean(a: String, b: Int, c: Option[Boolean] = None)
  case class RequestDroppedField(a: String)
  case class RequestReplacedType(a: String, c: Boolean = true)
  case class RequestRenamedField(a: String, c: Int = 0)
  case class RequestCoproduct[A](a: A :+: Int :+: String :+: CNil)
  case class RequestSuperCoproduct[A](a: A :+: Int :+: String :+: Boolean :+: CNil)
  case class RequestCoproductNoInt[A](
      b: A :+: String :+: CNil = Coproduct[A :+: String :+: CNil](""))

  case class Response(a: String, b: Int = 123)
  case class ResponseAddedBoolean(a: String, b: Int, c: Boolean)
  case class ResponseAddedOptionalBoolean(a: String, b: Int, c: Option[Boolean])
  case class ResponseReplacedType(a: String, b: Int = 123, c: Boolean)
  case class ResponseRenamedField(a: String, b: Int = 123, c: Int)
  case class ResponseDroppedField(a: String)
  case class ResponseCoproduct[A](a: A :+: Int :+: String :+: CNil)
  case class ResponseSuperCoproduct[A](
      a: A :+: Int :+: String :+: CNil = Coproduct[A :+: Int :+: String :+: CNil](0),
      b: A :+: Int :+: String :+: Boolean :+: CNil)
  case class ResponseCoproductNoInt[A](a: A :+: String :+: CNil)

  val request                   = Request("foo", 123)
  def requestCoproduct[A](a: A) = RequestCoproduct(Coproduct[A :+: Int :+: String :+: CNil](a))
  val requestCoproductInt       = RequestCoproduct(Coproduct[Request :+: Int :+: String :+: CNil](1))

  val response                        = Response("foo", 123)
  val responseAddedBoolean            = ResponseAddedBoolean(response.a, response.b, true)
  val responseReplacedType            = ResponseReplacedType(a = response.a, c = true)
  val responseRenamedField            = ResponseRenamedField(a = response.a, c = 456)
  val responseDroppedField            = ResponseDroppedField(response.a)
  def responseCoproduct[A](a: A)      = ResponseCoproduct(Coproduct[A :+: Int :+: String :+: CNil](a))
  def responseCoproductNoInt[A](a: A) = ResponseCoproductNoInt(Coproduct[A :+: String :+: CNil](a))

  //Original Service

  object service {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: Request): F[Response]

      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[Response]]
    }
  }

  //Updated request services

  object serviceRequestAddedBoolean {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: RequestAddedBoolean): F[Response]

      def getCoproduct(a: RequestCoproduct[RequestAddedBoolean]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedOptionalBoolean {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: RequestAddedOptionalBoolean): F[Response]

      def getCoproduct(
          a: RequestCoproduct[RequestAddedOptionalBoolean]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestAddedCoproductItem {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def getCoproduct(a: RequestSuperCoproduct[Request]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestRemovedCoproductItem {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def getCoproduct(a: RequestCoproductNoInt[Request]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestDroppedField {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: RequestDroppedField): F[Response]

      def getCoproduct(a: RequestCoproduct[RequestDroppedField]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestReplacedType {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: RequestReplacedType): F[Response]

      def getCoproduct(a: RequestCoproduct[RequestReplacedType]): F[ResponseCoproduct[Response]]
    }
  }

  object serviceRequestRenamedField {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: RequestRenamedField): F[Response]

      def getCoproduct(a: RequestCoproduct[RequestRenamedField]): F[ResponseCoproduct[Response]]
    }
  }

  //Updated response services

  object serviceResponseAddedBoolean {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: Request): F[ResponseAddedBoolean]

      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedBoolean]]
    }
  }

  object serviceResponseAddedBooleanCoproduct {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseSuperCoproduct[Response]]
    }
  }

  object serviceResponseRemovedIntCoproduct {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproductNoInt[Response]]
    }
  }

  object serviceResponseReplacedType {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: Request): F[ResponseReplacedType]

      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseReplacedType]]
    }
  }

  object serviceResponseRenamedField {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: Request): F[ResponseRenamedField]

      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseRenamedField]]
    }
  }

  object serviceResponseDroppedField {
    @service(AvroWithSchema)
    trait RPCService[F[_]] {
      def get(a: Request): F[ResponseDroppedField]

      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseDroppedField]]
    }
  }

  object handlers {

    class RPCServiceHandler[F[_]: Effect] extends service.RPCService[F] {
      def get(a: Request): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(
          ResponseCoproduct(Coproduct[Response :+: Int :+: String :+: CNil](response)))
    }

    class RequestAddedBooleanRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedBoolean.RPCService[F] {
      def get(a: RequestAddedBoolean): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestAddedBoolean]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestAddedOptionalBooleanRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedOptionalBoolean.RPCService[F] {
      def get(a: RequestAddedOptionalBoolean): F[Response] = Effect[F].delay(response)
      def getCoproduct(
          a: RequestCoproduct[RequestAddedOptionalBoolean]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestAddedCoproductItemRPCServiceHandler[F[_]: Effect]
        extends serviceRequestAddedCoproductItem.RPCService[F] {
      def getCoproduct(a: RequestSuperCoproduct[Request]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestRemovedCoproductItemRPCServiceHandler[F[_]: Effect]
        extends serviceRequestRemovedCoproductItem.RPCService[F] {
      def getCoproduct(a: RequestCoproductNoInt[Request]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestDroppedFieldRPCServiceHandler[F[_]: Effect]
        extends serviceRequestDroppedField.RPCService[F] {
      def get(a: RequestDroppedField): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestDroppedField]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestReplacedTypeRPCServiceHandler[F[_]: Effect]
        extends serviceRequestReplacedType.RPCService[F] {
      def get(a: RequestReplacedType): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestReplacedType]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class RequestRenamedFieldRPCServiceHandler[F[_]: Effect]
        extends serviceRequestRenamedField.RPCService[F] {
      def get(a: RequestRenamedField): F[Response] = Effect[F].delay(response)
      def getCoproduct(a: RequestCoproduct[RequestRenamedField]): F[ResponseCoproduct[Response]] =
        Effect[F].delay(responseCoproduct(response))
    }

    class ResponseAddedBooleanRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedBoolean.RPCService[F] {
      def get(a: Request): F[ResponseAddedBoolean] = Effect[F].delay(responseAddedBoolean)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseAddedBoolean]] =
        Effect[F].delay(responseCoproduct(responseAddedBoolean))
    }

    class ResponseAddedBooleanCoproductRPCServiceHandler[F[_]: Effect]
        extends serviceResponseAddedBooleanCoproduct.RPCService[F] {
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseSuperCoproduct[Response]] =
        Effect[F].delay(
          ResponseSuperCoproduct(
            b = Coproduct[Response :+: Int :+: String :+: Boolean :+: CNil](true)))
    }

    class ResponseRemovedIntCoproductRPCServiceHandler[F[_]: Effect]
        extends serviceResponseRemovedIntCoproduct.RPCService[F] {
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproductNoInt[Response]] =
        Effect[F].delay(responseCoproductNoInt(response))
    }

    class ResponseReplacedTypeRPCServiceHandler[F[_]: Effect]
        extends serviceResponseReplacedType.RPCService[F] {
      def get(a: Request): F[ResponseReplacedType] = Effect[F].delay(responseReplacedType)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseReplacedType]] =
        Effect[F].delay(responseCoproduct(responseReplacedType))
    }

    class ResponseRenamedFieldRPCServiceHandler[F[_]: Effect]
        extends serviceResponseRenamedField.RPCService[F] {
      def get(a: Request): F[ResponseRenamedField] = Effect[F].delay(responseRenamedField)
      def getCoproduct(a: RequestCoproduct[Request]): F[ResponseCoproduct[ResponseRenamedField]] =
        Effect[F].delay(responseCoproduct(responseRenamedField))
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

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val rpcServiceHandler: service.RPCService[ConcurrentMonad] =
      new RPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedBooleanRPCServiceHandler: serviceRequestAddedBoolean.RPCService[
      ConcurrentMonad] =
      new RequestAddedBooleanRPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedOptionalBooleanRPCServiceHandler: serviceRequestAddedOptionalBoolean.RPCService[
      ConcurrentMonad] =
      new RequestAddedOptionalBooleanRPCServiceHandler[ConcurrentMonad]

    implicit val requestAddedCoproductItemRPCServiceHandler: serviceRequestAddedCoproductItem.RPCService[
      ConcurrentMonad] =
      new RequestAddedCoproductItemRPCServiceHandler[ConcurrentMonad]

    implicit val requestRemovedCoproductItemRPCServiceHandler: serviceRequestRemovedCoproductItem.RPCService[
      ConcurrentMonad] =
      new RequestRemovedCoproductItemRPCServiceHandler[ConcurrentMonad]

    implicit val requestDroppedFieldRPCServiceHandler: serviceRequestDroppedField.RPCService[
      ConcurrentMonad] =
      new RequestDroppedFieldRPCServiceHandler[ConcurrentMonad]

    implicit val requestReplacedTypeRPCServiceHandler: serviceRequestReplacedType.RPCService[
      ConcurrentMonad] =
      new RequestReplacedTypeRPCServiceHandler[ConcurrentMonad]

    implicit val requestRenamedFieldRPCServiceHandler: serviceRequestRenamedField.RPCService[
      ConcurrentMonad] =
      new RequestRenamedFieldRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedBooleanRPCServiceHandler: serviceResponseAddedBoolean.RPCService[
      ConcurrentMonad] =
      new ResponseAddedBooleanRPCServiceHandler[ConcurrentMonad]

    implicit val responseAddedBooleanCoproductRPCServiceHandler: serviceResponseAddedBooleanCoproduct.RPCService[
      ConcurrentMonad] =
      new ResponseAddedBooleanCoproductRPCServiceHandler[ConcurrentMonad]

    implicit val responseRemovedIntCoproductRPCServiceHandler: serviceResponseRemovedIntCoproduct.RPCService[
      ConcurrentMonad] =
      new ResponseRemovedIntCoproductRPCServiceHandler[ConcurrentMonad]

    implicit val responseReplacedTypeRPCServiceHandler: serviceResponseReplacedType.RPCService[
      ConcurrentMonad] =
      new ResponseReplacedTypeRPCServiceHandler[ConcurrentMonad]

    implicit val responseRenamedFieldRPCServiceHandler: serviceResponseRenamedField.RPCService[
      ConcurrentMonad] =
      new ResponseRenamedFieldRPCServiceHandler[ConcurrentMonad]

    implicit val responseDroppedFieldRPCServiceHandler: serviceResponseDroppedField.RPCService[
      ConcurrentMonad] =
      new ResponseDroppedFieldRPCServiceHandler[ConcurrentMonad]

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCServiceClient: service.RPCService.Client[ConcurrentMonad] =
      service.RPCService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
