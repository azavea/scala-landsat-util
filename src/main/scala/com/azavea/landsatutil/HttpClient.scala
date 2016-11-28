package com.azavea.landsatutil

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object HttpClient {
  type SprayJsonReader[R] = Unmarshaller[ResponseEntity, R]

  def get[T: SprayJsonReader](url: String)(implicit system: ActorSystem, materializer: ActorMaterializer): Future[T] = {
    import system.dispatcher
    Http()
      .singleRequest(HttpRequest(uri = Uri(url)))
      .flatMap(resp ⇒ Unmarshal(resp.entity).to[T])
  }
}
