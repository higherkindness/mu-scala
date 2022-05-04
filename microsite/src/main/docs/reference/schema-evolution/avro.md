---
layout: docs
title: Schema Evolution - Avro
section: reference
permalink: /reference/schema-evolution/avro
---

# Avro - Schema Evolution

From now on, consider that we are using `AvroWithSchema` as the serialization
mechanism in your [Mu] program.

According to the [Avro
Specs](http://avro.apache.org/docs/current/spec.html#Schema+Resolution):

> A reader of Avro data, whether from an RPC or a file, can always parse that data because its schema is provided.
> But that schema may not be exactly the schema that was expected. For example, if the data was written with a different
> version of the software than it is read, then records may have had fields added or removed.

This page explains how such schema differences should be resolved to preserve
compatibility.

We'll try to summarise all the possible cases in both ends: request and
response.

However, you could go deeper by using this
[repo](https://github.com/higherkindness/mu-protocol-decimal-update) where you
can play with all of the possibilities.

All the cases below assume we are changing the schema used on the *server side*
first, so there are clients using the previous version of the schema while the
server uses the new schema.

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

### Modifying the Request

#### A: Adding a new non-optional field

You need to specify a default value for the new field, because clients using the
old schema will not include the new field in their requests.

* Before:

```
record Request {
  string a;
  int b;
}
```

* After:

```
record Request {
  string a;
  int b;
  boolean c = true;
}
```

#### B: Adding a new optional field

This is a special case of the previous scenario. `null` should be used as the
default value.

* Before:

```
record Request {
  string a;
  int b;
}
```

* After:

```
record Request {
  string a;
  int b;
  union {null, boolean} c = null;
}
```

#### C: Adding new item to a union

This change is safe. Older clients will only send an `int` or a `string`, while
newer clients might send a `boolean`.

* Before:

```
record Request {
  union {int, string} a;
}
```

* After:

```
record Request {
  union {int, string, boolean} a;
}
```

#### D: Removing item from a union

This change is NOT safe: older clients might send a `string`, and the server
will not know how to handle it.

The only safe choice is to add a new field with a default value.

Older clients will send only the old field, while newer clients should send the
same value for both the old and new fields. The server side will need to
implement logic to handle this situation.

* Before:

```
record Request {
  union {int, string, boolean} a;
}
```

* After:

```scala
record Request {
  union {int, string, boolean} a;
  union {int, boolean} b = 0;
}
```

#### E: Replacing item in a union

This is also an incompatible change. If the type is replaced, it will work while
the provided value is one of the types in common between the previous union and
the new one.

* Before:

```
record Request {
  union {int, string} a;
}
```

* After:

```
record Request {
  union {int, boolean} a;
}
```

It will work if the request is:

```scala
Request(a = 10)
```

And it will fail if the request is:

```scala
Request(a = "Hi")
```

Again, the safest strategy is to leave the existing field untouched and add a
new field.

#### F: Changing the type of an existing field

Changing the type of a field is an incompatible change, except in a few special
cases. So you need to add a new field with a default value.

Older clients will send only the old field, while newer clients will populate
both the old and new fields. The server side will need to implement logic to
handle this situation.

* Before:

```
record Request {
  string a;
  int b;
}
```

* After:

```
record Request {
  string a;
  int b;
  boolean c = true;
}
```

#### G: Renaming a field

This is NOT safe. Avro has no way of knowing that field `b` in the writer
(client's) schema and field `c` in the reader (server's) schema refer to the
same field. Schema resolution on the server side will ignore the unkown field
`b` and throw an error because the field `c` is not present.

* Before:

```
record Request {
  string a;
  int b;
}
```

* After:

```
record Request {
  string a;
  int c;
}
```

#### H: Removing a field

No action required. However, keep in mind that the value will be ignored when
old clients include it in the request.

* Before:

```
record Request {
  string a;
  int b;
}
```

* After:

```
record Request {
  string a;
}
```

### Modifying the Response

#### I: Adding a new non-optional field

In this case, the old clients will ignore the value of the new field, so
this is a compatible change.

* Before:

```
record Response {
  string a;
  int b;
}
```

* After:

```
record Response {
  string a;
  int b;
  boolean c;
}
```

#### J: Adding a new optional field

This is just a special case of the previous scenario.

#### K: Adding a new item to a union

In this scenario, older clients will fail when the server sets the field to a
value with the new type.

The safest solution would be to add a default value to the existing field and
add a new field.

* Before:

```
record Response {
  union {int, string} a;
}
```

* After:

```
record Response {
  union {int, string} a = 0;
  union {int, string, boolean} b;
}
```

#### L: Removing item from a union

No action will be required in this case.

* Before:

```
record Response {
  union {int, string, boolean} a;
}
```

* After:

```
record Response {
  union {int, string} a;
}
```

#### M: Replacing item from a union

As long as the value of the union field belongs to one of the previous version's
types, older clients should be able to accept the response as valid. Thus, we
would need to follow the same approach as above when _Adding a new item to a
union_.

* Before:

```
record Response {
  union {int, string} a;
}
```

* After:

```
record Response {
  union {int, boolean} a;
}
```

In this example, as long as the server only sends `int` values, we're OK.

#### N: Changing the type of an existing field

We should add a default value to the existing field and add a new field with the
new type.

* Before:

```
record Response {
  string a;
  int b;
}
```

* After:

```
record Response {
  string a;
  int b = 123;
  boolean c;
}
```

#### O: Renaming a field

This is NOT safe. Avro has no way of knowing that field `b` in the reader
(client's) schema and field `c` in the writer (server's) schema refer to the
same field. Schema resolution on the client side will ignore the unknown field
`c` and throw an error because the field `b` is not present.

* Before:

```
record Response {
  string a;
  int b;
}
```

* After:

```
record Response {
  string a;
  int c;
}
```

#### P: Removing a field

This evolution should never happen since we would lose backward compatibility.
Nonetheless, it would work only under the special case that the old response has
a default value for the field that we want to delete, where this operation would
be feasible by removing the field in the new version of the server response.

* Before:

```
record Response {
  string a;
  int b = 123;
}
```

* After:

```
record Response {
  string a;
}
```

[Mu]: https://github.com/higherkindness/mu-scala
