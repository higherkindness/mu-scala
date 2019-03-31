---
layout: docs
title: Schema Evolution - Avro
permalink: /schema-evolution/avro
---

# Avro - Schema Evolution

From now on, consider that we are using `AvroWithSchema` as serialization mechanism.

According to the [Avro Specs](http://avro.apache.org/docs/current/spec.html#Schema+Resolution):

> A reader of Avro data, whether from an RPC or a file, can always parse that data because its schema is provided. But that schema may not be exactly the schema that was expected. For example, if the data was written with a different version of the software than it is read, then records may have had fields added or removed.

In terms of Scala, this section specifies how such schema differences should be resolved preserving compatibility. We'll try to summarise a bit all the possible cases, but you could go deeper by using this [repo](https://github.com/higherkindness/mu-protocol-decimal-update) where you can play with all of the possibilities.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Cases

- [A: Add new non-optional field in the request](#a-add-new-non-optional-field-in-the-request)
- [B: Add new optional field in the request](#b-add-new-optional-field-in-the-request)
- [C: Add item to a coproduct in the request](#c-add-item-to-a-coproduct-in-the-request)
- [D: Remove item to a coproduct in the request](#d-remove-item-to-a-coproduct-in-the-request)
- [E: Replace item in a coproduct in the request](#e-replace-item-in-a-coproduct-in-the-request)
- [F: Change type of an existing field in the request](#f-change-type-of-an-existing-field-in-the-request)
- [G: Rename field in the request](#g-rename-field-in-the-request)
- [H: Remove field in the request](#h-remove-field-in-the-request)
- [I: Add new non-optional field in the response](#i-add-new-non-optional-field-in-the-response)
- [J: Add new optional field in the response](#j-add-new-optional-field-in-the-response)
- [K: Add item to a coproduct in the response](#k-add-item-to-a-coproduct-in-the-response)
- [L: Remove item to a coproduct in the response](#l-remove-item-to-a-coproduct-in-the-response)
- [M: Replace item in a coproduct in the response](#m-replace-item-in-a-coproduct-in-the-response)
- [N: Change type of an existing field in the response](#n-change-type-of-an-existing-field-in-the-response)
- [O: Rename field in the response](#o-rename-field-in-the-response)
- [P: Remove field in the response](#p-remove-field-in-the-response)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### A: Add new non-optional field in the request

Request: Provide default value to the new field.

```scala
case class Request(a: String, b: Int)
case class NewRequest(a: String, b: Int, c: Boolean = true)
```

### B: Add new optional field in the request

Request: This is just another case of the previous scenario. The solutions could be to provide a default value, that presumably could be None/null.

### C: Add item to a coproduct in the request

Request: No action required

```scala
case class Request(a: Int :+: String :+: CNil)
case class NewRequest(a: Int :+: String :+: Boolean :+: CNil)
```

### D: Remove item to a coproduct in the request

Request: The only way to deal with this situation is considering this as a change of type, see next case.

```scala
case class Request(a: Int :+: String :+: CNil)
case class NewRequest(b: String :+: CNil = Coproduct[String :+: CNil](""))
```

### E: Replace item in a coproduct in the request

Request: If the type is just replaced, it will work while the passed value is one of the types in common between the previous coproduct and the new one.

```scala
case class Request(a: Int :+: String :+: CNil)
case class NewRequest(a: Int :+: Boolean :+: CNil)
```

It will work if the request is:
```scala
Request(a = Coproduct[Int :+: String :+: CNil](10))
```

and it will fail if the request is:
```scala
Request(a = Coproduct[Int :+: String :+: CNil]("Hi"))
```

### F: Change type of an existing field in the request

Request: It's is impossible to deal with a type swapping if we maintain the same name of the field. So the solution is considering this replacement as a combination of removing the old field/type, and adding a new field with the new type with a default value.

```scala
case class Request(a: String, b: Int)
case class NewRequest(a: String, c: Boolean = true)
```

### G: Rename field in the request

Request: Renaming a field is a particular case of creating a field with the new name and default value, and remove the old one, that will be ignored if old clients pass it.

```scala
case class Request(a: String, b: Int)
case class NewRequest(a: String, c: Int = 0)
```

### H: Remove field in the request

Request: No action required. But note that the value will be ignored when old clients include it in the request.

```scala
case class Request(a: String, b: Int)
case class NewRequest(a: String)
```

### I: Add new non-optional field in the response

Response: No action required. Old clients will ignore the value of the new field.

```scala
case class Response(a: String, b: Int)
case class NewResponse(a: String, b: Int, c: Boolean)
```

### J: Add new optional field in the response

Response: This is a just a particular case of the previous scenario.

### K: Add item to a coproduct in the response

Response: Obviously, it fails when the value is the new item. So the solution is providing a default value to the old coproduct and creating a new field with the new coproduct.

```scala
case class Response(a: Int :+: String :+: CNil)
case class NewResponse(
      a: Int :+: String :+: CNil = Coproduct[Int :+: String :+: CNil](0),
      b: Int :+: String :+: Boolean :+: CNil)
```

### L: Remove item to a coproduct in the response

Response: No action required.

```scala
case class Response(a: Int :+: String :+: CNil)
case class NewResponse(a: Int :+: CNil)
```

### M: Replace item in a coproduct in the response

Response: As long as the value of the coproduct belongs to the previous version, the old client should be able to accept the response as valid.

```scala
case class Response(a: Int :+: String :+: CNil)
case class NewResponse(a: Int :+: Boolean :+: CNil)
```

### N: Change type of an existing field in the response

Response: It requires to provide a default value to the previous type, and to create a new field with the new type.

```scala
case class Response(a: String, b: Int)
case class NewResponse(a: String, b: Int = 123, c: Boolean)
```

### O: Rename field in the response

Response: It requires to remain the previous name/type with a default value and to add a new field with the same type.

```scala
case class Response(a: String, b: Int)
case class NewResponse(a: String, b: Int = 123, c: Int)
```

### P: Remove field in the response

Response: This evolution should never happen. Only under the special case that the old response has a default value for the field we want to drop, then this operation is feasible, just removing the field in the new version of the response.

```scala
case class Response(a: String, b: Int = 123)
case class NewResponse(a: String)
```
