
# Routeguide-example

This module shows a simple example using [freestyle-rpc](https://github.com/frees-io/freestyle-rpc), based on the Route Guide Demo (using Scala annotations for service definitions),
from [this example in grpc-java](https://github.com/grpc/grpc-java/tree/v1.10.x/examples/src/main/java/io/grpc/examples/routeguide).

## Running the Example

Run server (interpreted to `cats.effect.IO` in this case):

```bash
sbt example-routeguide-server/runServer
```

Run client interpreting to `cats.effect.IO`:

```bash
sbt example-routeguide-client/runClientIO
```

Run client interpreting to `monix.eval.Task`:

```bash
sbt example-routeguide-client/runClientTask
```

## Generating the IDL files

```bash
sbt example-routeguide-protocol/idlGen
```

The previous command will overwrite [this proto file](protocol/src/main/resources/proto/service.proto).

(It will also generate [this Avro file](protocol/src/main/resources/avro/service.avpr) which will contain the messages but no RPC services since ours are annotated with `Protobuf`.)

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)
