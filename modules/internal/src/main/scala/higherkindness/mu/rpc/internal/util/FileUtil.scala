/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc
package internal
package util

import java.io._

object FileUtil {

  implicit class FileOps(val file: File) extends AnyVal {

    def requireExisting: File = {
      require(file.exists, s"${file.getAbsolutePath} doesn't exist")
      file
    }

    def allFiles: Seq[File] = allMatching(_ => true)

    def allMatching(f: File => Boolean): Seq[File] =
      if (file.isDirectory) file.listFiles.flatMap(_.allMatching(f))
      else Seq(file).filter(f)

    def write(lines: Seq[String]): Unit = {
      val writer = new PrintWriter(file)
      try lines.foreach(writer.println(_))
      finally writer.close()
    }

    def write(line: String): Unit = write(Seq(line))

  }

}
