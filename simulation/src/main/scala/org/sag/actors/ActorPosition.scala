package org.sag.actors

import akka.actor.ActorRef
import org.sag.RemoteApi.Point
/**
 * @author Piotr Ganicz
 */
case class ActorPosition(actualPosition: Point, self: ActorRef)
