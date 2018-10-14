---
layout: docs
title: References
permalink: /docs/rpc/references
---

# References

* [Freestyle](http://frees.io/)
* [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call)
* [gRPC](https://grpc.io/)
* [Protocol Buffers Docs](https://developers.google.com/protocol-buffers/docs/overview)
* [scalamacros](https://github.com/scalamacros/paradise)
* [PBDirect](https://github.com/btlines/pbdirect)
* [ScalaPB](https://scalapb.github.io/)
* [Monix](https://monix.io)
* [gRPC Java API](https://grpc.io/grpc-java/javadoc/)
* [Metrifier](https://github.com/47deg/metrifier)
* [FS2 Docs](https://github.com/functional-streams-for-scala/fs2)
* [HTTP/2](https://http2.github.io/)

## Comparing HTTP and RPC

This is not specifically about [frees-rpc]. Very often our microservices architectures are based on `HTTP` where perhaps, it is not the best glue to connect them, and [RPC] services might fit better.

[Metrifier] is a project where we compare, in different bounded ecosystems, `HTTP` and `RPC`. And it turns out RPC is usually faster than HTTP. If you're interested in learning more, we encourage to take a look at the documentation.

## Next Steps

If you want to delve deeper into [frees-rpc], we have a complete example at the [examples] module, which is based on the [Route Guide Demo](https://grpc.io/docs/tutorials/basic/java.html#generating-client-and-server-code) originally shared by the [gRPC Java Project](https://github.com/grpc/grpc-java/tree/6ea2b8aacb0a193ac727e061bc228b40121460e3/examples/src/main/java/io/grpc/examples/routeguide).

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: https://github.com/higherkindness/freestyle-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
[examples]: https://github.com/higherkindness/freestyle-rpc/tree/master/modules/examples
