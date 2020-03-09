---
layout: docs
title: Backward/Forward Data Evolution
section: reference
permalink: /reference/schema-evolution
---

# Backward/Forward Data Evolution

This section is about how data flows through the network, and how are they encoded/decoded into/from bytes in both sides of the wire in a compatible way.

Currently, [Mu] brings the ability to encode data in bytes based on Avro and Protocol buffers. In the next sections, we are going to pass through both serialization standards to see how to preserve both forward and backward compatibility in your system:

* [Avro](schema-evolution/avro)
* [Protocol Buffers](schema-evolution/proto)

[Mu]: https://github.com/higherkindness/mu-scala
