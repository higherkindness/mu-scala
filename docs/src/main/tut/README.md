---
layout: docs
title: RPC
permalink: /docs/rpc
---

# Freestyle-RPC

[RPC] atop **Freestyle** is **`frees-rpc`**.

Freestyle RPC is a purely functional library for building RPC endpoint-based services with support for [RPC] and [HTTP/2].

## Comparing HTTP and RPC

This is not specifically about [frees-rpc]. Very often our microservices architectures are based on `HTTP` where perhaps, it is not the best glue to connect them, and [RPC] services might fit better.

[Metrifier] is a project where we compare, in different bounded ecosystems, `HTTP` and `RPC`. And it turns out RPC is usually faster than HTTP. If you're interested in learning more, we encourage to take a look at the documentation.

## Next Steps

If you want to delve deeper into [frees-rpc], we have a complete example at the [freestyle-rpc-examples] repository, which is based on the [Route Guide Demo](https://grpc.io/docs/tutorials/basic/java.html#generating-client-and-server-code) originally shared by the [gRPC Java Project](https://github.com/grpc/grpc-java/tree/6ea2b8aacb0a193ac727e061bc228b40121460e3/examples/src/main/java/io/grpc/examples/routeguide).