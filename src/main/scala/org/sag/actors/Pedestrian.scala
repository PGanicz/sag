package org.sag.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Scheduler}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import org.sag.model.Protocol._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class Pedestrian(var actualPosition: Position, targetPosition: Position, var health: Int) extends Actor with ActorLogging {
  import context.dispatcher
  var scheduler: Scheduler = context.system.scheduler
  val mediator: ActorRef = DistributedPubSub(context.system).mediator
  var pedestrianState: PedestrianState.Value = PedestrianState.CREATED

  context become registration
  scheduler.scheduleOnce(100 millisecond, self, Tick)

  def registration: Receive = {
    case Tick =>
      if (pedestrianState != PedestrianState.REGISTERED) {
        log.info("Publishing registration")
        mediator ! Publish("registration", RegisterPedestrian(self))
        scheduler.scheduleOnce(200 millisecond, self, Tick)
      }
    case PedestrianRegistered =>
      log.info("Successful registration!")
      context become receive
      pedestrianState = PedestrianState.REGISTERED
  }

  override def receive: Receive = {
    case DeployPedestrian =>
      log.info(s"Pedestrian is approaching to $actualPosition")
      publishPosition()
    case NextStep =>
      log.info(s"Pedestrian is moving to ${actualPosition.approxOneStepTo(targetPosition)} with target $targetPosition")
      scheduler.scheduleOnce(200 millisecond, self, EndOfMovement)
    case EndOfMovement =>
      actualPosition = actualPosition.approxOneStepTo(targetPosition)
      log.info(s"Pedestrian has arrived to $actualPosition")
      publishPosition()
    case Collision =>
      health -= 1
      if (health == 0) {
        log.info(s"Collision: pedestrian is dead")
        mediator ! Publish("position", ActorKilled)
        self ! PoisonPill
      } else {
        actualPosition = getRandomPosition(actualPosition)
        log.info(s"Collision: Pedestrian is moved to $actualPosition")
        publishPosition()
      }
    case MoveAccepted =>
      if (actualPosition == targetPosition) {
        log.info("Pedestrian reached the target")
        mediator ! Publish("position", TargetReached)
        self ! PoisonPill
        mediator ! Publish("position", ActorKilled)
      }
      self ! NextStep
  }

  private def getRandomPosition(position: Position): Position = {
    val x = Random.nextInt(2) - 1
    val y = Random.nextInt(2) - 1
    Position(position.x + x, position.y + y)
  }

  private def publishPosition(): Unit = mediator ! Publish("position", ActorPosition(actualPosition, self))
}

object PedestrianState extends Enumeration {
  type PedestrianState = Value
  val CREATED, REGISTERED = Value
}
