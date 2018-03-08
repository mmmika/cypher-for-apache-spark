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
package org.opencypher.spark.test.fixture

import org.opencypher.okapi.ir.test.support.creation.propertygraph.TestPropertyGraphFactory
import org.opencypher.okapi.test.BaseTestSuite
import org.opencypher.spark.impl.CAPSConverters._
import org.opencypher.spark.impl.CAPSGraph
import org.opencypher.spark.test.support.creation.caps.{CAPSScanGraphFactory, CAPSTestGraphFactory}

trait GraphCreationFixture {
  self: CAPSSessionFixture with BaseTestSuite =>

  def capsGraphFactory: CAPSTestGraphFactory = CAPSScanGraphFactory

  val initGraph: String => CAPSGraph =
    (createQuery) => CAPSScanGraphFactory(TestPropertyGraphFactory(createQuery)).asCaps
}
