package higherkindness.mu.rpc.internal.service.makro

import scala.reflect.macros.blackbox.Context

class TreeHelpers[C <: Context](val c: C) {
  import c.universe._

  /*
   * When you write an anonymous parameter in an anonymous function that
   * ignores the parameter, e.g. `List(1, 2, 3).map(_ => "hello")` the
   * -Wunused:params scalac flag does not warn you about it.  That's
   *  because the compiler attaches a `NoWarnAttachment` to the tree for
   *  the parameter.
   *
   * But if you write the same thing in a quasiquote inside a macro, the
   * attachment does not get added, so you get false-positive compiler
   * warnings at the macro use site like: "parameter value x$2 in anonymous
   * function is never used".
   *
   * (The parameter needs a name, even though the function doesn't
   * reference it, so `_` gets turned into a fresh name e.g. `x$2`.  The
   * same thing happens even if you're not in a macro.)
   *
   * I'd say this is a bug in Scala. We work around it by manually adding
   * the attachment.
   */
  def anonymousParam: ValDef = {
    val tree: ValDef = q"{(_) => 1}".vparams.head
    c.universe.internal.updateAttachment(
      tree,
      c.universe.asInstanceOf[scala.reflect.internal.StdAttachments].NoWarnAttachment
    )
    tree
  }

  def lit(x: Any): Literal = Literal(Constant(x.toString))

}
