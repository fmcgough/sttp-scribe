package software.purpledragon.sttp.scribe

import com.github.scribejava.core.model.Response
import com.github.scribejava.core.oauth.OAuthService
import com.github.scribejava.core.model.OAuthRequest
import sttp.client.Identity
import scala.concurrent.Future
import scala.compat.java8.FutureConverters._
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

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
        CompletableFuture.supplyAsync(new Supplier[Response] {
          def get(): Response = javaFuture.get()
        }).toScala
      }
    }
  }
}
