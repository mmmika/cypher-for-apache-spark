package org.opencypher.spark

import org.apache.spark.sql.{DataFrame, Dataset, Encoder}
import org.opencypher.spark.impl.StdRecord

trait CypherResult {
  def toDF: DataFrame
  def toDS[T : Encoder](f: CypherRecord => T): Dataset[T]
  def show(): Unit
}
