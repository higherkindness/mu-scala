/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc

import freestyle.rpc.idlgen.avro._
import freestyle.rpc.idlgen.proto.ProtoIdlGenerator
import freestyle.rpc.protocol.{Avro, AvroWithSchema, Protobuf, SerializationType}
import freestyle.rpc.internal.util.Toolbox.u._
import freestyle.rpc.internal.util.AstOptics.ast

package object idlgen {

  val DefaultRequestParamName = "arg"
  val EmptyType               = "Empty.type"

  val ScalaFileExtension = ".scala"

  val idlGenerators: Map[String, IdlGenerator] = Seq(ProtoIdlGenerator, AvroIdlGenerator)
    .map(g => g.idlType -> g)
    .toMap

  val srcGenerators: Map[String, SrcGenerator] = Seq(AvroSrcGenerator())
    .map(g => g.idlType -> g)
    .toMap

  val serializationTypes: Map[String, SerializationType] =
    Map("Protobuf" -> Protobuf, "Avro" -> Avro, "AvroWithSchema" -> AvroWithSchema)

  object BaseType {
    def unapply(tpe: Tree): Option[String] = tpe match {
      case ast._Ident(Ident(TypeName(name))) => Some(name)
      case _                                 => None
    }
  }

  object SingleAppliedTypeTree {
    def unapply(tpe: Tree): Option[(String, Tree)] = tpe match {
      case ast._AppliedTypeTree(AppliedTypeTree(ast._Ident(Ident(TypeName(ctor))), List(tree))) =>
        Some((ctor, tree))
      case _ => None
    }
  }

  object SingletonType {
    def unapply(tpe: Tree): Option[String] = tpe match {
      case ast._SingletonTypeTree(SingletonTypeTree(ast._Ident(Ident(TermName(t))))) => Some(t)
      case _                                                                         => None
    }
  }
}
