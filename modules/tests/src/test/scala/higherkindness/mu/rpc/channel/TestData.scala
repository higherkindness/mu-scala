package higherkindness.mu.rpc.channel

import higherkindness.mu.rpc.channel.utils.StringMarshaller
import higherkindness.mu.rpc.common.SC
import higherkindness.mu.rpc.testing.interceptors.NoopInterceptor
import io.grpc.{ClientCall, CompressorRegistry, DecompressorRegistry, MethodDescriptor}
import io.grpc.internal.testing.TestUtils

import java.util.concurrent.{Executor, TimeUnit}

object TestData {

  type M = MethodDescriptor[String, String]
  type C = ClientCall[String, String]

  val methodDescriptor: M = MethodDescriptor
    .newBuilder(new StringMarshaller(), new StringMarshaller())
    .setType(MethodDescriptor.MethodType.UNARY)
    .setFullMethodName(MethodDescriptor.generateFullMethodName("foo.Bar", "Bar"))
    .build()

  val authority: String      = s"${SC.host}:${SC.port}"
  val foo                    = "Bar"
  val failureMessage: String = "❗"

  val managedChannelConfigAllList: List[ManagedChannelConfig] = List(
    DirectExecutor,
    SetExecutor(new Executor() {
      override def execute(r: Runnable): Unit =
        throw new RuntimeException("Test executor")
    }),
    AddInterceptorList(List(new NoopInterceptor())),
    AddInterceptor(new NoopInterceptor()),
    UserAgent("User-Agent"),
    OverrideAuthority(TestUtils.TEST_SERVER_HOST),
    UsePlaintext(),
    DefaultLoadBalancingPolicy("round_robin"),
    SetDecompressorRegistry(DecompressorRegistry.getDefaultInstance),
    SetCompressorRegistry(CompressorRegistry.getDefaultInstance),
    SetIdleTimeout(1, TimeUnit.MINUTES),
    SetMaxInboundMessageSize(4096000)
  )
}
