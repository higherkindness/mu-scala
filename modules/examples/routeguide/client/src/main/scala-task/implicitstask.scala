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

package example.routeguide.client.task

import cats.effect.Resource
import example.routeguide.client.handlers.RouteGuideClientHandler
import example.routeguide.protocol.Protocols._
import example.routeguide.runtime._
import example.routeguide.client.runtime._
import monix.eval.Task

trait ClientTaskImplicits extends RouteGuide with ClientConf {

  implicit val routeGuideServiceClient: Resource[Task, RouteGuideService[Task]] =
    RouteGuideService.client[Task](channelFor)

  implicit val routeGuideClientHandler: RouteGuideClientHandler[Task] =
    new RouteGuideClientHandler[Task]
}

object implicits extends ClientTaskImplicits
