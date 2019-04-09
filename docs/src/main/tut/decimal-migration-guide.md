---
layout: docs
title: Migration guide for decimal types
permalink: /decimal-migration-guide
---

# Migration guide for decimal types (available from 0.15.1)

## Intended audience

This guide is only for projects that are using a version prior to `0.15.1` **and** have decimals in some of their protocols **and** want to serialize those decimals following the avro specs.

If you're starting a new project, you can use safely tagged `BigDecimal`s. Check the [Custom codecs section in Core concepts](core-concepts#custom-codecs) and [Plugin Settings section in IDL Generation](idl-generation#plugin-settings) for more information.

## Disclaimer

**IMPORTANT**: If you are using a version prior to `0.15.1` **and** do not want to serialize the decimals following the avro specs **and** you are using the plugin for generating the protocols, you need to set the `idlGenBigDecimal` setting key to `ScalaBigDecimalGen`.

## Background

The `0.13.5` version introduced custom encoders for `BigDecimal` values that serialize the values as byte arrays in a way not compliant with the avro specs.

Starting from `0.15.1`, there is a new way for serializing decimals following the avro specs. The idea is to convert the decimal to a scala `BigDecimal` with a `shapeless` tag indicating the precision and scale.

Let's see it with an example. Suppose the following service definition:

**AVDL**

`models.avdl`

```avroidl
@namespace("mu.rpc.protocols")

protocol StockInfoModels {

  record StockInfoRequest {
    string stockId;
  }

  record StockInfoResponse {
    string stockId;
    decimal(10,2) price;
    decimal(5,4) rate;
  }

}
```

`services.avdl`

```avroidl
@namespace("mu.rpc.protocols")

protocol StockInfoService {

  import idl "models.avdl";
  
  mu.rpc.protocols.StockInfoResponse getStockInfo(mu.rpc.protocols.StockInfoRequest request);

}
```

**Scala**
```scala
package higherkindness.mu.rpc.protocols

import higherkindness.mu.rpc.internal.encoders.avro.bigdecimal._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._

@message case class StockInfoRequest(stockId: String)

@message case class StockInfoResponse(stockId: String, price: BigDecimal, rate: BigDecimal)

@service(AvroWithSchema) trait StockInfoService[F[_]] {

  def getStockInfo(request: mu.rpc.protocols.StockInfoRequest): F[mu.rpc.protocols.StockInfoResponse]

}
```

With the *Scala* definition, you can serialize the `BigDecimal`s according to the avro specs (you can query the *scale* in the value) but you can't deserialize it, because the *Scala* **type** doesn't give us information about the *scale*.

Starting with `0.15.1` you can generate the following service in *Scala* (manually or through the `idlgen` plugin with the setting `idlGenBigDecimal := ScalaBigDecimalTaggedGen`)

```scala
package higherkindness.mu.rpc.protocols

import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._
import shapeless.{@@, Nat}

@message case class StockInfoRequest(stockId: String)

@message case class StockInfoResponse(stockId: String, price: BigDecimal @@ (Nat._10, Nat._2), rate: BigDecimal @@ (Nat._5, Nat._4))

@service(AvroWithSchema) trait StockInfoService[F[_]] {

  def getStockInfo(request: mu.rpc.protocols.StockInfoRequest): F[mu.rpc.protocols.StockInfoResponse]

}
```
 
As you can see, the type now reflects the precision and the scale of these `BigDecimal`s.
 
## How should you upgrade?
 
If you have services with `decimal`s, those `decimal`s are serialized in the old way, so upgrading your server or client will break the communication. The process is the following:

1. We'll call `v1` your current model/protocol version. 
2. Create a **new protocol version** duplicating the decimal fields (`v2`). Set the type of the new fields to tagged decimals (as shown above)
3. **Upgrade your server** to `v2` and emit the same value in each pair of duplicated fields. The old ones will be serialized in the old format, the new ones will be serialized with the new format.
4. Clients on `v1` will be reading old fields -> **We're good**
5. Create a **new protocol version**, removing the old decimal fields (`v3`)
6. **Upgrade your clients** to `v3`, they will be reading the new values -> **We're good**
7. As soon as all your clients are using `v3` (or a higher version) you can safely **upgrade your server** to `v3`

## Services defined in AVDL

When you configure your project to use a tagged type, you can't mix `BigDecimal` and tagged `BigDecimal` types. For that reason, the process is slightly different.

On step **2**, the types which included the old decimals need to be changed to a custom type. You then implement an encoder for that type that serializes the values in the same way decimals were serialized before.

Luckily, there are a couple of modules created for facilitating this task:

* `"io.higherkindness" %% "legacy-avro-decimal-compat-protocol" % "x.x.x"`

Provides an avdl file (`legacyAvroDecimalCompatProtocol.avdl`) with the custom type (`mu.rpc.protocols.LegacyAvroDecimalCompat`) to replace your old `decimal` values. That way, you could go from this:
                        
```avroidl 
@namespace("mu.rpc.protocols")

protocol StockInfoModels {
            
  record StockInfoRequest {
    string stockId;
  }

  record StockInfoResponse {
    string stockId;
    decimal(10,2) price;
    decimal(5,4) rate;
  }

}
```

To this:

```avroidl
@namespace("mu.rpc.protocols")

protocol StockInfoModel {

  import idl "legacyAvroDecimalCompatProtocol.avdl";

  record StockInfoRequest {
    string stockId;
  }

  record StockInfoResponse {
    string stockId;
    mu.rpc.protocols.LegacyAvroDecimalCompat price;
    mu.rpc.protocols.LegacyAvroDecimalCompat rate;
    decimal(10,2) stockPrice;
    decimal(5,4) stockRate;
  }

}
```

* `"io.higherkindness" %% "legacy-avro-decimal-compat-encoders" % "x.x.x"`

Provides the serializers for the custom type.

For your convenience, there is a repository that shows an example about how to do this process:
* [higherkindness/mu-protocol-decimal-update](https://github.com/higherkindness/mu-protocol-decimal-update)
