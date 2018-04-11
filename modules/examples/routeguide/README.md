
# Route Guide Example

This module shows a simple example using [freestyle-rpc](https://github.com/frees-io/freestyle-rpc), based on the Route Guide Demo (using Scala annotations for service definitions),
from [this example in grpc-java](https://github.com/grpc/grpc-java/tree/v1.10.x/examples/src/main/java/io/grpc/examples/routeguide).

## Running the Example

Run server (interpreted to `cats.effect.IO` in this case):

```bash
sbt "project example-routeguide-server" "run"
```

Run client interpreting to `cats.effect.IO`:

```bash
sbt "project example-routeguide-client" "runClientIO"
```

Run client interpreting to `monix.eval.Task`:

```bash
sbt "project example-routeguide-client" "runClientTask"
```

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)
