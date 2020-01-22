package controllers.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, Terminated}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import controllers.actors.ClientRequestHandlerActor.RegisterWebSocket
import org.sag.{BuildingMapConfiguration, DisplayFinalPositions, DisplayRegistered, DisplayState, FinalPositionsDisplayed, RegisterDisplay}

/**
 * @author Piotr Ganicz
 */
object ClientRequestHandlerActor {
  def props = Props(new ClientRequestHandlerActor())

  case class RegisterWebSocket(webSocketChannelActor: ActorRef)

}

class ClientRequestHandlerActor extends Actor with ActorLogging with Stash {

  import context.dispatcher

  import scala.concurrent.duration._

  val mediator = DistributedPubSub(context.system).mediator
  context.become(registration)
  scheduler.scheduleOnce(1 seconds, self, Tick)

  def scheduler = context.system.scheduler

  def registration: Receive = {
    case Tick =>
      println("Publishing display registration!")
      mediator ! Publish("remoteDisplay", RegisterDisplay(self))
      scheduler.scheduleOnce(1 seconds, self, Tick)
    case DisplayRegistered(config) =>
      log.info("Successful registration!")
      context.watch(sender)
      context become handleRequests(config)
      unstashAll()
    case RegisterWebSocket(webSocketChannel) => stash()
  }

  def handleRequests(config: BuildingMapConfiguration, websocketChannels: List[ActorRef] = List.empty): Receive = {
    case RegisterWebSocket(webSocketChannel) =>
      log.info("new context is {}", (webSocketChannel :: websocketChannels))
      context.become(handleRequests(config, webSocketChannel :: websocketChannels))
      webSocketChannel ! config
    case displayState@DisplayState(_) =>
      log.info("position has arrived {}", displayState)
      websocketChannels.foreach(_ ! displayState)
    case position@DisplayFinalPositions(_) =>
      log.info("final position has arrived {}", position)
      websocketChannels.foreach(_ ! position)
      sender ! FinalPositionsDisplayed
    case Terminated(server) =>
      context.become(registration)
      self ! Tick
  }

  case object Tick

  def receive: Receive = {
    case _ =>
  }
}
