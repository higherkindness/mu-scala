### Mezzo
[![Build Status](https://api.travis-ci.org/andyscott/mezzo.png?branch=master)](https://travis-ci.org/andyscott/mezzo)

```tut:invisible
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

implicit val system = ActorSystem("testing")
implicit val mat    = ActorMaterializer()
```

### Introduction

Mezzo is a hydration framework for watering and growing your code from a
definition of your API.

What does this mean?

Consider a simple API for a counter:

```tut:silent
import scala.concurrent.Future

trait CounterAPI {
  def adjust(delta: Long): Future[Long]
  def reset()            : Future[Unit]
  def read()             : Future[Long]
}
```

This is a very traditional looking API. What we really want to
do is model our API as a series of case classes (an ADT). This will
allow to get a lot of work done for free.

```tut:silent
sealed abstract class CounterOp[A] extends Product with Serializable
object CounterOp {
  case class Adjust(delta: Long) extends CounterOp[Long]
  case object Reset extends CounterOp[Unit]
  case object Read extends CounterOp[Long]
}
```

If we want, we can implement the traditional interface by lifting our ADT
operations into our return type of Future. We can do this with a natural
transformation (`~>`).

```tut:silent
import cats.~>

case class CounterOps(eval: CounterOp ~> Future) extends CounterAPI {
  import CounterOp._

  def adjust(delta: Long): Future[Long] = eval(Adjust(delta))
  def reset()            : Future[Unit] = eval(Reset)
  def read()             : Future[Long] = eval(Read)
}
```

We can go ahead and implement our API using a natural transformation.
This is a lot like writing the body of an Actor, except the body is
strongly typed.

```tut:silent
class DummyCounter extends (CounterOp ~> Future) {

  override def apply[A](rawOp: CounterOp[A]): Future[A] = rawOp match {
    case op: CounterOp.Adjust => handleAdjust(op)
    case CounterOp.Reset      => handleReset()
    case CounterOp.Read       => handleRead()
  }

  @volatile private[this] var count: Long = 0

  private[this] def handleAdjust(op: CounterOp.Adjust): Future[Long] =
    Future { count = count + op.delta; count }
  private[this] def handleReset(): Future[Unit] =
    Future { count = 0 }
  private[this] def handleRead(): Future[Long] =
    Future { count }
}
```

We can now instantly hydrate a HTTP server and HTTP client for our API.

```tut:silent
import mezzo.Hydrate
import mezzo.h2akka._
import io.circe.generic.auto._
```

*server:*
```tut:silent
import akka.http.scaladsl.Http

val backend = new DummyCounter()
val routes  = Hydrate[AkkaHttpRoutes].hydrate[CounterOp].apply(backend)
val binding = Http().bindAndHandle(routes, "localhost", 8080)
```

*client:*
```tut:silent
import akka.http.scaladsl.Http

val handler = AkkaClientRequestHandler(system, "http://localhost:8080/")
val client  = Hydrate[AkkaHttpClient].hydrate[CounterOp].apply(handler)
val counter = CounterOps(client)
```

And the client works as expected:

```tut:book
Await.result(counter.read(),      10.seconds)
Await.result(counter.adjust(1),   10.seconds)
Await.result(counter.adjust(10),  10.seconds)
Await.result(counter.adjust(100), 10.seconds)
Await.result(counter.read(),      10.seconds)
Await.result(counter.reset(),     10.seconds)
Await.result(counter.read(),      10.seconds)
```

```tut:invisible
system.terminate()
```
### Documentation

Documetation coming soon.

### License
The license can be found in [COPYING].

[COPYING]: COPYING
