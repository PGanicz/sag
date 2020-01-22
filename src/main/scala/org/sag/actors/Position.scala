package org.sag.actors

/**
 * @author Piotr Ganicz
 */
case class Position(var x: Integer,var y: Integer) {
  private def countMove(x: Int, y: Int): Int = {
    val diff = Math.abs(x - y)
    if (diff != 0) {
      return (x - y) / diff
    }
    0
  }

  def approxOneStepTo(target: Position): Position = {
    Position(x + countMove(target.x, x), y + countMove(target.y, y))
  }

  override def toString: String = {
    s"[$x, $y]"
  }
}
