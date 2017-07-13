package org.opencypher.spark.api.spark

import org.opencypher.spark.SparkCypherTestSuite
import org.opencypher.spark.api.ir.global.TokenRegistry
import org.opencypher.spark.api.record._

class SparkCypherGraphTest extends SparkCypherTestSuite {

  implicit val space = SparkGraphSpace.empty(session, TokenRegistry.empty)

  val `:Person` =
    NodeScan.on("n" -> "ID") {
      _.build
       .withImpliedLabel("Person")
       .withOptionalLabel("Swedish" -> "IS_SWEDE")
       .withProperty("name" -> "NAME")
       .withProperty("lucky_number" -> "NUM")
    }
    .from(SparkCypherRecords.create(
      Seq("ID", "IS_SWEDE", "NAME", "NUM"),
      Seq(
        (1, true, "Mats", 23),
        (2, false, "Martin", 42),
        (3, false, "Max", 1337),
        (4, false, "Stefan", 9))
    ))

  val `:KNOWS` =
    RelationshipScan.on("r" -> "ID") {
      _.from("SRC").to("DST").relType("KNOWS")
       .build
       .withProperty("since" -> "SINCE")
    }
    .from(SparkCypherRecords.create(
      Seq("SRC", "ID", "DST", "SINCE"),
      Seq(
        (1, 1, 2, 2017),
        (1, 2, 3, 2016),
        (1, 3, 4, 2015),
        (2, 4, 3, 2016),
        (2, 5, 4, 2013),
        (3, 6, 4, 2016))
    ))

  test("Construct graph from scans") {
     val graph = SparkCypherGraph.create(`:Person`, `:KNOWS`)

     val nodes = graph.nodes("n")

     nodes shouldMatch `:Person`.records
  }
}