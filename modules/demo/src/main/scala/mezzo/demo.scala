/* -
 * Mezzo [demo]
 */

package mezzodemo

import scala.Predef._

import mezzo.Hydrate
import mezzo.h2akka._

import cats._
import io.circe.generic.auto._

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try


// a simple demo API facade
case class CounterAPI(eval: CounterOp ~> Future) {
  import CounterOp._

  def adjust(delta: Long): Future[Long] = eval(Adjust(delta))
  def reset()            : Future[Unit] = eval(Reset)
  def read()             : Future[Long] = eval(Read)
}

// the actual algebra for our API (I'd actually call this the API)
sealed abstract class CounterOp[A] extends Product with Serializable
object CounterOp {
  case class Adjust(delta: Long) extends CounterOp[Long]
  case object Reset extends CounterOp[Unit]
  case object Read extends CounterOp[Long]
}

// an implementation of the API
class MutableCounterBackend extends (CounterOp ~> Future) {

  override def apply[A](rawOp: CounterOp[A]): Future[A] = rawOp match {
    case op: CounterOp.Adjust => handleAdjust(op)
    case CounterOp.Reset      => handleReset()
    case CounterOp.Read       => handleRead()
  }

  private[this] var count: Long = 0

  private[this] def handleAdjust(op: CounterOp.Adjust): Future[Long] =
    Future { count = count + op.delta; count }

  private[this] def handleReset(): Future[Unit] =
    Future { count = 0 }

  private[this] def handleRead(): Future[Long] =
    Future { count }

}

object DummyApp extends App {

  implicit val system = ActorSystem("testing")
  implicit val mat    = ActorMaterializer()

  // start an HTTP server using the mutable counter backend logic
  val backend = new MutableCounterBackend()
  val routes  = Hydrate[AkkaHttpRoutes].hydrate[CounterOp].apply(backend)
  val binding = Http().bindAndHandle(routes, "localhost", 8080)

  // wait for the server to start...
  Await.result(binding, 10.seconds)

  // create a HTTP client for our service
  val handler = AkkaClientRequestHandler(system, "http://localhost:8080/")
  val client  = Hydrate[AkkaHttpClient].hydrate[CounterOp].apply(handler)
  val counter = CounterAPI(client)

  // interact with our service over HTTP
  println("---> " + Try(Await.result(counter.read(),      10.seconds)))
  println("---> " + Try(Await.result(counter.adjust(100), 10.seconds)))
  println("---> " + Try(Await.result(counter.reset(),     10.seconds)))
  println("---> " + Try(Await.result(counter.adjust(200), 10.seconds)))
  println("---> " + Try(Await.result(counter.read(),      10.seconds)))
  println("---> " + Try(Await.result(counter.adjust(300), 10.seconds)))
  println("---> " + Try(Await.result(counter.read(),      10.seconds)))

  // shutdown the world
  system.terminate()

}
