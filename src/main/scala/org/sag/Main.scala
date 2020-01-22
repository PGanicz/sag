package org.sag

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.sag.actors.{Pedestrian, Platform, Position}

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("emergency", ConfigFactory.load.getConfig("headquarter"))
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)
  var pedestrian = system.actorOf(Props(new Pedestrian(Position(1,1), Position(10,10), 1)), "Pedestrian_1")
  system.actorOf(Props[Platform], "1")

  Thread sleep (10 second).toMillis
  system.terminate()
}
