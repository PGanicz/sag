package org.sag.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import org.sag.model.Protocol._

import scala.collection.mutable.ListBuffer

/**
 * @author Piotr Ganicz
 */
class Platform extends Actor with ActorLogging {
  val mediator = DistributedPubSub(context.system).mediator

  var pedestrianPositions = Map.empty[ActorRef, Position]
  var targetReached = 0
  var pedestrians = new ListBuffer[ActorRef]()
  var numberOfPedestrians = 1;

  mediator ! Subscribe("position", self)
  mediator ! Subscribe("registration", self)

  def receive: Receive = {
    case SubscribeAck(Subscribe("position", None, _)) => log.info("Subscribed to position topic")
    case SubscribeAck(Subscribe("registration", None, _)) => log.info("Subscribed to registration topic")
    case ActorPosition(pedestrianPosition, publisher) =>
      pedestrianPositions += (publisher -> pedestrianPosition)
      if (pedestrianPositions.values.count(_ == pedestrianPosition) > 1) {
        pedestrianPositions.foreach {
          case (pedestrian: ActorRef, currentPosition: Position)
            if (currentPosition == pedestrianPosition) =>
            log.info(s"${pedestrian.path.name} has collided at $currentPosition")
            pedestrian ! Collision
          case _ =>
        }
      } else {
        log.info(s"${publisher.path.name} position at $pedestrianPosition is safe")
        publisher ! MoveAccepted
      }
    case TargetReached =>
      targetReached += 1
      if (targetReached == pedestrians.size) {
        log.info("All targets reached")
        self ! PoisonPill
      }
    case ActorKilled =>

    case RegisterPedestrian(pedestrian: ActorRef) =>
      pedestrian ! PedestrianRegistered
      pedestrians += pedestrian
      if (pedestrians.size == numberOfPedestrians) {
        pedestrians.foreach(entity => entity ! DeployPedestrian)
      }
  }
}
