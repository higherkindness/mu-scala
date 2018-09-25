# Running Benchmarks

We are using the [Java Microbenchmark Harness (JMH)](http://openjdk.java.net/projects/code-tools/jmh/) tool.

## Protobuf Benchmarks

```bash
sbt "benchmarks/jmh:run -o proto-benchmark-results.txt -i 20 -wi 20 -f 2 -t 4 -r 1 -w 1 freestyle.rpc.benchmarks.ProtoBenchmark"
```

## Avro Benchmarks

```bash
sbt "benchmarks/jmh:run -o avro-benchmark-results.txt -i 20 -wi 20 -f 2 -t 4 -r 1 -w 1 freestyle.rpc.benchmarks.AvroBenchmark"
```

## AvroWithSchema Benchmarks

```bash
sbt "benchmarks/jmh:run -o avrowithschema-benchmark-results.txt -i 20 -wi 20 -f 2 -t 4 -r 1 -w 1 freestyle.rpc.benchmarks.AvroWithSchemaBenchmark"
```

## Parameters

In the commands above, the specified command line arguments mean "20 iterations", "20 warmup iterations", "2 forks", "4 threads". `r` and `w` are specifying the minimum time (seconds) to spend at each measurement warmup iteration/iteration.
