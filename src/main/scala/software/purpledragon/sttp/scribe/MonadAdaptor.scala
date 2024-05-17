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
