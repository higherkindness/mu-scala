import freestyle.FreestylePlugin
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtprotoc.ProtocPlugin.autoImport.PB
import com.trueaccord.scalapb.compiler.{Version => cv}

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val demoCommonSettings = Seq(
      PB.protoSources.in(Compile) := Seq(sourceDirectory.in(Compile).value / "proto"),
      PB.targets.in(Compile) := Seq(scalapb.gen() -> sourceManaged.in(Compile).value),
      libraryDependencies ++= Seq(
        "io.grpc"                % "grpc-netty"            % cv.grpcJavaVersion,
        "com.trueaccord.scalapb" %% "scalapb-runtime"      % cv.scalapbVersion % "protobuf",
        "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % cv.scalapbVersion
      )
    )

    lazy val commandAliases: Seq[Def.Setting[_]] =
      addCommandAlias(
        "runServer",
        ";project demo-greeting;runMain freestyle.rpc.demo.greeting.GreetingServerApp") ++
        addCommandAlias(
          "runClient",
          ";project demo-greeting;runMain freestyle.rpc.demo.greeting.GreetingClientApp") ++
        addCommandAlias("validateHttpDemo", ";project demo-http;clean;compile;test")

    lazy val GOPATH = Option(sys.props("go.path")).getOrElse("/your/go/path")

  }

  override def projectSettings: Seq[Def.Setting[_]] = scalaMetaSettings

}
