package org.sag.actors

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.sag.{BuildingMapConfiguration, MapElement}

import scala.language.postfixOps

/**
 * @author Piotr Ganicz
 */
object BuildingMapConfigurationParser {

  def parseFile(fileName: String): BuildingMapConfiguration = {
    val source = io.Source.fromResource(fileName)
    val data = source.getLines()
      .map(line => line.toCharArray.map {
        case ' ' => MapElement.Space
        case '#' => MapElement.Wall
      })
      .toArray
    source.close()
    BuildingMapConfiguration(data)
  }
}



