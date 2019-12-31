---
layout: docs
title: Generating sources from Avro
permalink: /generate-sources-from-avro
---

# Generating sources from Avro

In this section we are going to explain how we can generate the different Scala structures using the `Avro` IDL.

To achieve this generation **Mu** use [avrohugger](https://github.com/julianpeeters/avrohugger) behind the scenes on the command `srcGen` which runs on compile time by default.

## Avro Protocols

Let's start from the beginning, everything on `Avro` should be declared inside a `protocol`.

The name of that protocol will be the name of our Scala file.

```plaintext
protocol People {
 ...
}
```

***srcGen =>***

`People.scala`

Furthermore, the `protocol` can have a `namespace` which will be our Scala package:

```plaintext
@namespace("example.protocol")
protocol People {
 ...
}
```

***srcGen =>***

`example.protocol.People.scala`

## Messages

On `Avro`, the messages are declared with the keyword `record` and contains different fields inside.
The `record` will be translated to a `case class` with the same fields on it:

```plaintext
record Person {
  string name;
  int age;
  boolean crossfitter;
}
```

***srcGen =>***

```tut:silent
final case class Person(name: String, age: Int, crossfitter: Boolean)
```

## Enums

`Avro` supports `enum`s too and they are translated to a Scala `Enumeration`:

```plaintext
enum Errors {
  NotFound, Duplicated, None
}
```

***srcGen =>***

```tut:silent
final object Errors extends Enumeration {
  type Errors = Value
  val NotFound, Duplicated, None = Value
}
```

## Unions

`Unions` are a complex `Avro` type for fields inside `record`s. 
As its name suggest, it represents a type composed by another types.

Depending on the types composing the `union`, `Mu` will interpret it on different ways:

### Optional fields

When we add a **`null`** to a `union` expression, we'll get a Scala `Option` of the other types declared along the `null`:

```plaintext
record PeopleRequest {
  union {null, string} name;
}
```

***srcGen =>***

```tut:silent
final case class PeopleRequest(name: Option[String])
```

### Eithers

When we join **`two non-null types`** on a `union` we'll get an Scala `Either` with the same types order:

```plaintext
record PeopleResponse {
  union { Errors, Person } result;
}
```

***srcGen =>***
  
```tut:silent
final case class PeopleResponse(result: Either[Errors.Value, Person])
```

### Coproducts

And finally, when we have **`three or more non-null types`** on a single `union`, 
we'll have a [shapeless](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#coproducts-and-discriminated-unions)' `Coproduct` on the same order as well:

```plaintext
record PeopleResponse {
  union{ string, int, Errors } result;
}
```

***srcGen =>***

```tut:silent
import shapeless.{:+:, CNil}

final case class PeopleResponse(result: String :+: Int :+: Errors.Value :+: CNil)
```
  
## Services

When we declare a method or `endpoint` inside a `protocol` this will be converted to a `trait` and intended as a **`Mu service`**.

As we would want to have our models separated from our services. `Avro` make us able to import other `Avro` files to use their `record`s:

```plaintext
protocol PeopleService {
  import idl "People.avdl"; //Under the same folder

  example.protocol.PeopleResponse getPerson(example.protocol.PeopleRequest request);

}
```

***srcGen =>***

```scala
@service(Avro) trait PeopleService[F[_]] {

  def getPerson(request: example.protocol.PeopleRequest): F[example.protocol.PeopleResponse]

}
```

Also, an endpoint can be declared without params or non returning anything and `Mu` will use its `Empty` type to cover these cases:

```plaintext
protocol PeopleService {

  void insertPerson();

}
```

***srcGen =>***

```scala
@service(Avro) trait PeopleService[F[_]] {

  def insertPerson(arg: Empty.type): F[Empty.type]

}
```

That's all from the *Mu* source generation from `Avro`. 
For a full understanding of the `Avro` syntax we recommend you to take a look to the [Avro Official site](http://avro.apache.org/docs/current/idl.html)
where you can find all the `Avro` supported types and some interesting resources.
