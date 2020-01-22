package org.sag

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.sag.actors.{BuildingGeometry, BuildingMapConfigurationParser, BuildingMapGeometry, Display, Occupant}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  var config = BuildingMapConfigurationParser.parseFile("building.txt")
  var mapGeometry = new BuildingMapGeometry(config);
  val system = ActorSystem("emergency", ConfigFactory.load.getConfig("simulation"))
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)
  var display = system.actorOf(Display.props(config), "display")

  system.actorOf(Props(new BuildingGeometry(display, 30)), "platform")
  for (i <- 1 to 30) {
    system.actorOf(Props(new Occupant(mapGeometry.getRandomPoint(), mapGeometry.getRandomPoint(), 10, mapGeometry)), s"Pedestrian_${i}")
  }

  Await.result(system.whenTerminated, 1 day)
}
