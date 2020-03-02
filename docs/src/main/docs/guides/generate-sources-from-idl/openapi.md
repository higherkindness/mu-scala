---
layout: docs
title: Generating sources from OpenAPI specifications
section: guides
permalink: /guides/generate-sources-from-openapi
---

# Generating sources from OpenAPI specifications

## Getting started

First add the sbt plugin in `project/plugins.sbt`:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "0.20.1")
```

[comment]: # (End Replace)

Then configure the plugin by adding a few lines to `build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._

// Look for OpenAPI IDL files
muSrcGenIdlType := IdlType.OpenAPI

// Generate code that is compatible with http4s v0.20.x
muSrcGenOpenApiHttpImpl := higherkindness.mu.rpc.idlgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20
```

Finally add the appropriate Circe and http4s dependencies so the generated code
will compile:

```
libraryDependencies ++= Seq(
  "io.circe"   %% "circe-core"          % "0.12.3",
  "io.circe"   %% "circe-generic"       % "0.12.3",
  "org.http4s" %% "http4s-blaze-client" % "0.20.16",
  "org.http4s" %% "http4s-circe"        % "0.20.16"
)
```

Suppose you want to generate Scala code for a REST service based on the
"Petstore" example OpenAPI IDL file (available for download [here](https://github.com/OAI/OpenAPI-Specification/blob/master/examples/v3.0/petstore.yaml)), `src/main/resources/petstore/petstore.yaml`.

You can run the source generator directly:

```sh
$ sbt muSrcGen
```

or as part of compilation:

```sh
$ sbt compile
```

Once the source generator has run, there should be a generated Scala file at
`target/scala-2.12/src_managed/main/petstore/petstore.scala`.

The file is very large so we won't show it here, but it contains:

* case classes for all the models
* Circe `Encoder`/`Decoder`s and http4s `EntityEncoder`/`EntityDecoder`s for all models
* An interface for a client for the REST API:

    ```scala
    trait PetstoreClient[F[_]] {
      import PetstoreClient._
      def getPets(limit: Option[Int], name: Option[String]): F[Pets]
      def createPet(newPet: NewPet): F[Either[CreatePetErrorResponse, Unit]]
      def getPet(petId: Int): F[Either[GetPetErrorResponse, Pet]]
      def deletePet(petId: Int): F[Unit]
      def updatePet(petId: Int, updatePet: UpdatePet): F[Unit]
    }
    ```
* An object containing factory methods to build an http4s-based client:

    ```scala
    object PetstoreHttpClient {
      def build[F[_]: Effect: Sync](client: Client[F], baseUrl: Uri)(implicit ...): PetstoreClient[F] = ...
      def apply[F[_]: ConcurrentEffect](baseUrl: Uri)(implicit ...): Resource[F, PetstoreClient[F]] = ...
    }
    ```

## Working example

For a full working example project including both a server and a client, check out
[47deg/petstore4s](https://github.com/47deg/petstore4s).
