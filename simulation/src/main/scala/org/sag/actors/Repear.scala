package org.sag.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

import scala.collection.mutable.ArrayBuffer

/**
 * @author Piotr Ganicz
 */
object Reaper {
  val name = "reaper"
  case class WatchMe(ref: ActorRef)
}

class Reaper extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator

  import Reaper._

  val watched = ArrayBuffer.empty[ActorRef]
  mediator ! Subscribe("reaper", self)

  def allSoulsReaped() = {
    log.info("System termination")
    context.system.terminate()
  }

  final def receive = {
    case WatchMe(ref) =>
      context.watch(ref)
      watched += ref
    case Terminated(ref) =>
      watched -= ref
      if (watched.isEmpty) allSoulsReaped()
  }
}
