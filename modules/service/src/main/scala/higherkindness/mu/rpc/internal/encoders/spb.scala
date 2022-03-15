package higherkindness.mu.rpc.internal.encoders

import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scalapb.grpc.{Marshaller => SPBMarshaller}
import io.grpc.MethodDescriptor.Marshaller

object spb {

  implicit def scalapbGeneratedMessageMarshaller[A <: GeneratedMessage](implicit
      comp: GeneratedMessageCompanion[A]
  ): Marshaller[A] =
    SPBMarshaller.forMessage[A]

}
