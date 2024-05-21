/*
 * Copyright 2018 Michael Stringer
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

package software.purpledragon.sttp.scribe

import com.github.scribejava.core.model.{OAuthRequest, Response}
import com.github.scribejava.core.oauth.OAuthService
import sttp.client.Identity

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

trait MonadAdaptor[F[_]] {
  def executeRequest(service: OAuthService, request: OAuthRequest): F[Response]
}

object MonadAdaptor {

  object Implicits {

    implicit val identity: MonadAdaptor[Identity] = new MonadAdaptor[Identity] {
      def executeRequest(service: OAuthService, request: OAuthRequest): Response = {
        service.execute(request)
      }
    }

    implicit val future: MonadAdaptor[Future] = new MonadAdaptor[Future] {
      def executeRequest(service: OAuthService, request: OAuthRequest): Future[Response] = {
        val javaFuture = service.executeAsync(request)
        CompletableFuture
          .supplyAsync(new Supplier[Response] {
            def get(): Response = javaFuture.get()
          })
          .toScala
      }
    }
  }
}
