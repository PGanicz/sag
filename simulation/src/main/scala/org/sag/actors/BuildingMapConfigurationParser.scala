package org.sag.actors

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
        case 'E' => MapElement.Exit
      })
      .toArray
    source.close()
    BuildingMapConfiguration(data)
  }
}



