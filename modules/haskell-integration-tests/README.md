# Mu-Haskell integration tests

This module contains a suite of integration tests to check that a Mu-Scala gRPC
server and a Mu-Haskell gRPC client can communicate, and vice versa.

## How it works

The tests are written in Scala. Depending on the test, they do one of the
following.

### Scala server, Haskell client

1. Start a Mu-Scala server in the same process as the tests
2. Use a Mu-Haskell client running in a Docker container to send requests to the
   server
3. Verify the response information that the client prints to stdout

This relies on the test knowing what responses the Mu-Scala server will send.

### Haskell server, Scala client

1. Start a Mu-Haskell server in a Docker container
2. Use a Mu-Scala client running in the same process as the tests to send
   requests to the server
3. Verify that the response is as expected.

This relies on the test knowing what responses the Mu-Haskell server will send.

## Haskell server and client

The Haskell code is managed as a Stack project in the `mu-haskell-client-server`
directory.

The Stack project is made up of 5 modules:

* `protocol`, where types are generated from the `weather.proto` and
  `weather.avdl` files
* `protobuf-server`
* `protobuf-client`
* `avro-server`
* `avro-client`

To build the server and client executables:

```
stack build
```

These executables are put in a Docker image for use in the tests. You can build
the image with:

```
./build.sh
```

This will take ages the first time you run it, as it needs to download and
compile the world. But the result will be cached, so subsequent builds will be
much faster.

## How to make a change

1. Make any necessary changes to the Haskell code in `mu-haskell-client-server`.
2. Rebuild the Haskell Docker image.
3. Make any necessary changes to the Scala code.
4. Run the tests to check they work: `sbt haskell-integration-tests/test`
5. Push the new Docker image to Docker Hub so the CI integration system can use it: `docker push`.
   (Currently only @cb372 can do this step. If it becomes a major bottleneck, we
   can migrate the image to a `higherkindness` organisation or something.)
6. Push your changes to GitHub
