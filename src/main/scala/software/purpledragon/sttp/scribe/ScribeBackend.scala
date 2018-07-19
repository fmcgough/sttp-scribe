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

import java.io.{FileOutputStream, IOException, InputStream, UnsupportedEncodingException}
import java.util.zip.{GZIPInputStream, InflaterInputStream}

import com.github.scribejava.core.model.{OAuthRequest, Verb, Response => ScribeResponse}
import com.github.scribejava.core.oauth.OAuthService
import com.softwaremill.sttp._
import com.softwaremill.sttp.internal.SttpFile

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.io.Source
import scala.language.higherKinds

abstract class ScribeBackend(service: OAuthService) extends SttpBackend[Id, Nothing] {
  override def send[T](request: Request[T, Nothing]): Id[Response[T]] = {
    val oAuthRequest = new OAuthRequest(method2Verb(request.method), request.uri.toString())

    request.headers foreach {
      case (name, value) => oAuthRequest.addHeader(name, value)
    }

    setRequestPayload(request.body, oAuthRequest)

    signRequest(oAuthRequest)

    val response = service.execute(oAuthRequest)
    handleResponse(response, request.response)
  }

  override def close(): Unit = Unit
  override def responseMonad: MonadError[Id] = IdMonad

  private def handleResponse[T](r: ScribeResponse, responseAs: ResponseAs[T, Nothing]): Response[T] = {
    val code = r.getCode

    // scribe includes the status line as a header with a key of 'null' :-(
    val headers = r.getHeaders.asScala
      .to[immutable.Seq]
      .filterNot(_._1 == null)

    val contentEncoding = Option(r.getHeader(HeaderNames.ContentEncoding))
    val charsetFromHeaders = Option(r.getHeader(HeaderNames.ContentType)).flatMap(encodingFromContentType)

    val is = wrapInput(r.getStream, contentEncoding)

    val body = if (StatusCodes.isSuccess(code)) {
      Right(readResponseBody(is, responseAs, charsetFromHeaders))
    } else {
      Left(readResponseBody(is, asByteArray, charsetFromHeaders))
    }

    Response(body, code, r.getMessage, headers, Nil)
  }

  private def readResponseBody[T](is: InputStream, responseAs: ResponseAs[T, Nothing], charset: Option[String]): T = {
    responseAs match {
      case MappedResponseAs(raw, g) =>
        g(readResponseBody(is, raw, charset))

      case IgnoreResponse =>
      // nothing to do?

      case ResponseAsString(encoding) =>
        Source.fromInputStream(is, charset.getOrElse(encoding)).mkString

      case ResponseAsByteArray =>
        toByteArray(is)

      case ResponseAsStream() =>
        // only possible when the user requests the response as a stream of
        // Nothing. Oh well ...
        throw new IllegalStateException()

      case ResponseAsFile(output, overwrite) =>
        val f = output.toFile

        if (f.exists() && !overwrite) {
          throw new IOException(s"File ${f.getAbsolutePath} exists - overwriting prohibited")
        } else {
          // ensure dir exists
          f.getParentFile.mkdirs()

          val os = new FileOutputStream(f)
          transfer(is, os)
          os.close()
          SttpFile.fromFile(f)
        }
    }
  }

  protected def signRequest(request: OAuthRequest): Unit

  private def encodingFromContentType(contentType: String): Option[String] = {
    contentType
      .split(";")
      .map(_.trim.toLowerCase)
      .collectFirst {
        case s if s.startsWith("charset=") => s.substring(8)
      }
  }

  private def setRequestPayload(body: RequestBody[_], request: OAuthRequest): Unit = {
    body match {
      case StringBody(content, encoding, contentType) =>
        request.setPayload(content)
        request.setCharset(encoding)
        contentType.foreach(request.addHeader("Content-Type", _))

      case ByteArrayBody(content, contentType) =>
        request.setPayload(content)
        contentType.foreach(request.addHeader("Content-Type", _))

      case ByteBufferBody(content, contentType) =>
        request.setPayload(content.array())
        contentType.foreach(request.addHeader("Content-Type", _))

      case FileBody(content, contentType) =>
        request.setPayload(content.toFile)
        contentType.foreach(request.addHeader("Content-Type", _))

      case InputStreamBody(_, _) =>
        ???
      case StreamBody(content) =>
        ???
      case MultipartBody(parts) =>
        ???
      case NoBody =>
      // nothing to set
    }
  }

  private def method2Verb(method: Method): Verb = {
    method match {
      case Method.GET => Verb.GET
      case Method.POST => Verb.POST
      case Method.PUT => Verb.PUT
      case Method.DELETE => Verb.DELETE
      case Method.OPTIONS => Verb.OPTIONS
      case Method.PATCH => Verb.PATCH
      case Method.TRACE => Verb.TRACE
      case m => throw new NotImplementedError(s"Scribe does not support $m")
    }
  }

  private def wrapInput(is: InputStream, encoding: Option[String]) = {
    encoding.map(_.toLowerCase) match {
      case None => is
      case Some("gzip") => new GZIPInputStream(is)
      case Some("deflate") => new InflaterInputStream(is)
      case Some(ce) => throw new UnsupportedEncodingException(s"Unsupported encoding: $ce")
    }
  }

}
