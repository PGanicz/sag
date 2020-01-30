package org.sag.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import org.sag.RemoteApi.Point
import org.sag.actors.Display.{RegisterPosition, SimulationEnd}
import org.sag.actors.Reaper.WatchMe
import org.sag.model.Protocol._
import org.sag.{ActorDead, RegisterDisplay}

import scala.collection.mutable.ListBuffer

/**
 * @author Piotr Ganicz
 */
class BuildingGeometry(display: ActorRef, numberOfPedestrians: Int) extends Actor with ActorLogging {
    val mediator = DistributedPubSub(context.system).mediator


  var pedestrianPositions = Map.empty[ActorRef, Point]
  var targetReached = 0
  var deadActors = 0
  var pedestrians = new ListBuffer[ActorRef]()

  mediator ! Subscribe("position", self)
  mediator ! Subscribe("registration", self)
  mediator ! DistributedPubSubMediator.Publish("reaper", WatchMe(self))

  def receive: Receive = {
    case SubscribeAck(Subscribe("position", None, _)) => log.info("Subscribed to position topic")
    case SubscribeAck(Subscribe("registration", None, _)) => log.info("Subscribed to registration topic")
    case ActorPosition(pedestrianPosition: Point, publisher: ActorRef) =>
      pedestrianPositions += (publisher -> pedestrianPosition)
      if (pedestrianPositions.values.count(_ == pedestrianPosition) > 1) {
        pedestrianPositions.foreach {
          case (pedestrian: ActorRef, currentPosition: Point)
            if (currentPosition == pedestrianPosition) =>
            log.info(s"${pedestrian.path.name} has collided at $currentPosition")
            pedestrian ! Collision
          case _ =>
        }
      } else {
        log.info(s"${publisher.path.name} position at $pedestrianPosition is safe")
        display ! RegisterPosition(publisher, pedestrianPosition)
        publisher ! MoveAccepted
      }
    case TargetReached(point) =>
      targetReached += 1
      pedestrianPositions = pedestrianPositions - sender()
      display ! RegisterPosition(sender(), point)
      showAfterSimulationStats()
    case ActorKilled(point) =>
      pedestrianPositions = pedestrianPositions - sender()
      deadActors += 1
      display ! ActorDead(sender(),point)
      showAfterSimulationStats()
    case RegisterPedestrian(pedestrian: ActorRef) =>
      pedestrian ! PedestrianRegistered
      pedestrians += pedestrian
      if (pedestrians.size == numberOfPedestrians) {
        pedestrians.foreach(entity => entity ! DeployPedestrian)
      }
  }

  def showAfterSimulationStats(): Unit = {
    if (targetReached + deadActors == pedestrians.size) {
      log.info(s"All targets reached. Deaths: ${deadActors} Reached: ${targetReached}")
      self ! PoisonPill
      display ! SimulationEnd
    }
  }
}
