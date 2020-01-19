package org.sag

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * @author Piotr Ganicz
 */
object Main extends App {
  val config = ConfigFactory.load();
  val system = ActorSystem("emergency", config)

  system.terminate()
}
