---
layout: docs
title: Core concepts
permalink: /core-concepts
---

# Core concepts

## About gRPC

> [gRPC](https://grpc.io/about/) is a modern, open source, and high-performance RPC framework that can run in any environment. It can efficiently connect services in and across data centers with pluggable support for load balancing, tracing, health checking, and authentication. It's also applicable in the last mile of distributed computing to connect devices, mobile applications, and browsers to backend services.

In this project, we are focusing on the [Java gRPC] implementation.

In the upcoming sections, we'll take a look at how we can implement RPC protocols (Messages and Services) in both *gRPC* and *Mu-RPC*.

## Messages and Services

### gRPC

As you might know, [gRPC] uses protocol buffers (a.k.a. *protobuf*) by default:

* As the Interface Definition Language (IDL) - for describing both the service interface and the structure of the payload messages. It is possible to use other alternatives if desired.
* For serializing/deserializing structured data - just as you define [JSON] data with a `.json` file extension, with protobufs you define files with `.proto` as an extension.

In the example given in the [gRPC guide], you might have a proto file like this:

```protobuf
message Person {
  string name = 1;
  int32 id = 2;
  bool has_ponycopter = 3;
}
```

Then, once you’ve specified your data structures, you can use the protobuf compiler `protoc` to generate data access classes in your preferred language(s) from your proto definition.

Likewise, you can define [gRPC] services in your proto files, with RPC method parameters and return types specified as protocol buffer messages:

```protobuf
// The greeter service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply);
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

Correspondingly, [gRPC] uses protoc with a special [gRPC] plugin to generate code from your proto file for this `Greeter` RPC service.

You can find more information about Protocol Buffers in the [Protocol Buffers' documentation](https://developers.google.com/protocol-buffers/docs/overview).

### Mu-RPC

In the previous section, we’ve seen an overview of what [gRPC] offers for defining protocols and generating code (compiling protocol buffers).
Now, we'll show how [Mu] offers the same thing, but following FP principles.

First things first, the main difference with respect to [gRPC] is that your protocols, both messages and services,
will reside with your business-logic in your Scala files using [scalamacros] annotations to set them up.
We’ll see more details on this shortly.

As a recommended approach, [Mu] generates the services and messages definitions in Scala code from `IDL` files.
We'll check out this feature further in [this section](generate-sources-from-idl).

Let’s start looking at how to define the `Person` message that we saw previously.
Before starting, this is the Scala import we need:

```scala mdoc:silent
import higherkindness.mu.rpc.protocol._
```

`Person` would be defined as follows:

```scala mdoc:silent
/**
 * Message Example.
 *
 * @param name Person name.
 * @param id Person Id.
 * @param has_ponycopter Has Ponycopter check.
 */
case class Person(name: String, id: Int, has_ponycopter: Boolean)
```

As we can see, this is quite simple. By the same token, let’s see now how the `Greeter` service would be translated to the [Mu] style (in your `.scala` file):

```scala mdoc:silent
object protocol1 {

  /**
    * The request message containing the user's name.
    * @param name User's name.
    */
  case class HelloRequest(name: String)

  /**
    * The response message,
    * @param message Message containing the greetings.
    */
  case class HelloReply(message: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
      * The greeter service definition.
      *
      * @param request Say Hello Request.
      * @return HelloReply.
      */
    def sayHello(request: HelloRequest): F[HelloReply]

  }
}
```

Naturally, the [RPC] services are grouped in a *Tagless Final* algebra.
Therefore, you only need to concentrate on the API that you want to expose as abstract smart constructors, without worrying how they will be implemented.

In the above example, we can see that `sayHello` returns an `F[HelloReply]`. However, very often the services might:

* Return an empty response.
* Receive an empty request.
* Return and receive a combination of both.

`Mu` provides an `Empty` object, defined at `mu.rpc.protocol`, that you might want to use for these purposes.

For instance:

```scala mdoc:silent
object protocol2 {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  case class HelloRequest(name: String)

  /**
   * The response message.
   * @param message Message containing the greetings.
   */
  case class HelloReply(message: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    def sayHello(request: HelloRequest): F[HelloReply]

    def emptyResponse(request: HelloRequest): F[Empty.type]

    def emptyRequest(request: Empty.type): F[HelloReply]

    def emptyRequestRespose(request: Empty.type): F[Empty.type]
  }
}
```

We are also using some additional annotations:

* `@service(Protobuf)`: tags the trait as an [RPC] service, in order to derive server and client code (macro expansion). It receives as its argument the type of serialization that will be used to encode/decode data. Our serialization type is `Protocol Buffers` in the example. `Avro` is also supported as another type of serialization.

We'll see more details about these and other annotations in the following sections.

## Compression

[Mu] allows us to compress the data we are sending in our services. We can enable this compression either on the server or the client side.

[Mu] supports `Gzip` as compression format.

For server side compression, we just have to add the annotation `Gzip` in our defined services.

Let's see an example of a unary service:

```scala mdoc:silent
object service1 {

  @service(Protobuf, Gzip)
  trait Greeter[F[_]] {

    /**
     * Unary RPC with Gzip compression
     *
     * @param empty Client request.
     * @return empty server response.
     */
    def emptyCompressed(empty: Empty.type): F[Empty.type]

  }

}
```

To enable compression on the client side, we just have to add an option to the client in the channel builder.

Let's see an example of a client with the compression enabled.

Since [Mu] relies on `ConcurrentEffect` from the [cats-effect library](https://github.com/typelevel/cats-effect), we'll need a runtime for executing our effects.

We'll be using `IO` from `cats-effect`, but you can use any type that has a `ConcurrentEffect` instance.

For executing `IO` we need a `ContextShift[IO]` used for running `IO` instances and a `Timer[IO]` that is used for scheduling, let's go ahead and create them.

*Note:* You'd need an implicit `monix.execution.Scheduler` in the case you're using Monix observables.

```scala mdoc:silent
trait CommonRuntime {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val timer: cats.effect.Timer[cats.effect.IO]     = cats.effect.IO.timer(EC)
  implicit val cs: cats.effect.ContextShift[cats.effect.IO] = cats.effect.IO.contextShift(EC)

}
```

```scala mdoc:silent
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.config._
import higherkindness.mu.rpc.config.channel._
import io.grpc.CallOptions
import service1._

trait ChannelImplicits extends CommonRuntime {

  val channelFor: ChannelFor =
    ConfigForAddress[IO]("rpc.host", "rpc.port").unsafeRunSync

  implicit val serviceClient: Resource[IO, Greeter[IO]] =
    Greeter.client[IO](channelFor, options = CallOptions.DEFAULT.withCompression("gzip"))
}

object implicits extends ChannelImplicits
```

## Service Methods

As [gRPC], [Mu] allows you to define two main kinds of service methods:

* **Unary RPC**: the simplest way of communicating, one client request, and one server response.
* **Streaming RPC**: similar to unary, but depending the kind of streaming, the client, server, or both will send back a stream of responses. There are three kinds of streaming, server, client and bidirectional streaming.

Let's complete our protocol's example with an unary service method:

```scala mdoc:silent
object service2 {

  case class HelloRequest(greeting: String)

  case class HelloResponse(reply: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * Unary RPC where the client sends a single request to the server and gets a single response back,
     * just like a normal function call.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client request.
     * @return Server response.
     */
    def sayHello(request: HelloRequest): F[HelloResponse]

  }

}
```

Here `sayHello` is our unary RPC.

In the [Streaming section](streaming), we are going to see all the streaming options.

## Custom codecs

[Mu] allows you to use custom decoders and encoders. It creates implicit `Marshaller` instances for your messages using existing serializers/deserializers for the serialization type you're using.

If you're using `Protobuf`, [Mu] uses instances of [PBDirect] for creating the `Marshaller` instances. For `Avro`, it uses instances of [Avro4s].

Let's see a couple of samples, one per each type of serialization. Suppose you want to serialize `java.time.LocalDate` as part of your messages in `String` format. With `Protobuf`, as we've mentioned, you need to provide the instances of [PBDirect] for that type. Specifically, you need to provide a `PBScalarValueWriter` and a `PBScalarValueReader`.

```scala mdoc:silent
object protocol3 {

  import java.time._
  import java.time.format._

  import com.google.protobuf.{CodedInputStream, CodedOutputStream}
  import pbdirect._

  import cats.syntax.contravariant._
  import cats.syntax.functor._

  implicit val localDateWriter: PBScalarValueWriter[LocalDate] =
    PBScalarValueWriter[String].contramap[LocalDate](_.format(DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val localDateReader: PBScalarValueReader[LocalDate] =
    PBScalarValueReader[String].map(string => LocalDate.parse(string, DateTimeFormatter.ISO_LOCAL_DATE))

  case class HelloRequest(name: String, date: LocalDate)

  case class HelloReply(message: String)

  @service(Protobuf)
  trait Greeter[F[_]] {

    def sayHello(request: HelloRequest): F[HelloReply]
  }
}
```

For `Avro` the process is quite similar, but in this case we need to provide three instances of three [Avro4s] type classes: `SchemaFor`, `Encoder`, and `Decoder`.

```scala mdoc:silent
object protocol4 {

  import java.time._
  import java.time.format._

  import com.sksamuel.avro4s._
  import org.apache.avro.Schema
  import org.apache.avro.Schema.Field

  implicit object LocalDateSchemaFor extends SchemaFor[LocalDate] {
    override def schema(fm: com.sksamuel.avro4s.FieldMapper): Schema =
      Schema.create(Schema.Type.STRING)
  }

  implicit object LocalDateEncoder extends Encoder[LocalDate] {
    override def encode(value: LocalDate, schema: Schema, fm: FieldMapper): String =
      value.format(DateTimeFormatter.ISO_LOCAL_DATE)
  }

  implicit object LocalDateDecoder extends Decoder[LocalDate] {
    override def decode(value: Any, schema: Schema, fm: FieldMapper): LocalDate =
      LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE)
  }

  case class HelloRequest(name: String, date: LocalDate)

  case class HelloReply(message: String)

  @service(Avro)
  trait Greeter[F[_]] {

    def sayHello(request: HelloRequest): F[HelloReply]
  }
}
```

[Mu] provides serializers for `BigDecimal`, `BigDecimal` with tagged 'precision' and 'scale' (like `BigDecimal @@ (Nat._8, Nat._2)`), `java.time.LocalDate` and `java.time.LocalDateTime`. The only thing you need to do is add the following import to your service:

* `BigDecimal` in `Protobuf`
  * `import higherkindness.mu.rpc.internal.encoders.pbd.bigDecimal._`
* `java.time.LocalDate`, `java.time.LocalDateTime` and `java.time.Instant` in `Protobuf`
  * `import higherkindness.mu.rpc.internal.encoders.pbd.javatime._`
* `BigDecimal` in `Avro` (**note**: this encoder is not avro spec compliant)
  * `import higherkindness.mu.rpc.internal.encoders.avro.bigdecimal._`
* Tagged `BigDecimal` in `Avro`
  * `import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._`
* `java.time.LocalDateTime` in `Avro`
  * `import higherkindness.mu.rpc.internal.encoders.avro.javatime._`

Mu also provides instances for `org.joda.time.LocalDate` and `org.joda.time.LocalDateTime`, but you need the `mu-rpc-marshallers-jodatime` extra dependency. See the [main section](/mu/scala/) for the SBT instructions.

* `org.joda.time.LocalDate` and `org.joda.time.LocalDateTime` in `Protobuf`
  * `import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.pbd._`
* `org.joda.time.LocalDate` and `org.joda.time.LocalDateTime` in `Avro`
  * `import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro._`

**Note**: If you want to send one of these instances directly as a request or response through Avro, you need to provide an instance of `Marshaller`. [Mu] provides the marshallers for `BigDecimal`, `java.time.LocalDate`, `java.time.LocalDateTime`, `java.time.Instant`, `org.joda.time.LocalDate` and `org.joda.time.LocalDateTime` in a separate package:
* `BigDecimal` in `Avro`
  * `import higherkindness.mu.rpc.internal.encoders.avro.bigdecimal.marshallers._`
* Tagged `BigDecimal` in `Avro` (**note**: this encoder is not avro spec compliant)
  * `import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged.marshallers._`
* `java.time.LocalDate`, `java.time.LocalDateTime` and `java.time.Instant` in `Avro`
  * `import higherkindness.mu.rpc.internal.encoders.avro.javatime.marshallers._`
* `org.joda.time.LocalDate` and `org.joda.time.LocalDateTime` in `Avro`
  * `import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro.marshallers._`

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[Mu]: https://github.com/higherkindness/mu
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[PBDirect]: https://github.com/47deg/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
[Avro4s]: https://github.com/sksamuel/avro4s

