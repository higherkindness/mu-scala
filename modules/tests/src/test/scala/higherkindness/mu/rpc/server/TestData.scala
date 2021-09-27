package higherkindness.mu.rpc.server

import higherkindness.mu.rpc.common.SC

import java.io.File
import java.util.concurrent.{Executor, TimeUnit}
import higherkindness.mu.rpc.server.netty._
import io.grpc._
import io.grpc.internal.GrpcUtil
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.{GrpcSslContexts, InternalProtocolNegotiators}
import io.grpc.util.MutableHandlerRegistry
import io.netty.channel.ChannelOption
import io.netty.channel.local.LocalServerChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.ssl.SslContext

import scala.jdk.CollectionConverters._
import java.util

object TestData {

  val timeout: Long                              = 1L
  val timeoutUnit: TimeUnit                      = TimeUnit.MINUTES
  val b: Boolean                                 = true
  val unit: Unit                                 = ()
  val sd1: ServerServiceDefinition               = ServerServiceDefinition.builder("s1").build()
  val sd2: ServerServiceDefinition               = ServerServiceDefinition.builder("s2").build()
  val serviceList: List[ServerServiceDefinition] = List(sd1, sd2)
  val immutableServiceList: List[ServerServiceDefinition] = List(sd1)
  val mutableServiceList: List[ServerServiceDefinition]   = List(sd2)

  val cert: File         = TestUtils.loadCert("server1.pem")
  val key: File          = TestUtils.loadCert("server1.key")
  val sslCtx: SslContext = GrpcSslContexts.forServer(cert, key).build()

  val grpcAllConfigList: List[GrpcConfig] = List(
    AddService(sd1),
    DirectExecutor,
    SetExecutor(new Executor() {
      override def execute(r: Runnable): Unit =
        throw new RuntimeException("Test executor")
    }),
    AddBindableService(new BindableService {
      override def bindService(): ServerServiceDefinition = sd1
    }),
    AddTransportFilter(new ServerTransportFilter() {
      val key1: Attributes.Key[String] = Attributes.Key.create("key1")
      val key2: Attributes.Key[String] = Attributes.Key.create("key2")
      override def transportReady(attrs: Attributes): Attributes =
        attrs.toBuilder.set(key1, "foo").set(key2, "bar").build()

      override def transportTerminated(attrs: Attributes): Unit = (): Unit
    }),
    AddStreamTracerFactory(new ServerStreamTracer.Factory() {
      override def newServerStreamTracer(fullMethodName: String, headers: Metadata) =
        throw new UnsupportedOperationException
    }),
    SetFallbackHandlerRegistry(new MutableHandlerRegistry),
    UseTransportSecurity(cert, key),
    SetDecompressorRegistry(DecompressorRegistry.getDefaultInstance),
    SetCompressorRegistry(CompressorRegistry.getDefaultInstance),
    // Netty configurations:
    ChannelType((new LocalServerChannel).getClass),
    WithChildOption[Boolean](ChannelOption.valueOf("ALLOCATOR"), true),
    BossEventLoopGroup(new NioEventLoopGroup(0)),
    WorkerEventLoopGroup(new NioEventLoopGroup(0)),
    SetSslContext(sslCtx),
    SetProtocolNegotiator(InternalProtocolNegotiators.tls(sslCtx)),
    MaxConcurrentCallsPerConnection(1000),
    FlowControlWindow(5),
    MaxMessageSize(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE),
    MaxHeaderListSize(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE),
    KeepAliveTime(GrpcUtil.DEFAULT_KEEPALIVE_TIMEOUT_NANOS, TimeUnit.NANOSECONDS),
    KeepAliveTimeout(GrpcUtil.DEFAULT_KEEPALIVE_TIMEOUT_NANOS, TimeUnit.NANOSECONDS),
    MaxConnectionIdle(1000, TimeUnit.MILLISECONDS),
    MaxConnectionAge(1000, TimeUnit.MILLISECONDS),
    MaxConnectionAgeGrace(1000, TimeUnit.MILLISECONDS),
    PermitKeepAliveTime(1000, TimeUnit.MILLISECONDS),
    PermitKeepAliveWithoutCalls(true)
  )

  val serverMock: Server = new Server {
    override def start(): Server                                          = this
    override def shutdown(): Server                                       = this
    override def shutdownNow(): Server                                    = this
    override def getPort: Int                                             = SC.port
    override def isShutdown: Boolean                                      = b
    override def isTerminated: Boolean                                    = b
    override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = b
    override def awaitTermination(): Unit                                 = {}
    override def getServices: util.List[ServerServiceDefinition]          = serviceList.asJava
    override def getImmutableServices: util.List[ServerServiceDefinition] =
      immutableServiceList.asJava
    override def getMutableServices: util.List[ServerServiceDefinition] = mutableServiceList.asJava
  }

}
