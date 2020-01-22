package org.sag.model

import akka.actor.ActorRef

/**
 * @author Piotr Ganicz
 */

object Protocol {
  final case class RegisterPedestrian(self: ActorRef)

  final case class DeployPedestrian()

  final case class EndOfMovement()

  final case class NextStep()

  final case class MoveAccepted()

  final case class PedestrianDeployed()

  final case class TargetReached()

  final case class PedestrianRegistered()

  final case class Collision()
  final case class Tick()
  final case class ActorKilled()

}
