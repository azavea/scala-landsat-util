package com.azavea.landsatutil

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class HttpClient(apiEndpoint: Uri)(implicit val system: ActorSystem = ActorSystem(
  s"${HttpClient.AKKA_COOKIE}${java.util.UUID.randomUUID}")) {

  implicit private val materializer = ActorMaterializer()
  import system.dispatcher

  private val protocol = Http()
  private val host = {
    if(apiEndpoint.scheme == "https") {
      protocol.cachedHostConnectionPoolHttps[NotUsed](apiEndpoint.authority.host.address())
    }
    else {
      protocol.cachedHostConnectionPool[NotUsed](
        apiEndpoint.authority.host.address())
    }
  }

  type SprayJsonReader[R] = Unmarshaller[ResponseEntity, R]
  /** Submit query to configured API endpoint.*/
  def get[T: SprayJsonReader](query: Uri.Query): Future[T] = {
    val req = apiEndpoint.withQuery(query).toRelative
    system.log.debug(s"Request: $req")
    Source.single(HttpRequest(uri = req) -> NotUsed)
      .via(host)
      .runWith(Sink.head)
      .flatMap {
        case (Success(resp), _) ⇒ Unmarshal(resp.entity).to[T]
        case (Failure(e), _) ⇒ Future.failed(e)
      }
  }

  /** Release all resources associated with network communications. */
  def shutdown(): Unit = {
    protocol.shutdownAllConnectionPools() onComplete { _ ⇒
      val x = materializer.shutdown()
      // As a hack, using Akka system name to determine if we created it.
      if(system.name.startsWith(HttpClient.AKKA_COOKIE)) {
        // This produces a logged error that is not actually an error.
        // See https://github.com/akka/akka-http/issues/497,
        // TODO: Remove this comment when Akka fixes the message.
        system.terminate()
      }
    }
  }
}

object HttpClient {
  private val AKKA_COOKIE = "00http_query_"
}
