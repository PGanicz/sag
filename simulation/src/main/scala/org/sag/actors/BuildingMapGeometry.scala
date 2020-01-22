package org.sag.actors

import org.sag.RemoteApi.Point
import org.sag.astar.{AStar, Engine}
import org.sag.{BuildingMapConfiguration, MapElement}

import scala.util.Random

/**
 * @author Piotr Ganicz
 */
trait Command

case object Left extends Command
case object Right extends Command
case object Up extends Command
case object Down extends Command

class BuildingMapGeometry(config: BuildingMapConfiguration) {

  val engine = new Engine[Point, Command] {

    def valid(self: Point): Boolean = self match {
      case (x, y) =>
        x >= 0 && x < config.map(0).length &&
          y >= 0 && y < config.map.length &&
          config.map(y)(x) != MapElement.Wall
    }

    def bisimilar(self: Point, other: Point): Boolean =
      self == other

    def hash(self: Point): Int = self.hashCode

    def transition(state: Point, cmd: Command): Point = (state, cmd) match {
      case ((x, y), Left) => (x - 1, y)
      case ((x, y), Right) => (x + 1, y)
      case ((x, y), Up) => (x, y - 1)
      case ((x, y), Down) => (x, y + 1)
    }

    def commands = List(Left, Right, Up, Down)

    def distance(fst: Point, snd: Point): Double = manhattan(fst, snd)
  }

  def manhattan(from: Point, to: Point): Double = (from, to) match {
    case ((x1, y1), (x2, y2)) => math.abs(x1 - x2) + math.abs(y1 - y2)
  }

  def astar(state: Point, goal: Point): Option[List[Command]] = {
    AStar(state, goal, engine).computePath
  }

  def transition(state: Point, command: Command): Point = {
    engine.transition(state, command)
  }

  def valid(state: Point): Boolean = {
    engine.valid(state)
  }

  def getRandomPoint(): Point = {
    var point = (Random.nextInt(config.map(0).length), Random.nextInt(config.map.length))
    while (!valid(point)) {
      point = (Random.nextInt(config.map(0).length), Random.nextInt(config.map.length))
    }
    point
  }
  def getRandomSurrounding(point: Point): Point = {
    var newPoint = point match {case (x,y) => (x+Random.nextInt(2) - 1, y + Random.nextInt(2) - 1)}
    while (!valid(newPoint)) {
      newPoint = point match {case (x,y) => (x+Random.nextInt(2) - 1, y + Random.nextInt(2) - 1)}
    }
    newPoint
  }
}
