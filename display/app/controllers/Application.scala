package controllers

import akka.actor.{ActorRef, ActorSystem, Address}
import akka.cluster.Cluster
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import controllers.actors.ClientRequestHandlerActor.RegisterWebSocket
import controllers.actors.{ClientRequestHandlerActor, WebSocketActor}
import javax.inject.{Inject, Singleton}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, WebSocket}

/**
 * @author Piotr Ganicz
 */
@Singleton
class Application @Inject()(cc: ControllerComponents)(implicit mat: Materializer) extends AbstractController(cc)  {

  val system = ActorSystem("emergency", ConfigFactory.load.getConfig("display"))
  val serverconfig = ConfigFactory.load.getConfig("simulation")
  val serverHost = serverconfig.getString("akka.remote.artery.canonical.hostname")
  val serverPort = serverconfig.getString("akka.remote.artery.canonical.port")
  val address = Address("akka.tcp", "emergency", serverHost, serverPort.toInt)
  var clientRequestHandler: ActorRef = _

  Cluster(system).join(address)
  clientRequestHandler = system.actorOf(ClientRequestHandlerActor.props, name = "ClientRequestHandlerActor")


  def index: Action[AnyContent] = Action {
    Ok(views.html.index("Emergency simulation"))
  }

  def startdisplay = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      WebSocketActor.props(out, clientRequestHandler)
    } (system , mat)
  }
}
