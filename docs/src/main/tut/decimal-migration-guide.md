---
layout: docs
title: Migration guide for decimal types
permalink: /docs/rpc/decimal-migration-guide
---

# Migration guide for decimal types (available from 0.15.1)

## Background

The `0.13.5` version introduced custom encoders for `BigDecimal` values that serialize the values as byte arrays in a way not compliant with the avro specs.

Starting from `0.15.1`, there is a new way for serializing decimals following the avro specs. The idea is to convert the decimal to a scala `BigDecimal` with a `shapeless` tag indicating the precision and scale.

Let's see it with an example. Suppose the following service definition:

**AVDL**
```avdl
@namespace("freestyle.rpc.protocols")

protocol StockInfoService {

  record StockInfoRequest {
    string stockId;
  }

  record StockInfoResponse {
    string stockId;
    decimal(10,2) price;
    decimal(5,4) rate;
  }
  
  freestyle.rpc.protocols.StockInfoResponse getStockInfo(freestyle.rpc.protocols.StockInfoRequest request);

}
```

**Scala**
```scala
package freestyle.rpc.protocols

import freestyle.rpc.internal.encoders.avro.bigdecimal._
import freestyle.rpc.internal.encoders.avro.javatime._
import freestyle.rpc.protocol._

@message case class StockInfoRequest(stockId: String)

@message case class StockInfoResponse(stockId: String, price: BigDecimal, rate: BigDecimal)

@service(AvroWithSchema) trait StockInfoService[F[_]] {

  def getStockInfo(request: freestyle.rpc.protocols.StockInfoRequest): F[freestyle.rpc.protocols.StockInfoResponse]

}
```

With the *Scala* definition, you could serialize the `BigDecimal`s accordingly to the avro specs (you can query the *scale* in the value) but you can't deserialize it, because the *Scala* **type** doesn't give us information about the *scale*

Starting from `0.15.1` you could generate the following service in *Scala* (manually or through the `idlgen` plugin with the setting `idlGenBigDecimal := ScalaBigDecimalTaggedGen`)

 ```scala
 package freestyle.rpc.protocols
 
 import freestyle.rpc.internal.encoders.avro.bigDecimalTagged._
 import freestyle.rpc.internal.encoders.avro.javatime._
 import freestyle.rpc.protocol._
 import shapeless.{@@, Nat}
 
 @message case class StockInfoRequest(stockId: String)
 
 @message case class StockInfoResponse(stockId: String, price: BigDecimal @@ (Nat._10, Nat._2), rate: BigDecimal @@ (Nat._5, Nat._4))
 
 @service(AvroWithSchema) trait StockInfoService[F[_]] {
 
   def getStockInfo(request: freestyle.rpc.protocols.StockInfoRequest): F[freestyle.rpc.protocols.StockInfoResponse]
 
 }
 ```
 
 As you can see, now we can know by type what is the precision and the scale of these `BigDecimal`
 
## How to upgrade?
 
 If you have services with `decimal`s, those `decimal`s are serialized in the old way, so upgrading your server or client will break the communications.
 
### Services defined in Scala
 
* Add the model dependency
* Add the encoders dependency
* Duplicate the `BigDecimal` fields and change them their name
* Change the type of the old ones to `freestyle.rpc.protocols.AvroBigDecimalCompat`
* Add the compat encoders import
 
### Services defined in AVDL

* Add the protocol dependency
* Add the encoders dependency
* Duplicate the `decimal` fields and change them their name
* Change the type of the old ones to `freestyle.rpc.protocols.AvroBigDecimalCompat`
* Add the compat encoders import