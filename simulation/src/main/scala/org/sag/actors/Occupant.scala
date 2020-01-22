package org.sag.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Scheduler}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import org.sag.RemoteApi.Point
import org.sag.model.Protocol._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random


class Occupant(var actualPosition: Point, targetPosition: Point, var health: Int, mapGeometry: BuildingMapGeometry) extends Actor with ActorLogging {

  import context.dispatcher

  var scheduler: Scheduler = context.system.scheduler
  val mediator: ActorRef = DistributedPubSub(context.system).mediator
  var pedestrianState: PedestrianState.Value = PedestrianState.CREATED
  var actions: List[Command] = List()

  context become registration
  scheduler.scheduleOnce(100 millisecond, self, Tick)

  def registration: Receive = {
    case Tick =>
      if (pedestrianState != PedestrianState.REGISTERED) {
        log.info("Publishing registration")
        mediator ! Publish("registration", RegisterPedestrian(self))
        scheduler.scheduleOnce(1 second, self, Tick)
      }
    case PedestrianRegistered =>
      log.info("Successful registration!")
      context become receive
      pedestrianState = PedestrianState.REGISTERED
  }

  override def receive: Receive = {
    case DeployPedestrian =>
      log.info(s"Pedestrian is approaching to $actualPosition")
      calculateNewActionList()
      publishPosition()
    case NextStep =>
      if (actions.nonEmpty ) {
        var nextAction = actions.head
        actions = actions.drop(1)
        actualPosition = computeNextPosition(nextAction)
        log.info(s"Pedestrian is moving to ${actualPosition} with target $targetPosition")
        scheduler.scheduleOnce(200 millisecond, self, EndOfMovement)
      }
    case EndOfMovement =>
      log.info(s"Pedestrian has arrived to $actualPosition")
      publishPosition()
    case Collision =>
      health -= 1
      if (health == 0) {
        log.info(s"Collision: pedestrian is dead")
        mediator ! Publish("position", ActorKilled)
        self ! PoisonPill
      } else {
        actualPosition = mapGeometry.getRandomSurrounding(actualPosition)
        calculateNewActionList()
        log.info(s"Collision: Pedestrian is moved to $actualPosition")
        publishPosition()
      }
    case MoveAccepted =>
      if (actualPosition == targetPosition) {
        log.info("Pedestrian reached the target")
        mediator ! Publish("position", TargetReached)
        self ! PoisonPill
      }
      self ! NextStep
  }

  def calculateNewActionList(): Unit = {
    log.info(s"Calculating new from ${actualPosition} to ${targetPosition}")
    mapGeometry.astar(actualPosition, targetPosition) match {
      case Some(list) => actions = list
      case None => actions = List()
    }
  }

  def computeNextPosition(action: Command): Point = {
    mapGeometry.transition(actualPosition, action)
  }

  private def publishPosition(): Unit = mediator ! Publish("position", ActorPosition(actualPosition, self))
}

object PedestrianState extends Enumeration {
  type PedestrianState = Value
  val CREATED, REGISTERED = Value
}
