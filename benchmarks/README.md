# Benchmarks

We are using the [Java Microbenchmark Harness (JMH)](http://openjdk.java.net/projects/code-tools/jmh/) tool.

## Running Benchmarks Manually

### Protobuf Benchmarks

Last released version:

```bash
sbt "benchmarks-vprev/jmh:run -o proto-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.ProtoBenchmark"
```

Next version:

```bash
sbt "benchmarks-vnext/jmh:run -o proto-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.ProtoBenchmark"
```

### Avro Benchmarks

Last released version:

```bash
sbt "benchmarks-vprev/jmh:run -o avro-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.AvroBenchmark"
```

Next version:

```bash
sbt "benchmarks-vnext/jmh:run -o avro-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.AvroBenchmark"
```

### AvroWithSchema Benchmarks

Last released version:

```bash
sbt "benchmarks-vprev/jmh:run -o avrowithschema-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.AvroWithSchemaBenchmark"
```

Next version:

```bash
sbt "benchmarks-vnext/jmh:run -o avrowithschema-benchmark-results.txt -i 10 -wi 10 -f 2 -t 1 -r 1 -w 1 mu.rpc.benchmarks.AvroWithSchemaBenchmark"
```

### Parameters

In the commands above, the specified command line arguments mean "10 iterations", "10 warmup iterations", "2 forks", "1 threads". `r` and `w` are specifying the minimum time (seconds) to spend at each measurement warmup iteration/iteration.

## Running Benchmarks - Scripts

The next command will run all the available benchmarks in the project:

```bash
benchmarks/run-benchmarks-all
```

Then, we can aggregate the results to compare each other: the current (work in progress) with the last released version:

```bash
benchmarks/aggregate /path/to/the/results/directory
```

### Credits

These scripts are based on the ones from the great [Monix](https://github.com/monix/monix) library.