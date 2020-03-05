---
layout: docs
title: Testing a gRPC server
section: tutorials
permalink: tutorials/testing-grpc-server
---

# Tutorial: Testing a gRPC server

This tutorial will show you how to write a unit test for a gRPC service using an
in-memory gRPC server and client.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have some understanding of Protobuf and `.proto` file syntax
* have read the [Getting Started guide](../getting-started)
* have followed the [gRPC server and client tutorial](grpc-server-client)

TODO write the tutorial. Here's the code for the test:

```scala
import cats.effect._
import com.example.hello._
import higherkindness.mu.rpc.server._
import higherkindness.mu.rpc.testing.servers.withServerChannel
import io.grpc.ManagedChannel
import org.scalatest.flatspec.AnyFlatSpec
import scala.concurrent.ExecutionContext.Implicits

class ServerSpec extends AnyFlatSpec {

  val EC: scala.concurrent.ExecutionContext = Implicits.global

  implicit val timer: Timer[IO]     = IO.timer(EC)
  implicit val cs: ContextShift[IO] = IO.contextShift(EC)

  implicit val greeter: Greeter[IO] = new HappyGreeter[IO]

  /*
   * A cats-effect Resource that builds a gRPC server and client
   * connected to each other via an in-memory channel.
   */
  val clientResource: Resource[IO, Greeter[IO]] = for {
    sc        <- withServerChannel(Greeter.bindService[IO])
    clientRes <- Greeter.clientFromChannel[IO](IO.pure(sc.channel))
  } yield clientRes

  behavior of "RPC server"

  it should "be happy" in {
    val response: HelloResponse = clientResource
      .use(client => client.SayHello(HelloRequest("somebody")))
      .unsafeRunSync()

    assert(response.happy === true)
  }

}
```
