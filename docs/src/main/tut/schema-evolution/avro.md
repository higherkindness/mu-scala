---
layout: docs
title: Schema Evolution - Avro
permalink: /schema-evolution/avro
---

# Avro - Schema Evolution

From now on, consider that we are using `AvroWithSchema` as the serialization mechanism in your [mu] program.

According to the [Avro Specs](http://avro.apache.org/docs/current/spec.html#Schema+Resolution):

> A reader of Avro data, whether from an RPC or a file, can always parse that data because its schema is provided. But that schema may not be exactly the schema that was expected. For example, if the data was written with a different version of the software than it is read, then records may have had fields added or removed.

For Scala, this section specifies how such schema differences should be resolved to preserve compatibility. We'll try to summarise a bit all the possible cases in both ends: request and response. However, you could go deeper by using this [repo](https://github.com/higherkindness/mu-protocol-decimal-update) where you can play with all of the possibilities.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Cases

- [Modifying the Request (Client side)](#modifying-the-request-client-side)
  - [A: Adding a new non-optional field](#a-adding-a-new-non-optional-field)
  - [B: Adding a new optional field](#b-adding-a-new-optional-field)
  - [C: Adding new item to a union](#c-adding-new-item-to-a-union)
  - [D: Removing item from a union](#d-removing-item-from-a-union)
  - [E: Replacing item in a union](#e-replacing-item-in-a-union)
  - [F: Changing the type of an existing field](#f-changing-the-type-of-an-existing-field)
  - [G: Renaming a field](#g-renaming-a-field)
  - [H: Removing a field](#h-removing-a-field)
- [Modifying the Response (Server side)](#modifying-the-response-server-side)
  - [I: Adding a new non-optional field](#i-adding-a-new-non-optional-field)
  - [J: Adding a new optional field](#j-adding-a-new-optional-field)
  - [K: Adding a new item to a union](#k-adding-a-new-item-to-a-union)
  - [L: Removing item from a union](#l-removing-item-from-a-union)
  - [M: Replacing item from a union](#m-replacing-item-from-a-union)
  - [N: Changing the type of an existing field](#n-changing-the-type-of-an-existing-field)
  - [O: Renaming a field](#o-renaming-a-field)
  - [P: Removing a field](#p-removing-a-field)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### Modifying the Request (Client side)

#### A: Adding a new non-optional field

You need to specify a default value for the new field.

* Before:

```scala
case class Request(a: String, b: Int)
```

* After:

```scala
case class NewRequest(a: String, b: Int, c: Boolean = true)
```

#### B: Adding a new optional field

This is a particular case of the previous scenario, hence, the solution could be providing a default value, that presumably, in Scala would be `None`.

#### C: Adding new item to a union

In this case, we are safe and no actions are required.

* Before:

```scala
case class Request(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewRequest(a: Int :+: String :+: Boolean :+: CNil)
```

#### D: Removing item from a union

In this case, we'd be breaking the compatibility. The only way to deal with this situation is considering this as a change of type, see the next case for details.

* Before:

```scala
case class Request(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewRequest(b: String :+: CNil = Coproduct[String :+: CNil](""))
```

#### E: Replacing item in a union

As we saw previously, again we are breaking the compatibility. If the type is replaced, it will work while the provided value is one of the types in common between the previous `coproduct` and the new one.

* Before:

```scala
case class Request(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewRequest(a: Int :+: Boolean :+: CNil)
```

It will work if the request is:

```scala
Request(a = Coproduct[Int :+: String :+: CNil](10))
```

And it will fail if the request is:

```scala
Request(a = Coproduct[Int :+: String :+: CNil]("Hi"))
```

#### F: Changing the type of an existing field

In this case, it's not possible to deal with a type swap, if we are maintaining the same name of the field. So the solution would be to consider this replacement as a combination of removing the old field/type, and adding a new field with the new type with a default value.

* Before:

```scala
case class Request(a: String, b: Int)
```

* After:

```scala
case class NewRequest(a: String, c: Boolean = true)
```

#### G: Renaming a field

This operation is completely safe, in Avro, the names are not being sent as part of the request, so no matter how they are named.

* Before:

```scala
case class Request(a: String, b: Int)
```

* After:

```scala
case class NewRequest(a: String, c: Int)
```

#### H: Removing a field

No action required. However, keep in mind that the value will be ignored when old clients include it in the request.

* Before:

```scala
case class Request(a: String, b: Int)
```

* After:

```scala
case class NewRequest(a: String)
```

### Modifying the Response (Server side)

#### I: Adding a new non-optional field

In this case, the old clients will ignore the value of the new field, so everything will be safe in terms of backward compatibility.

* Before:

```scala
case class Response(a: String, b: Int)
```

* After:

```scala
case class NewResponse(a: String, b: Int, c: Boolean)
```

#### J: Adding a new optional field

This would be just a particular case of the previous scenario, where the default value would be `None`, in the case of `Scala`.

#### K: Adding a new item to a union

In this scenario, the old clients will fail when the result is including the new item. Hence, the solution would be to provide a default value to the old coproduct and creating a new field with the new coproduct.

* Before:

```scala
case class Response(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewResponse(
      a: Int :+: String :+: CNil = Coproduct[Int :+: String :+: CNil](0),
      b: Int :+: String :+: Boolean :+: CNil)
```

#### L: Removing item from a union

No action will be required in this case.

* Before:

```scala
case class Response(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewResponse(a: Int :+: CNil)
```

#### M: Replacing item from a union

As long as the value of the coproduct belongs to the previous version, the old client should be able to accept the response as valid. Thus, we would need to follow the same approach as above when _Adding a new item to a coproduct_.

* Before:

```scala
case class Response(a: Int :+: String :+: CNil)
```

* After:

```scala
case class NewResponse(a: Int :+: Boolean :+: CNil)
```

#### N: Changing the type of an existing field

It will require providing a default value for the previous type, and then, we would need to create a new field with the new type.

* Before:

```scala
case class Response(a: String, b: Int)
```

* After:

```scala
case class NewResponse(a: String, b: Int = 123, c: Boolean)
```

#### O: Renaming a field

It's also safe in Avro, the server responses don't include the parameter names inside and they will be ignored when the data is serialized and sent through the wire.

* Before:

```scala
case class Response(a: String, b: Int)
```

* After:

```scala
case class NewResponse(a: String, c: Int)
```

#### P: Removing a field

This evolution should never happen since we would lose backward compatibility. Nonetheless, it would work only under the special case that the old response has a default value for the field that we want to delete, where this operation would be feasible by removing the field in the new version of the server response.

* Before:

```scala
case class Response(a: String, b: Int = 123)
```

* After:

```scala
case class NewResponse(a: String)
```

[mu]: https://github.com/higherkindness/mu