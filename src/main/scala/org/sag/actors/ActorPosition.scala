package org.sag.actors

import akka.actor.ActorRef

/**
 * @author Piotr Ganicz
 */
case class ActorPosition(actualPosition: Position, self: ActorRef)
