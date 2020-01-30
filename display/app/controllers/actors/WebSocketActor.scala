package controllers.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import controllers.actors.ClientRequestHandlerActor.RegisterWebSocket
import org.sag.{BuildingMapConfiguration, DisplayFinalPositions, DisplayState}
import play.api.libs.json._

case class Send(data: String)

object WebSocketActor {
  def props(channel: ActorRef, clientRequestHandlerActor: ActorRef): Props = Props(new WebSocketActor(channel, clientRequestHandlerActor))
}

class WebSocketActor(channel: ActorRef, clientRequestHandlerActor: ActorRef) extends Actor with ActorLogging {

  clientRequestHandlerActor ! RegisterWebSocket(self)

  implicit object DisplayStateWriter extends Writes[DisplayState] with DefaultWrites {
    def writes(displayState: DisplayState) = Json.obj(
      "state" -> Json.arr(displayState.state.foldLeft(List.empty[Json.JsValueWrapper])((arr, state) => Json.obj(
        "name" -> Json.toJson(state._1),
        "x" -> Json.toJson(state._2.head._1.intValue()),
        "y" -> Json.toJson(state._2.head._2.intValue())
      ) :: arr): _*))
  }

  implicit object BuildingMapConfigurationWriter extends Writes[BuildingMapConfiguration] with DefaultWrites {
    def writes(mapConfig: BuildingMapConfiguration) = Json.obj(
      "config" -> mapConfig.map,
      "x" -> Json.toJson(mapConfig.map(0).length),
      "y" -> Json.toJson(mapConfig.map.length)
      )
  }

  implicit object ClosePositionsWriter extends Writes[DisplayFinalPositions] with DefaultWrites {
    def writes(displayFinalPositions: DisplayFinalPositions) = Json.obj(
      "final" -> Json.arr(displayFinalPositions.displayState.state.foldLeft(List.empty[Json.JsValueWrapper])((arr, state) => Json.obj(
        "name" -> Json.toJson(state._1),
        "x" -> Json.toJson(state._2.head._1.intValue()),
        "y" -> Json.toJson(state._2.head._2.intValue())
      ) :: arr): _*))
  }

  implicit object CollusionWriter extends Writes[Collision] with DefaultWrites {
    def writes(collusion: Collision) = Json.obj(
      "collusions" -> Json.arr(collusion.collision.foldLeft(List.empty[Json.JsValueWrapper])((arr, position) => Json.obj(
        "x" -> Json.toJson(position._1.intValue()),
        "y" -> Json.toJson(position._2.intValue())
      ) :: arr): _*))
  }

  def receive = {
    case Send(data) =>
      log.info("Data to push {} ", data)
      channel ! data
    case position@DisplayState(_) =>
      log.info("State to push {} ", position)

      val positions = position.state.values.map(v => (v.head._1, v.head._2))
      val duplicatesItem = positions groupBy { x => x } filter { case (_, lst) => lst.size > 1 } keys

      if (duplicatesItem.nonEmpty) {
        channel ! Json.stringify(Json.toJson(Collision(duplicatesItem)))
      }

      channel ! Json.stringify(Json.toJson(position))
    case config@BuildingMapConfiguration(_) =>
      log.info("Config to push {} ", config)
      channel ! Json.stringify(Json.toJson(config))
    case position@DisplayFinalPositions(_) =>
      log.info("Close positions to push {} ", position)
      channel ! Json.stringify(Json.toJson(position))
  }

  case class Collision(collision: Iterable[(Int, Int)])

}
