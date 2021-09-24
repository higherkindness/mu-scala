package higherkindness.mu.rpc.protocol

import cats.effect.{unsafe, IO, Resource}
import munit.Suite

trait RPCFixtures { self: Suite =>

  def buildResourceFixture[A](name: String, resource: Resource[IO, A])(implicit
      runtime: unsafe.IORuntime
  ): Fixture[A] = new Fixture[A](name) {
    private var client: Option[A]          = None
    private var shutdown: Option[IO[Unit]] = None
    def apply() = client.getOrElse(throw new IllegalStateException("Not initialized"))
    override def beforeAll(): Unit = {
      val tuple = resource.allocated.unsafeRunSync()
      client = Some(tuple._1)
      shutdown = Some(tuple._2)
    }
    override def afterAll(): Unit =
      shutdown.foreach(_.unsafeRunSync())
  }

}
