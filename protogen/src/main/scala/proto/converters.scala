/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle
package rpc
package protocol

import freestyle.rpc.protocol.model.{ProtoFieldMod, _}

import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._
import scala.meta.contrib._

object converters {

  trait ScalaMetaSource2ProtoDefinitions {
    def convert(s: Source): ProtoDefinitions
  }

  class DefaultScalaMetaSource2ProtoDefinitions(
      implicit OC: ScalaMetaObject2ProtoOptions,
      MC: ScalaMetaClass2ProtoMessage,
      SC: ScalaMetaTrait2ProtoService)
      extends ScalaMetaSource2ProtoDefinitions {

    override def convert(s: Source): ProtoDefinitions = ProtoDefinitions(
      options = optionDirectives(s).toList.flatMap(OC.convert),
      messages = messageClasses(s).map(MC.convert).toList,
      services = serviceClasses(s).map(SC.convert).toList
    )

    private[this] def optionDirectives(source: Source): Seq[Object] = source.collect {
      case o: Object /*if o.hasMod(mod"@option")*/ => o
    }

    private[this] def messageClasses(source: Source): Seq[Class] = source.collect {
      case c: Class if c.hasMod(mod"@message") => c
    }

    private[this] def serviceClasses(source: Source): Seq[Trait] = source.collect {
      case t: Trait if t.hasMod(mod"@service") => t
    }
  }

  object ScalaMetaSource2ProtoDefinitions {
    implicit def defaultSourceToProtoDefinitions(
        implicit OC: ScalaMetaObject2ProtoOptions,
        MC: ScalaMetaClass2ProtoMessage,
        SC: ScalaMetaTrait2ProtoService): ScalaMetaSource2ProtoDefinitions =
      new DefaultScalaMetaSource2ProtoDefinitions
  }

  trait ScalaMetaClass2ProtoMessage {
    def convert(c: Class): ProtoMessage
  }

  object ScalaMetaClass2ProtoMessage {
    implicit def defaultClass2MessageConverter(implicit PC: ScalaMetaParam2ProtoMessageField) =
      new ScalaMetaClass2ProtoMessage {
        override def convert(c: Class): ProtoMessage = ProtoMessage(
          name = c.name.value,
          fields = c.ctor.paramss.flatten.zipWithIndex.map {
            case (p, t) => PC.convert(p, t + 1, None)
          }.toList
        )
      }
  }

  trait ScalaMetaParam2ProtoMessageField {
    def convert(p: Term.Param, tag: Int, mod: Option[ProtoFieldMod]): ProtoMessageField
  }

  object ScalaMetaParam2ProtoMessageField {
    implicit def defaultParam2ProtoMessageField: ScalaMetaParam2ProtoMessageField =
      new ScalaMetaParam2ProtoMessageField {
        override def convert(
            p: Term.Param,
            tag: Int,
            mod: Option[ProtoFieldMod] = None): ProtoMessageField = p match {
          case param"..$mods $paramname: Double = $expropt" =>
            ProtoDouble(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: Float = $expropt" =>
            ProtoFloat(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: Long = $expropt" =>
            ProtoInt64(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: Boolean = $expropt" =>
            ProtoBool(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: Int = $expropt" =>
            ProtoInt32(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: String = $expropt" =>
            ProtoString(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: Array[Byte] = $expropt" =>
            ProtoBytes(mod = mod, name = paramname.value, tag = tag)
          case param"..$mods $paramname: List[$tpe] = $expropt" =>
            convert(param"..$mods $paramname: $tpe = $expropt", tag, Some(Repeated))
          case param"..$mods $paramname: Option[$tpe] = $expropt" =>
            convert(param"..$mods $paramname: $tpe = $expropt", tag, None)
          case param"..$mods $paramname: $tpe = $expr" =>
            val ntpe = tpe match {
              case Some(tt) => tt.toString
              case _        => tpe.toString
            }
            ProtoCustomType(mod = mod, name = paramname.value, tag = tag, id = ntpe)
        }
      }
  }

  trait ScalaMetaTrait2ProtoService {
    def convert(t: Trait): ProtoService
  }

  object ScalaMetaTrait2ProtoService {
    implicit def defaultTrait2ServiceConverter =
      new ScalaMetaTrait2ProtoService {
        override def convert(t: Trait): ProtoService = {
          ProtoService(
            name = t.name.value,
            rpcs = t.collect {
              case q"@rpc @stream[ResponseStreaming.type] def $name[..$tparams]($request): FS[Observable[$response]]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamType(request),
                  response = response.toString(),
                  streamingType = Some(ResponseStreaming))
              case q"@rpc @stream[RequestStreaming.type] def $name[..$tparams]($param): FS[$response]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamStreamingType(param),
                  response = response.toString(),
                  streamingType = Some(RequestStreaming)
                )
              case q"@rpc @stream[BidirectionalStreaming.type] def $name[..$tparams]($param): FS[Observable[$response]]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamStreamingType(param),
                  response = response.toString(),
                  streamingType = Some(BidirectionalStreaming)
                )
              case q"@rpc def $name[..$tparams]($request): FS[$response]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamType(request),
                  response = response.toString(),
                  streamingType = None)
            }
          )
        }

        private[this] def extractParamType(param: Term.Param): String =
          param.decltpe match {
            case Some(retType) => retType.toString()
            case None =>
              throw new IllegalArgumentException(s"unexpected $param without return type")
          }

        private[this] def extractParamStreamingType(param: Term.Param): String =
          param.decltpe match {
            case Some(retType) =>
              retType match {
                case t"Observable[$request]" => request.toString()
                case _ =>
                  throw new IllegalArgumentException(
                    s"$param not enclosed in monix.reactive.Observable[_]")
              }
            case None =>
              throw new IllegalArgumentException(s"unexpected $param without return type")
          }

      }
  }

  trait ScalaMetaObject2ProtoOptions {
    def convert(o: Object): List[ProtoOption]
  }

  object ScalaMetaObject2ProtoOptions {
    implicit def defaultObject2Options: ScalaMetaObject2ProtoOptions =
      new ScalaMetaObject2ProtoOptions {
        override def convert(o: Object): List[ProtoOption] = {
          o.mods.collect {
            case Mod.Annot(
                Term.Apply(
                  Ctor.Ref.Name("option"),
                  Seq(
                    Term.Arg.Named(Term.Name("name"), Lit.String(name)),
                    Term.Arg.Named(Term.Name("value"), Lit.String(value)),
                    Term.Arg.Named(Term.Name("quote"), Lit.Boolean(quote))))) =>
              ProtoOption(name, value, quote)
          }.toList
        }
      }
  }

}
