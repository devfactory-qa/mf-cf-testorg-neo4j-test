/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.interpreted.commands.expressions

import org.neo4j.cypher.internal.runtime.ExecutionContext
import org.neo4j.cypher.internal.runtime.interpreted.ListSupport
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.cypher.internal.v4_0.util.symbols._
import org.neo4j.values.AnyValue

case class ReduceFunction(collection: Expression, id: String, expression: Expression, acc: String, init: Expression)
  extends NullInNullOutExpression(collection) with ListSupport {

  override def compute(value: AnyValue, m: ExecutionContext, state: QueryState): AnyValue = {
    val list = makeTraversable(value)
    val iterator = list.iterator()
    val initialAcc = init(m, state)

    m.set(acc, initialAcc)
    while(iterator.hasNext) {
      m.set(id, iterator.next())
      m.set(acc, expression(m, state))
    }
    val result = m.getByName(acc)

    // Clean up accumulator slots
    m.set(acc, null)
    m.set(id, null)

    result
  }

  def rewrite(f: (Expression) => Expression) =
    f(ReduceFunction(collection.rewrite(f), id, expression.rewrite(f), acc, init.rewrite(f)))

  def arguments: Seq[Expression] = Seq(collection, init)

  override def children = Seq(collection, expression, init)

  def variableDependencies(expectedType: CypherType) = AnyType

  def symbolTableDependencies = (collection.symbolTableDependencies ++ expression.symbolTableDependencies ++ init.symbolTableDependencies) - id - acc
}
