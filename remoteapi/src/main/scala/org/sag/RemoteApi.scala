package org.sag

import akka.actor.ActorRef
import org.sag.RemoteApi.Point
import org.sag.MapElement.MapElement

/**
 * @author Piotr Ganicz
 */
object RemoteApi {
  type Point = (Int, Int)
}

case class ActorDead(occupant: ActorRef, position: Point)

case class DisplayState(state: Map[String, List[Point]] = Map.empty) {
  def update(event: PositionChangedEvent) = copy(state + (event.occupant -> (event.position :: state.getOrElse(event.occupant, List.empty))))

  def update(event: ActorDead) = copy(state.-(event.occupant.path.name))
}

case class DisplayFinalPositions(displayState: DisplayState)

case object FinalPositionsDisplayed

case class PositionChangedEvent(occupant: String, position: Point)

object MapElement extends Enumeration with Serializable {
  type MapElement = Value
  val Space, Wall, Exit = Value
}
case class BuildingMapConfiguration(map: Array[Array[MapElement]])

case class RegisterDisplay(display: ActorRef)

case class DisplayRegistered(config: BuildingMapConfiguration)
