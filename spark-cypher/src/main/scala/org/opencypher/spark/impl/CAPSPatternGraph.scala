/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.spark.impl

import org.apache.spark.storage.StorageLevel
import org.opencypher.okapi.api.graph.PropertyGraph
import org.opencypher.okapi.api.schema.Schema
import org.opencypher.okapi.api.types.{CTNode, CTRelationship}
import org.opencypher.okapi.ir.api.expr._
import org.opencypher.okapi.relational.impl.table.{ColumnName, RecordHeader, SlotContent}
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.impl.CAPSConverters._
import org.opencypher.spark.impl.table.CAPSRecordHeader._

class CAPSPatternGraph(private[spark] val baseTable: CAPSRecords, val schema: VerifiedSchema)(implicit val session: CAPSSession)
    extends CAPSGraph {

  private val header = baseTable.header

  def show(): Unit = baseTable.data.show()

  override def cache(): CAPSPatternGraph = map(_.cache())

  override def persist(): CAPSPatternGraph = map(_.persist())

  override def persist(storageLevel: StorageLevel): CAPSPatternGraph = map(_.persist(storageLevel))

  override def unpersist(): CAPSPatternGraph = map(_.unpersist())

  override def unpersist(blocking: Boolean): CAPSPatternGraph = map(_.unpersist(blocking))

  private def map(f: CAPSRecords => CAPSRecords) =
    new CAPSPatternGraph(f(baseTable), schema)

  override def nodes(name: String, nodeCypherType: CTNode): CAPSRecords = {
    val targetNode = Var(name)(nodeCypherType)
    val nodeSchema = schema.forNodeScan(nodeCypherType.labels)
    val targetNodeHeader = RecordHeader.nodeFromSchema(targetNode, nodeSchema)
    val extractionNodes: Seq[Var] = header.nodesForType(nodeCypherType)

    extractRecordsFor(targetNode, targetNodeHeader, extractionNodes)
  }

  override def relationships(name: String, relCypherType: CTRelationship): CAPSRecords = {
    val targetRel = Var(name)(relCypherType)
    val targetRelHeader = RecordHeader.relationshipFromSchema(targetRel, schema.forRelationship(relCypherType))
    val extractionRels = header.relationshipsForType(relCypherType)

    extractRecordsFor(targetRel, targetRelHeader, extractionRels)
  }

  private def extractRecordsFor(targetVar: Var, targetHeader: RecordHeader, extractionVars: Seq[Var]): CAPSRecords = {
    val extractionSlots = extractionVars.map { candidate =>
      candidate -> (header.childSlots(candidate) :+ header.slotFor(candidate))
    }.toMap

    val relColumnsLookupTables = extractionSlots.map {
      case (relVar, slotsForRel) =>
        relVar -> createScanToBaseTableLookup(targetVar, slotsForRel.map(_.content))
    }

    val extractedDf = baseTable
      .toDF()
      .flatMap(RowExpansion(targetHeader, targetVar, extractionSlots, relColumnsLookupTables))(targetHeader.rowEncoder)
    val distinctData = extractedDf.distinct()

    CAPSRecords.verifyAndCreate(targetHeader, distinctData)
  }

  private def createScanToBaseTableLookup(scanTableVar: Var, slotContents: Seq[SlotContent]): Map[String, String] = {
    slotContents.map { baseTableSlotContent =>
      ColumnName.of(baseTableSlotContent.withOwner(scanTableVar)) -> ColumnName.of(baseTableSlotContent)
    }.toMap
  }

  override def union(other: PropertyGraph): CAPSGraph = {
    CAPSUnionGraph(this, other.asCaps)
  }

}
