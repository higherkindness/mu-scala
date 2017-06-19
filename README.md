
[comment]: # (Start Badges)

[![Build Status](https://travis-ci.org/frees-io/freestyle-rpc.svg?branch=master)](https://travis-ci.org/frees-io/freestyle-rpc) [![codecov.io](http://codecov.io/github/frees-io/freestyle-rpc/coverage.svg?branch=master)](http://codecov.io/github/frees-io/freestyle-rpc?branch=master) [![Maven Central](https://img.shields.io/badge/maven%20central-0.0.1-green.svg)](https://oss.sonatype.org/#nexus-search;gav~io.frees~freestyle*) [![Latest version](https://img.shields.io/badge/freestyle--rpc-0.0.1-green.svg)](https://index.scala-lang.org/frees-io/freestyle-rpc) [![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/frees-io/freestyle-rpc/master/LICENSE) [![Join the chat at https://gitter.im/47deg/freestyle](https://badges.gitter.im/47deg/freestyle.svg)](https://gitter.im/47deg/freestyle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![GitHub Issues](https://img.shields.io/github/issues/frees-io/freestyle-rpc.svg)](https://github.com/frees-io/freestyle-rpc/issues) [![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org)

[comment]: # (End Badges)

# freestyle-rpc

Simple RPC with Freestyle

## Greeting Demo

Run server:

```
sbt runServer
```

Run client:

```
sbt runClient
```

## User Demo

Based on https://github.com/grpc-ecosystem/grpc-gateway.

[grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway) is a plugin of protoc. It reads gRPC service definition, and generates a reverse-proxy server which translates a RESTful JSON API into gRPC. This server is generated according to custom options in your gRPC definition. 
This server is generated according to [custom options](https://cloud.google.com/service-management/reference/rpc/google.api#http) in your gRPC definition.

### Prerequisites

It's mandatory to follow these [instructions](https://github.com/grpc-ecosystem/grpc-gateway#installation) before proceeding. You might want use `brew install protobuf` if you're using OSX.

And then:

```bash
$ brew install go
$ go get -u github.com/golang/protobuf/{proto,protoc-gen-go}
$ go get -u github.com/grpc-ecosystem/grpc-gateway/protoc-gen-grpc-gateway
$ go get -u github.com/grpc-ecosystem/grpc-gateway/protoc-gen-swagger
$ go get -u -v google.golang.org/grpc
$ go get -u github.com/golang/protobuf/protoc-gen-go
```

Finally, make sure that your `$GOPATH/bin` is in your `$PATH`.

### Troubleshooting

#### `failed to run aclocal: No such file or directory`

The development release of a program source code often comes with `autogen.sh` which is used to prepare a build process, including verifying program functionality and generating configure script. This `autogen.sh` script then relies on `autoreconf` to invoke `autoconf`, `automake`, `aclocal` and other related tools.

The missing `aclocal` is part of `automake` package. Thus, to fix this error, install `automake` package.

* `OSX`: 

https://gist.github.com/justinbellamy/2672db1c78f024f2d4fe

* `Debian`, `Ubuntu` or `Linux Mint`:

```bash
$ sudo apt-get install automake
```

* `CentOS`, `Fedora` or `RHEL`:

```bash
$ sudo yum install automake
```

## Run Demo

### Running the Server

```
sbt -Dgo.path=$GOPATH ";project demo-http;runMain freestyle.rpc.demo.user.UserServerApp"
```

### Running the Client

Now, you could invoke the service:

* Using the client, as usual:

```
sbt -Dgo.path=$GOPATH ";project demo-http;runMain freestyle.rpc.demo.user.UserClientApp"
```

### Generating and Running the Gateway

You could generate a reverse proxy and writing an endpoint as it's described [here](https://github.com/grpc-ecosystem/grpc-gateway#usage).

To run the gateway:

```bash
go run demo/gateway/server/entry.go
```

Then, you could use `curl` or similar to fetch the user over `HTTP`:

```bash
curl -X POST \
     -H "Content-Type: application/json" \
     -H "Cache-Control: no-cache" \
     -H "Postman-Token: 1e813409-6aa6-8cd1-70be-51305f31667f" \
     -d '{
        "password" : "password"
     }' "http://127.0.0.1:8080/v1/frees"
```

HTTP Response:

```bash
{"name":"Freestyle","email":"hello@frees.io"}%
```

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)