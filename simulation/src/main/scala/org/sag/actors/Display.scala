package org.sag.actors

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.persistence.{PersistentActor, SnapshotOffer, SnapshotSelectionCriteria}
import org.sag.RemoteApi.Point
import org.sag.actors.Reaper.WatchMe
import org.sag.{ActorDead, BuildingMapConfiguration, DisplayFinalPositions, DisplayRegistered, DisplayState, FinalPositionsDisplayed, PositionChangedEvent, RegisterDisplay}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * @author Piotr Ganicz
 */
case object Exception

class Display(config: BuildingMapConfiguration) extends PersistentActor with ActorLogging {

  import org.sag.actors.Display._

  case object TakeSnapshot
  case object PrintMap

  case object FailureCheck
  implicit val executor = context.dispatcher

  val buildingConfig =  (new BuildingMapGeometry(BuildingMapConfigurationParser.parseFile("building.txt")))

  val mediator = DistributedPubSub(context.system).mediator

  var pedestrianPositions = DisplayState()

  var remoteDisplay: Option[ActorRef] = None

  var stop = false
  val snapShotInterval = 1000

  def scheduleSnapshot = true

  def initialDelayForSnapshot = 5 second

  def scheduledSnapshots = 5 second

  def initialDelayForFailureCheck =5 second

  def scheduledFailureCheck = 5 second

  mediator ! Subscribe("remoteDisplay", self)
  mediator ! DistributedPubSubMediator.Publish("reaper", WatchMe(self))

  override def preStart() {
    deleteSnapshots(SnapshotSelectionCriteria())
    deleteMessages(Long.MaxValue)

    context.system.scheduler.scheduleOnce(initialDelayForSnapshot, self, TakeSnapshot)
    context.system.scheduler.scheduleOnce(100 milliseconds, self, PrintMap)
    super.preStart()
  }

  def updateState(evt: PositionChangedEvent) = evt match {
    case event@PositionChangedEvent(pedestrian, pedestrianPosition) =>
      log.info(s"Register $pedestrianPosition to ${pedestrian}")
      pedestrianPositions = pedestrianPositions.update(event)
      remoteDisplay match {
        case Some(display) => display ! pedestrianPositions
        case None =>
      }
  }

  val receiveRecover: Receive = {
    case evt: PositionChangedEvent => log.warning(s"Recover event - $evt"); updateState(evt)
    case SnapshotOffer(_, snapshot: DisplayState) => log.warning(s"Recover snapshot - $snapshot"); pedestrianPositions = snapshot
    case _ =>
  }
  
  val receiveCommand: Receive = {
    case SubscribeAck(Subscribe("remoteDisplay", None, _)) =>
      log.info("Subscribed to remoteDisplay topic!")
    case RegisterDisplay(display) =>
      remoteDisplay = Some(display)
      log.info ("Remote display registration")
      display ! DisplayRegistered(config)
    case event@ActorDead(ref, point) => {
        pedestrianPositions = pedestrianPositions.update(event)
    }
    case RegisterPosition(pedestrian, pedestrianPosition) =>
      persist(PositionChangedEvent(pedestrian.path.name, pedestrianPosition))(updateState)
    case ShowPositions =>
      pedestrianPositions.state.foreach {
        case (pedestrian, position :: history) => log.info(s"Last known position of ${pedestrian} is $position. History[$history]")
        case _ =>
      }
      remoteDisplay match {
        case Some(display) =>
          log.info(s"sending final !!!!! ${display}")
          display ! DisplayFinalPositions(pedestrianPositions)
        case None =>
          sender ! PositionsDisplayed
      }
    case PrintMap =>
      buildingConfig.printMap(pedestrianPositions)
      if (!stop) {
        context.system.scheduler.scheduleOnce(100 milliseconds, self, PrintMap)
      }
    case SimulationEnd =>
      stop = true
      self ! PoisonPill
    case TakeSnapshot =>
      log.info("Taking snapshot...")
      saveSnapshot(pedestrianPositions)
      context.system.scheduler.scheduleOnce(initialDelayForSnapshot, self, TakeSnapshot)
    case Exception =>
      throw new Exception("Some really serious problem!");
  }

  override def postRestart(err: Throwable) = {
    log.info("I am back again...")
  }

  override def persistenceId: String = "displayEvents1"
}

object Display {

  def props(config: BuildingMapConfiguration): Props = Props(classOf[Display], config)

  case object ShowPositions

  case object PositionsDisplayed

  case class RegisterPosition(pedestrian: ActorRef, position: Point)

  case object SimulationEnd
}
