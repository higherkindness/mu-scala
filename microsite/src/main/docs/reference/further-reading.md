---
layout: docs
title: Further reading
section: reference
permalink: /reference/further-reading
---

# Useful links

* [Higherkindness](https://higherkindness.io/)
* [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call)
* [gRPC](https://grpc.io/)
* [Avro](https://avro.apache.org/)
* [Protocol Buffers Docs](https://developers.google.com/protocol-buffers/docs/overview)
* [FS2 Docs](https://github.com/functional-streams-for-scala/fs2)
* [gRPC Java API](https://grpc.io/grpc-java/javadoc/)
* [Metrifier](https://github.com/47deg/metrifier)
* [HTTP/2](https://http2.github.io/)

## Comparing HTTP and RPC

This is not specifically about [Mu]. Very often our microservices architectures are based on HTTP with JSON serialization, but perhaps it is not the best glue to connect them, and [RPC] services might be a better fit.

[Metrifier] is a project where we compare, in different bounded ecosystems, HTTP and RPC. We found that RPC is usually faster than HTTP + JSON. If you're interested in learning more, we encourage you to take a look at the documentation.

## Next Steps

If you want to dive deeper into [Mu], we have a complete example in the [examples] repo, which is based on the [Route Guide Demo](https://grpc.io/docs/tutorials/basic/java.html#generating-client-and-server-code) originally shared by the [gRPC Java Project](https://github.com/grpc/grpc-java/tree/6ea2b8aacb0a193ac727e061bc228b40121460e3/examples/src/main/java/io/grpc/examples/routeguide).

[examples]: https://github.com/higherkindness/mu-scala-examples
[Metrifier]: https://github.com/47deg/metrifier
[Mu]: https://github.com/higherkindness/mu
[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
