package com.azavea.landsatutil

import spray.json._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._
import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

object HttpClient {
  def get[T: RootJsonReader](url: String)(implicit timeout: Duration): T = {
    val system = ActorSystem(s"url_request_${java.util.UUID.randomUUID}")
    try {
      get(url, system)
    } finally {
      system.terminate()
    }
  }

  def get[T: RootJsonReader](url: String, system: ActorSystem)(implicit timeout: Duration): T = {
    implicit val s = system
    import s.dispatcher
    implicit val t: Timeout = timeout.toMinutes minutes

    val pipeline = sendReceive ~> unmarshal[T]
    val response: Future[T] =
      pipeline(Get(url))

    Await.result(response, timeout)
  }
}
