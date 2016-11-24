package com.azavea.landsatutil

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object HttpClient {
  type SprayJsonReader[R] = Unmarshaller[ResponseEntity, R]

  def get[T: SprayJsonReader](url: String)(implicit timeout: Duration): T = {
    val system = ActorSystem(s"url_request_${java.util.UUID.randomUUID}")

    try {
      get(url, system)
    } finally {
      system.terminate()
    }
  }

  def get[T: SprayJsonReader](url: String, system: ActorSystem)(implicit timeout: Duration): T = {
    implicit val s = system
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    val response = Http()
      .singleRequest(HttpRequest(uri = Uri(url)))
      .flatMap(resp ⇒ Unmarshal(resp.entity).to[T])

    Await.result(response, timeout)
  }
}
