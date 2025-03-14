---
layout: docs
title: Testing an RPC service
section: tutorials
permalink: tutorials/testing-rpc-service
---

# Tutorial: Testing an RPC service

This tutorial will show you how to write a unit test for an RPC service using an
in-memory channel and client.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have read the [Getting Started guide](../getting-started)
* have followed the [gRPC server and client tutorial](grpc-server-client)

Mu supports both Protobuf and Avro. For the purposes of this tutorial we will
assume you are using Protobuf, but it's possible to follow the tutorial even if
you are using Avro.

## Service definition

Let's use the following service definition. (For an explanation of how to create
a service definition, check out the [RPC service definition with Protobuf
tutorial](service-definition/protobuf).)

A client sends a `HelloRequest` containing a name, and the server responds with
a greeting and an indication of whether it is feeling happy or not.

```scala
case class HelloRequest(name: String)
case class HelloResponse(greeting: String, happy: Boolean)

trait Greeter[F[_]] {
  def SayHello(req: HelloRequest): F[HelloResponse]
}
```

## Service implementation

Here's the implementation we want to test.

```scala mdoc:silent
import cats.Applicative
import cats.syntax.applicative.*
import mu.examples.protobuf.greeter.*

class HappyGreeter[F[_]: Applicative] extends Greeter[F] {

  def SayHello(req: HelloRequest): F[HelloResponse] =
    HelloResponse(s"Hello, ${req.name}!", happy = true).pure[F]

}
```

We're going to write a test to check that the service is always happy.

We'll use the [MUnit] testing library, with [munit-cats-effect] for smooth
integration with cats-effect `IO`.

## mu-rpc-testing

We'll also make use of the `mu-rpc-testing` module. This contains some helpers
for setting up an in-memory service for testing.

```
libraryDependencies += "io.higherkindness" %% "mu-rpc-testing" % "@VERSION@" % Test
```

## Create the service and client

The test will instantiate a `HappyGreeter`, but instead of connecting it to a
real gRPC server and exposing it over HTTP, we'll connect it to an in-memory
channel.

We'll also create a client and connect it to the same in-memory channel, so it
can make requests to the service.

```scala mdoc:silent
import mu.examples.protobuf.greeter.Greeter
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.testing.servers.withServerChannel

trait ServiceAndClient {

  given Greeter[IO] = new HappyGreeter[IO]

  /*
   * A cats-effect Resource that builds a gRPC server and client
   * connected to each other via an in-memory channel.
   */
  val clientResource: Resource[IO, Greeter[IO]] = for {
    sc        <- withServerChannel(Greeter.bindService[IO])
    clientRes <- Greeter.clientFromChannel[IO](IO.pure(sc.channel))
  } yield clientRes

}
```

The important part here is the use of `withServerChannel`. This is a helper method
provided by the `mu-rpc-testing` module that connects the service to an
in-memory channel so we don't need to start a real gRPC server.

## Write the test

Now we're ready to write our test.

With the service and client in place, the test consists of using the client to
make a request and then asserting that the response matches what we expect.

```scala mdoc:invisible
// MUnit's Location macro doesn't like running inside mdoc,
// so we work around it by providing a dummy Location to avoid
// invoking the macro
import munit.Location
given Location = Location.empty
```

```scala mdoc:silent
import mu.examples.protobuf.greeter.{HelloRequest, HelloResponse}
import munit.CatsEffectSuite

class ServiceSpec extends CatsEffectSuite with ServiceAndClient {

  test("service is happy") {
    clientResource
      .use(client => client.SayHello(HelloRequest("somebody")))
      .map(_.happy)
      .assertEquals(true)
  }

}
```

## Run the test

Let's see the test in action:

```
sbt:hello-mu-protobuf> tests/test
mu.examples.protobuf.greeter.ServiceSpec:
  + service is happy 0.314s
[info] Passed: Total 1, Failed 0, Errors 0, Passed 1
```

## Bonus points: property-based test

Since the `HappyGreeter` service is *always* happy, we could also write this as
a property-based test with ScalaCheck to verify that the service's happiness
does not depend on the incoming request.

```scala mdoc:silent
import munit.catseffect.IOFixture
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

class PropertyBasedServiceSpec extends CatsEffectSuite with ScalaCheckSuite with ServiceAndClient {

  val requestGen: Gen[HelloRequest] = Gen.alphaStr.map(HelloRequest(_))

  val client = ResourceSuiteLocalFixture("client", clientResource)

  override def munitFixtures: Seq[IOFixture[_]] = List(client)

  property("server is always happy") {
    val c = client()
    forAllNoShrink(requestGen) { request =>
      val response: HelloResponse = c.SayHello(request).unsafeRunSync()
      response.happy :| "response should be happy"
    }
  }

}
```

Let's run this test as well:

```
sbt:hello-mu-protobuf> tests/testOnly mu.examples.protobuf.greeter.PropertyBasedServiceSpec
mu.examples.protobuf.greeter.PropertyBasedServiceSpec:
  + server is always happy 0.243s
[info] Passed: Total 1, Failed 0, Errors 0, Passed 1
```

[MUnit]: https://scalameta.org/munit/
[munit-cats-effect]: https://github.com/typelevel/munit-cats-effect
