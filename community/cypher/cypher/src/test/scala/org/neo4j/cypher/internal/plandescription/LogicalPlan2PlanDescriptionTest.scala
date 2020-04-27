/*
 * Copyright (c) 2002-2020 "Neo4j,"
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
package org.neo4j.cypher.internal.plandescription

import org.neo4j.cypher.CypherVersion
import org.neo4j.cypher.QueryPlanTestSupport.StubExecutionPlan
import org.neo4j.cypher.internal.ast
import org.neo4j.cypher.internal.ast.AllResource
import org.neo4j.cypher.internal.ast.CreateNodeLabelAction
import org.neo4j.cypher.internal.ast.CreateUserAction
import org.neo4j.cypher.internal.ast.LabelsQualifier
import org.neo4j.cypher.internal.ast.ProcedureResultItem
import org.neo4j.cypher.internal.ast.PropertyResource
import org.neo4j.cypher.internal.ast.ShowRolePrivileges
import org.neo4j.cypher.internal.ast.ShowUserAction
import org.neo4j.cypher.internal.ast.UserAllQualifier
import org.neo4j.cypher.internal.ast.UserQualifier
import org.neo4j.cypher.internal.expressions.And
import org.neo4j.cypher.internal.expressions.CachedProperty
import org.neo4j.cypher.internal.expressions.Equals
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.expressions.FunctionName
import org.neo4j.cypher.internal.expressions.LabelName
import org.neo4j.cypher.internal.expressions.LabelToken
import org.neo4j.cypher.internal.expressions.LessThan
import org.neo4j.cypher.internal.expressions.ListLiteral
import org.neo4j.cypher.internal.expressions.MapExpression
import org.neo4j.cypher.internal.expressions.NODE_TYPE
import org.neo4j.cypher.internal.expressions.Namespace
import org.neo4j.cypher.internal.expressions.ProcedureName
import org.neo4j.cypher.internal.expressions.Property
import org.neo4j.cypher.internal.expressions.PropertyKeyName
import org.neo4j.cypher.internal.expressions.PropertyKeyToken
import org.neo4j.cypher.internal.expressions.RelTypeName
import org.neo4j.cypher.internal.expressions.SemanticDirection
import org.neo4j.cypher.internal.expressions.SemanticDirection.BOTH
import org.neo4j.cypher.internal.expressions.SemanticDirection.INCOMING
import org.neo4j.cypher.internal.expressions.SemanticDirection.OUTGOING
import org.neo4j.cypher.internal.expressions.SignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.StringLiteral
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.expressions.functions.Collect
import org.neo4j.cypher.internal.expressions.functions.Count
import org.neo4j.cypher.internal.expressions.functions.Point
import org.neo4j.cypher.internal.ir.CreateNode
import org.neo4j.cypher.internal.ir.CreateRelationship
import org.neo4j.cypher.internal.ir.NoHeaders
import org.neo4j.cypher.internal.ir.PatternRelationship
import org.neo4j.cypher.internal.ir.ShortestPathPattern
import org.neo4j.cypher.internal.ir.VarPatternLength
import org.neo4j.cypher.internal.ir.ordering.ProvidedOrder
import org.neo4j.cypher.internal.logical.plans
import org.neo4j.cypher.internal.logical.plans.Aggregation
import org.neo4j.cypher.internal.logical.plans.AllNodesScan
import org.neo4j.cypher.internal.logical.plans.AlterUser
import org.neo4j.cypher.internal.logical.plans.Anti
import org.neo4j.cypher.internal.logical.plans.AntiConditionalApply
import org.neo4j.cypher.internal.logical.plans.AntiSemiApply
import org.neo4j.cypher.internal.logical.plans.Apply
import org.neo4j.cypher.internal.logical.plans.Ascending
import org.neo4j.cypher.internal.logical.plans.AssertNotCurrentUser
import org.neo4j.cypher.internal.logical.plans.AssertSameNode
import org.neo4j.cypher.internal.logical.plans.CacheProperties
import org.neo4j.cypher.internal.logical.plans.CartesianProduct
import org.neo4j.cypher.internal.logical.plans.ConditionalApply
import org.neo4j.cypher.internal.logical.plans.CopyRolePrivileges
import org.neo4j.cypher.internal.logical.plans.Create
import org.neo4j.cypher.internal.logical.plans.CreateDatabase
import org.neo4j.cypher.internal.logical.plans.CreateIndex
import org.neo4j.cypher.internal.logical.plans.CreateNodeKeyConstraint
import org.neo4j.cypher.internal.logical.plans.CreateNodePropertyExistenceConstraint
import org.neo4j.cypher.internal.logical.plans.CreateRelationshipPropertyExistenceConstraint
import org.neo4j.cypher.internal.logical.plans.CreateRole
import org.neo4j.cypher.internal.logical.plans.CreateUniquePropertyConstraint
import org.neo4j.cypher.internal.logical.plans.CreateUser
import org.neo4j.cypher.internal.logical.plans.DeleteExpression
import org.neo4j.cypher.internal.logical.plans.DeleteNode
import org.neo4j.cypher.internal.logical.plans.DeletePath
import org.neo4j.cypher.internal.logical.plans.DeleteRelationship
import org.neo4j.cypher.internal.logical.plans.DenyDatabaseAction
import org.neo4j.cypher.internal.logical.plans.DenyDbmsAction
import org.neo4j.cypher.internal.logical.plans.DenyMatch
import org.neo4j.cypher.internal.logical.plans.DenyRead
import org.neo4j.cypher.internal.logical.plans.DenyTraverse
import org.neo4j.cypher.internal.logical.plans.Descending
import org.neo4j.cypher.internal.logical.plans.DetachDeleteExpression
import org.neo4j.cypher.internal.logical.plans.DetachDeleteNode
import org.neo4j.cypher.internal.logical.plans.DetachDeletePath
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipByIdSeek
import org.neo4j.cypher.internal.logical.plans.Distinct
import org.neo4j.cypher.internal.logical.plans.DoNotGetValue
import org.neo4j.cypher.internal.logical.plans.DoNotIncludeTies
import org.neo4j.cypher.internal.logical.plans.DoNothingIfExists
import org.neo4j.cypher.internal.logical.plans.DoNothingIfNotExists
import org.neo4j.cypher.internal.logical.plans.DropConstraintOnName
import org.neo4j.cypher.internal.logical.plans.DropDatabase
import org.neo4j.cypher.internal.logical.plans.DropIndex
import org.neo4j.cypher.internal.logical.plans.DropIndexOnName
import org.neo4j.cypher.internal.logical.plans.DropNodeKeyConstraint
import org.neo4j.cypher.internal.logical.plans.DropNodePropertyExistenceConstraint
import org.neo4j.cypher.internal.logical.plans.DropRelationshipPropertyExistenceConstraint
import org.neo4j.cypher.internal.logical.plans.DropResult
import org.neo4j.cypher.internal.logical.plans.DropRole
import org.neo4j.cypher.internal.logical.plans.DropUniquePropertyConstraint
import org.neo4j.cypher.internal.logical.plans.DropUser
import org.neo4j.cypher.internal.logical.plans.Eager
import org.neo4j.cypher.internal.logical.plans.EmptyResult
import org.neo4j.cypher.internal.logical.plans.EnsureValidNonSystemDatabase
import org.neo4j.cypher.internal.logical.plans.EnsureValidNumberOfDatabases
import org.neo4j.cypher.internal.logical.plans.ErrorPlan
import org.neo4j.cypher.internal.logical.plans.Expand
import org.neo4j.cypher.internal.logical.plans.ExpandAll
import org.neo4j.cypher.internal.logical.plans.ExpandInto
import org.neo4j.cypher.internal.logical.plans.FieldSignature
import org.neo4j.cypher.internal.logical.plans.FindShortestPaths
import org.neo4j.cypher.internal.logical.plans.ForeachApply
import org.neo4j.cypher.internal.logical.plans.GetValue
import org.neo4j.cypher.internal.logical.plans.GrantDatabaseAction
import org.neo4j.cypher.internal.logical.plans.GrantDbmsAction
import org.neo4j.cypher.internal.logical.plans.GrantMatch
import org.neo4j.cypher.internal.logical.plans.GrantRead
import org.neo4j.cypher.internal.logical.plans.GrantRoleToUser
import org.neo4j.cypher.internal.logical.plans.GrantTraverse
import org.neo4j.cypher.internal.logical.plans.IndexOrderNone
import org.neo4j.cypher.internal.logical.plans.IndexSeek
import org.neo4j.cypher.internal.logical.plans.IndexSeekLeafPlan
import org.neo4j.cypher.internal.logical.plans.IndexedProperty
import org.neo4j.cypher.internal.logical.plans.Input
import org.neo4j.cypher.internal.logical.plans.LeftOuterHashJoin
import org.neo4j.cypher.internal.logical.plans.LetAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSelectOrAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSelectOrSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSemiApply
import org.neo4j.cypher.internal.logical.plans.Limit
import org.neo4j.cypher.internal.logical.plans.LoadCSV
import org.neo4j.cypher.internal.logical.plans.LockNodes
import org.neo4j.cypher.internal.logical.plans.LogSystemCommand
import org.neo4j.cypher.internal.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.logical.plans.ManyQueryExpression
import org.neo4j.cypher.internal.logical.plans.ManySeekableArgs
import org.neo4j.cypher.internal.logical.plans.MergeCreateNode
import org.neo4j.cypher.internal.logical.plans.MergeCreateRelationship
import org.neo4j.cypher.internal.logical.plans.MultiNodeIndexSeek
import org.neo4j.cypher.internal.logical.plans.NodeByIdSeek
import org.neo4j.cypher.internal.logical.plans.NodeByLabelScan
import org.neo4j.cypher.internal.logical.plans.NodeCountFromCountStore
import org.neo4j.cypher.internal.logical.plans.NodeHashJoin
import org.neo4j.cypher.internal.logical.plans.NodeIndexSeek
import org.neo4j.cypher.internal.logical.plans.NodeUniqueIndexSeek
import org.neo4j.cypher.internal.logical.plans.Optional
import org.neo4j.cypher.internal.logical.plans.OptionalExpand
import org.neo4j.cypher.internal.logical.plans.OrderedAggregation
import org.neo4j.cypher.internal.logical.plans.OrderedDistinct
import org.neo4j.cypher.internal.logical.plans.PartialSort
import org.neo4j.cypher.internal.logical.plans.PartialTop
import org.neo4j.cypher.internal.logical.plans.PointDistanceRange
import org.neo4j.cypher.internal.logical.plans.PointDistanceSeekRangeWrapper
import org.neo4j.cypher.internal.logical.plans.ProcedureCall
import org.neo4j.cypher.internal.logical.plans.ProcedureReadOnlyAccess
import org.neo4j.cypher.internal.logical.plans.ProcedureSignature
import org.neo4j.cypher.internal.logical.plans.ProduceResult
import org.neo4j.cypher.internal.logical.plans.ProjectEndpoints
import org.neo4j.cypher.internal.logical.plans.Projection
import org.neo4j.cypher.internal.logical.plans.PruningVarExpand
import org.neo4j.cypher.internal.logical.plans.QualifiedName
import org.neo4j.cypher.internal.logical.plans.RangeQueryExpression
import org.neo4j.cypher.internal.logical.plans.RelationshipCountFromCountStore
import org.neo4j.cypher.internal.logical.plans.RemoveLabels
import org.neo4j.cypher.internal.logical.plans.RequireRole
import org.neo4j.cypher.internal.logical.plans.ResolvedCall
import org.neo4j.cypher.internal.logical.plans.RevokeDatabaseAction
import org.neo4j.cypher.internal.logical.plans.RevokeDbmsAction
import org.neo4j.cypher.internal.logical.plans.RevokeMatch
import org.neo4j.cypher.internal.logical.plans.RevokeRead
import org.neo4j.cypher.internal.logical.plans.RevokeRoleFromUser
import org.neo4j.cypher.internal.logical.plans.RevokeTraverse
import org.neo4j.cypher.internal.logical.plans.RightOuterHashJoin
import org.neo4j.cypher.internal.logical.plans.RollUpApply
import org.neo4j.cypher.internal.logical.plans.SelectOrAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.SelectOrSemiApply
import org.neo4j.cypher.internal.logical.plans.Selection
import org.neo4j.cypher.internal.logical.plans.SemiApply
import org.neo4j.cypher.internal.logical.plans.SetLabels
import org.neo4j.cypher.internal.logical.plans.SetNodePropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetNodeProperty
import org.neo4j.cypher.internal.logical.plans.SetPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetProperty
import org.neo4j.cypher.internal.logical.plans.SetRelationshipPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetRelationshipProperty
import org.neo4j.cypher.internal.logical.plans.ShowPrivileges
import org.neo4j.cypher.internal.logical.plans.ShowRoles
import org.neo4j.cypher.internal.logical.plans.ShowUsers
import org.neo4j.cypher.internal.logical.plans.SingleSeekableArg
import org.neo4j.cypher.internal.logical.plans.Skip
import org.neo4j.cypher.internal.logical.plans.Sort
import org.neo4j.cypher.internal.logical.plans.StartDatabase
import org.neo4j.cypher.internal.logical.plans.StopDatabase
import org.neo4j.cypher.internal.logical.plans.Top
import org.neo4j.cypher.internal.logical.plans.TriadicSelection
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipByIdSeek
import org.neo4j.cypher.internal.logical.plans.Union
import org.neo4j.cypher.internal.logical.plans.UnwindCollection
import org.neo4j.cypher.internal.logical.plans.ValueHashJoin
import org.neo4j.cypher.internal.logical.plans.VarExpand
import org.neo4j.cypher.internal.logical.plans.VariablePredicate
import org.neo4j.cypher.internal.plandescription.Arguments.Details
import org.neo4j.cypher.internal.plandescription.Arguments.EstimatedRows
import org.neo4j.cypher.internal.plandescription.Arguments.Order
import org.neo4j.cypher.internal.plandescription.Arguments.Planner
import org.neo4j.cypher.internal.plandescription.Arguments.PlannerImpl
import org.neo4j.cypher.internal.plandescription.Arguments.PlannerVersion
import org.neo4j.cypher.internal.plandescription.Arguments.RuntimeVersion
import org.neo4j.cypher.internal.plandescription.Arguments.Version
import org.neo4j.cypher.internal.planner.spi.IDPPlannerName
import org.neo4j.cypher.internal.planner.spi.PlanningAttributes.Cardinalities
import org.neo4j.cypher.internal.planner.spi.PlanningAttributes.ProvidedOrders
import org.neo4j.cypher.internal.util.Cardinality
import org.neo4j.cypher.internal.util.DummyPosition
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.LabelId
import org.neo4j.cypher.internal.util.PropertyKeyId
import org.neo4j.cypher.internal.util.attribution.Id
import org.neo4j.cypher.internal.util.attribution.IdGen
import org.neo4j.cypher.internal.util.attribution.SequentialIdGen
import org.neo4j.cypher.internal.util.symbols.CTInteger
import org.neo4j.cypher.internal.util.symbols.CTList
import org.neo4j.cypher.internal.util.symbols.CTNode
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class LogicalPlan2PlanDescriptionTest extends CypherFunSuite with TableDrivenPropertyChecks {

  private val CYPHER_VERSION = Version("CYPHER 4.1")
  private val RUNTIME_VERSION = RuntimeVersion("4.1")
  private val PLANNER_VERSION = PlannerVersion("4.1")

  implicit val idGen: IdGen = new SequentialIdGen()
  val readOnly = true
  val cardinalities = new Cardinalities
  val providedOrders = new ProvidedOrders
  val id = Id.INVALID_ID

  def attach[P <: LogicalPlan](plan: P, cardinality: Cardinality, providedOrder: ProvidedOrder = ProvidedOrder.empty): P = {
    cardinalities.set(plan.id, cardinality)
    providedOrders.set(plan.id, providedOrder)
    plan
  }

  val privLhsLP = attach(plans.AssertDbmsAdmin(ShowUserAction), 2.0, ProvidedOrder.empty)
  val privLhsPD = PlanDescriptionImpl(id, "AssertDbmsAdmin", NoChildren, Seq(Details("SHOW USER"), EstimatedRows(2)), Set.empty)

  val lhsLP = attach(AllNodesScan("a", Set.empty), 2.0, ProvidedOrder.empty)
  val lhsPD = PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("a"), EstimatedRows(2)), Set("a"))

  val rhsLP = attach(AllNodesScan("b", Set.empty), 2.0, ProvidedOrder.empty)
  val rhsPD = PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("b"), EstimatedRows(2)), Set("b"))

  private val pos: InputPosition = DummyPosition(0)

  test("Validate all arguments") {
    assertGood(
      attach(AllNodesScan("a", Set.empty), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("a"), EstimatedRows(1), Order(ProvidedOrder.asc(varFor("a"))), CYPHER_VERSION, RUNTIME_VERSION, Planner("COST"), PlannerImpl("IDP"), PLANNER_VERSION), Set("a")),
      validateAllArgs = true)

    assertGood(attach(Input(Seq("n1", "n2"), Seq("r"), Seq("v1", "v2"), nullable = false), 42.3),
      PlanDescriptionImpl(id, "Input", NoChildren, Seq(Details(Seq("n1", "n2", "r", "v1", "v2")), EstimatedRows(42.3), CYPHER_VERSION, RUNTIME_VERSION, Planner("COST"), PlannerImpl("IDP"), PLANNER_VERSION), Set("n1", "n2", "r", "v1", "v2")),
      validateAllArgs = true)
  }

  // Leaf Plans
  test("AllNodesScan") {
    assertGood(
      attach(AllNodesScan("a", Set.empty), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("a"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    assertGood(
      attach(AllNodesScan("b", Set.empty), 42.0, ProvidedOrder.asc(varFor("b")).desc(prop("b", "foo"))),
      PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("b"), Order(ProvidedOrder.asc(varFor("b")).desc(prop("b", "foo")))), Set("b")))
  }

  test("NodeByLabelScan") {
    assertGood(
      attach(NodeByLabelScan("node", label("X"), Set.empty), 33.0),
      PlanDescriptionImpl(id, "NodeByLabelScan", NoChildren, Seq(Details("node:X")), Set("node")))
  }

  test("NodeByIdSeek") {
    assertGood(
      attach(NodeByIdSeek("node", ManySeekableArgs(ListLiteral(Seq(number("1"), number("32")))(pos)), Set.empty), 333.0),
      PlanDescriptionImpl(id, "NodeByIdSeek", NoChildren, Seq(Details("node WHERE id(node) IN [1,32]")), Set("node")))
  }

  test("IndexSeek") {
    assertGood(
      attach(IndexSeek("x:Label(Prop)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexScan", NoChildren, Seq(Details("x:Label(Prop) WHERE exists(Prop)")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop)", getValue = GetValue), 23.0),
      PlanDescriptionImpl(id, "NodeIndexScan", NoChildren, Seq(Details("x:Label(Prop) WHERE exists(Prop), cache[x.Prop]")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop,Foo)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexScan", NoChildren, Seq(Details("x:Label(Prop, Foo) WHERE exists(Prop) AND exists(Foo)")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop,Foo)", getValue = GetValue), 23.0),
      PlanDescriptionImpl(id, "NodeIndexScan", NoChildren, Seq(Details("x:Label(Prop, Foo) WHERE exists(Prop) AND exists(Foo), cache[x.Prop], cache[x.Foo]")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 'Andres')"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeek", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop = \"Andres\"")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 'Andres')", getValue = GetValue), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeek", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop = \"Andres\", cache[x.Prop]")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 'Andres')", unique = true), 23.0),
      PlanDescriptionImpl(id, "NodeUniqueIndexSeek", NoChildren, Seq(Details("UNIQUE x:Label(Prop) WHERE Prop = \"Andres\"")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 'Andres' OR 'Pontus')"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeek", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop IN [\"Andres\", \"Pontus\"]")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 'Andres' OR 'Pontus')", unique = true), 23.0),
      PlanDescriptionImpl(id, "NodeUniqueIndexSeek", NoChildren, Seq(Details("UNIQUE x:Label(Prop) WHERE Prop IN [\"Andres\", \"Pontus\"]")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop > 9)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeekByRange", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop > 9")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop < 9)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeekByRange", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop < 9")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(9 <= Prop <= 11)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeekByRange", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop >= 9 AND Prop <= 11")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop STARTS WITH 'Foo')"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeekByRange", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop STARTS WITH \"Foo\"")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop ENDS WITH 'Foo')"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexEndsWithScan", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop ENDS WITH \"Foo\"")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop CONTAINS 'Foo')"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexContainsScan", NoChildren, Seq(Details("x:Label(Prop) WHERE Prop CONTAINS \"Foo\"")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 10,Foo = 12)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeek", NoChildren, Seq(Details("x:Label(Prop, Foo) WHERE Prop = 10 AND Foo = 12")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop = 10,Foo = 12)", unique = true), 23.0),
      PlanDescriptionImpl(id, "NodeUniqueIndexSeek", NoChildren, Seq(Details("UNIQUE x:Label(Prop, Foo) WHERE Prop = 10 AND Foo = 12")), Set("x")))

    assertGood(
      attach(IndexSeek("x:Label(Prop > 10,Foo)"), 23.0),
      PlanDescriptionImpl(id, "NodeIndexSeek", NoChildren, Seq(Details("x:Label(Prop, Foo) WHERE Prop > 10 AND exists(Foo)")), Set("x")))

    // This is ManyQueryExpression with only a single expression. That is possible to get, but the test utility IndexSeek cannot create those.
    assertGood(
      attach(NodeUniqueIndexSeek("x", LabelToken("Label", LabelId(0)), Seq(IndexedProperty(PropertyKeyToken("Prop", PropertyKeyId(0)), DoNotGetValue)),
        ManyQueryExpression(ListLiteral(Seq(StringLiteral("Andres")(pos)))(pos)), Set.empty, IndexOrderNone), 95.0),
      PlanDescriptionImpl(id, "NodeUniqueIndexSeek", NoChildren, Seq(Details("UNIQUE x:Label(Prop) WHERE Prop = \"Andres\"")), Set("x")))

    assertGood(
      attach(NodeIndexSeek("x", LabelToken("Label", LabelId(0)), Seq(IndexedProperty(PropertyKeyToken("Prop", PropertyKeyId(0)), DoNotGetValue)),
        RangeQueryExpression(PointDistanceSeekRangeWrapper(PointDistanceRange(
          FunctionInvocation(MapExpression(Seq(
            (key("x"), number("1")),
            (key("y"), number("2")),
            (key("crs"), StringLiteral("cartesian")(pos))
          ))(pos), FunctionName(Point.name)(pos)), number("10"), inclusive = true
        ))(pos)),
        Set.empty, IndexOrderNone),
        95.0),
      PlanDescriptionImpl(id, "NodeIndexSeekByRange", NoChildren, Seq(Details("x:Label(Prop) WHERE distance(Prop, point(1, 2, \"cartesian\")) <= 10")), Set("x")))
  }

  test("MultiNodeIndexSeek") {
    assertGood(
      attach(MultiNodeIndexSeek(Seq(IndexSeek("x:Label(Prop = 10,Foo = 12)", unique = true).asInstanceOf[IndexSeekLeafPlan], IndexSeek("y:Label(Prop = 12)", unique = false).asInstanceOf[IndexSeekLeafPlan])), 230.0),
      PlanDescriptionImpl(id, "MultiNodeIndexSeek", NoChildren, Seq(Details(Seq("UNIQUE x:Label(Prop, Foo) WHERE Prop = 10 AND Foo = 12", "y:Label(Prop) WHERE Prop = 12"))), Set("x", "y"))
    )
  }

  test("ProduceResult") {
    assertGood(attach(ProduceResult(lhsLP, Seq("a", "b")), 12.0),
      PlanDescriptionImpl(id, "ProduceResults", SingleChild(lhsPD), Seq(Details(Seq("a", "b"))), Set("a")))
  }

  test("Argument") {
    assertGood(attach(plans.Argument(Set.empty), 95.0),
      PlanDescriptionImpl(id, "EmptyRow", NoChildren, Seq.empty, Set.empty))

    assertGood(attach(plans.Argument(Set("a", "b")), 95.0),
      PlanDescriptionImpl(id, "Argument", NoChildren, Seq(Details("a, b")), Set("a", "b")))
  }

  test("RelationshipByIdSeek") {
    assertGood(attach(DirectedRelationshipByIdSeek("r", SingleSeekableArg(number("1")), "a", "b", Set.empty), 70.0),
      PlanDescriptionImpl(id, "DirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]->(b) WHERE id(r) = 1")), Set("r", "a", "b")))

    assertGood(attach(DirectedRelationshipByIdSeek("r", SingleSeekableArg(number("1")), "a", "b", Set("x")), 70.0),
      PlanDescriptionImpl(id, "DirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]->(b) WHERE id(r) = 1")), Set("r", "a", "b", "x")))

    assertGood(attach(DirectedRelationshipByIdSeek("r", ManySeekableArgs(ListLiteral(Seq(number("1")))(pos)), "a", "b", Set("x")), 70.0),
      PlanDescriptionImpl(id, "DirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]->(b) WHERE id(r) = 1")), Set("r", "a", "b", "x")))

    assertGood(attach(DirectedRelationshipByIdSeek("r", ManySeekableArgs(ListLiteral(Seq(number("1"), number("2")))(pos)), "a", "b", Set("x")), 70.0),
      PlanDescriptionImpl(id, "DirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]->(b) WHERE id(r) IN [1,2]")), Set("r", "a", "b", "x")))

    assertGood(attach(DirectedRelationshipByIdSeek("r", ManySeekableArgs(number("1")), "a", "b", Set("x")), 70.0),
      PlanDescriptionImpl(id, "DirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]->(b) WHERE id(r) = 1")), Set("r", "a", "b", "x")))

    assertGood(attach(UndirectedRelationshipByIdSeek("r", ManySeekableArgs(number("1")), "a", "b", Set("x")), 70.0),
      PlanDescriptionImpl(id, "UndirectedRelationshipByIdSeek", NoChildren, Seq(Details("(a)-[r]-(b) WHERE id(r) = 1")), Set("r", "a", "b", "x")))
  }

  test("LoadCSV") {
    assertGood(
      attach(LoadCSV(lhsLP, StringLiteral("file:///tmp/foo.csv")(pos), "u", NoHeaders, None, legacyCsvQuoteEscaping = false, csvBufferSize = 2), 27.6),
      PlanDescriptionImpl(id, "LoadCSV", SingleChild(lhsPD), Seq(Details("u")), Set("u", "a")))
  }

  test("Input") {
    assertGood(attach(Input(Seq("n1", "n2"), Seq("r"), Seq("v1", "v2"), nullable = false), 4.0),
      PlanDescriptionImpl(id, "Input", NoChildren, Seq(Details(Seq("n1", "n2", "r", "v1", "v2"))), Set("n1", "n2", "r", "v1", "v2")))
  }

  test("NodeCountFromCountStore") {
    assertGood(attach(NodeCountFromCountStore("x", List(Some(label("LabelName"))), Set.empty), 54.2),
      PlanDescriptionImpl(id, "NodeCountFromCountStore", NoChildren, Seq(Details("count( (:LabelName) ) AS x")), Set("x")))

    assertGood(attach(NodeCountFromCountStore("x", List(Some(label("LabelName")), Some(label("LabelName2"))), Set.empty), 54.2),
      PlanDescriptionImpl(id, "NodeCountFromCountStore", NoChildren, Seq(Details("count( (:LabelName:LabelName2) ) AS x")), Set("x")))

    assertGood(attach(NodeCountFromCountStore("  UNNAMED123", List(Some(label("LabelName"))), Set.empty), 54.2),
      PlanDescriptionImpl(id, "NodeCountFromCountStore", NoChildren, Seq(Details("count( (:LabelName) )")), Set("  UNNAMED123")))
  }

  test("ProcedureCall") {
    val ns = Namespace(List("my", "proc"))(pos)
    val name = ProcedureName("foo")(pos)
    val qualifiedName = QualifiedName(ns.parts, name.name)
    val signatureInputs = IndexedSeq(FieldSignature("a", CTInteger))
    val signatureOutputs = Some(IndexedSeq(FieldSignature("x", CTInteger), FieldSignature("y", CTList(CTNode))))
    val signature = ProcedureSignature(qualifiedName, signatureInputs, signatureOutputs, None, ProcedureReadOnlyAccess(Array.empty), id = 42)
    val callResults = IndexedSeq(ProcedureResultItem(varFor("x"))(pos), ProcedureResultItem(varFor("y"))(pos))
    val call = ResolvedCall(signature, Seq(varFor("a1")), callResults)(pos)

    assertGood(attach(ProcedureCall(lhsLP, call), 33.2),
      PlanDescriptionImpl(id, "ProcedureCall", SingleChild(lhsPD), Seq(Details("my.proc.foo(a1) :: (x :: INTEGER?, y :: LIST? OF NODE?)")), Set("a", "x", "y")))
  }

  test("RelationshipCountFromCountStore") {
    assertGood(attach(RelationshipCountFromCountStore("x", None, Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), None, Set.empty), 54.2),
      PlanDescriptionImpl(id, "RelationshipCountFromCountStore", NoChildren, Seq(Details("count( ()-[:LIKES|LOVES]->() ) AS x")), Set("x")))

    assertGood(attach(RelationshipCountFromCountStore("  UNNAMED122", None, Seq(RelTypeName("LIKES")(pos)), None, Set.empty), 54.2),
      PlanDescriptionImpl(id, "RelationshipCountFromCountStore", NoChildren, Seq(Details("count( ()-[:LIKES]->() )")), Set("  UNNAMED122")))

    assertGood(attach(RelationshipCountFromCountStore("x", Some(label("StartLabel")), Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), None, Set.empty), 54.2),
      PlanDescriptionImpl(id, "RelationshipCountFromCountStore", NoChildren, Seq(Details("count( (:StartLabel)-[:LIKES|LOVES]->() ) AS x")), Set("x")))

    assertGood(attach(RelationshipCountFromCountStore("x", Some(label("StartLabel")), Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), Some(label("EndLabel")), Set.empty), 54.2),
      PlanDescriptionImpl(id, "RelationshipCountFromCountStore", NoChildren, Seq(Details("count( (:StartLabel)-[:LIKES|LOVES]->(:EndLabel) ) AS x")), Set("x")))
  }

  test("CreateIndex") {
    assertGood(attach(CreateIndex(label("Label"), List(PropertyKeyName("prop")(pos)), Some("$indexName")), 63.2),
      PlanDescriptionImpl(id, "CreateIndex", NoChildren, Seq(Details("INDEX `$indexName` FOR (:Label) ON (prop)")), Set.empty))

    assertGood(attach(CreateIndex(label("Label"), List(PropertyKeyName("prop")(pos)), None), 63.2),
      PlanDescriptionImpl(id, "CreateIndex", NoChildren, Seq(Details("INDEX FOR (:Label) ON (prop)")), Set.empty))
  }

  test("DropIndex") {
    assertGood(attach(DropIndex(label("Label"), List(PropertyKeyName("prop")(pos))), 63.2),
      PlanDescriptionImpl(id, "DropIndex", NoChildren, Seq(Details("INDEX FOR (:Label) ON (prop)")), Set.empty))

    assertGood(attach(DropIndex(label("Label"), List(PropertyKeyName("prop1")(pos), PropertyKeyName("prop2")(pos))), 63.2),
      PlanDescriptionImpl(id, "DropIndex", NoChildren, Seq(Details("INDEX FOR (:Label) ON (prop1, prop2)")), Set.empty))
  }

  test("DropIndexOnName") {
    assertGood(attach(DropIndexOnName("indexName"), 63.2),
      PlanDescriptionImpl(id, "DropIndex", NoChildren, Seq(Details("INDEX indexName")), Set.empty))
  }

  test("CreateUniquePropertyConstraint") {
    assertGood(attach(CreateUniquePropertyConstraint(" x", label("Label"), Seq(prop(" x", "prop")), None), 63.2),
      PlanDescriptionImpl(id, "CreateUniquePropertyConstraint", NoChildren, Seq(Details("CONSTRAINT ON (` x`:Label) ASSERT (` x`.prop) IS UNIQUE")), Set.empty))

    assertGood(attach(CreateUniquePropertyConstraint("x", label("Label"), Seq(prop("x", "prop")), Some("constraintName")), 63.2),
      PlanDescriptionImpl(id, "CreateUniquePropertyConstraint", NoChildren, Seq(Details("CONSTRAINT constraintName ON (x:Label) ASSERT (x.prop) IS UNIQUE")), Set.empty))

    assertGood(attach(CreateUniquePropertyConstraint("x", label("Label"), Seq(prop("x", "prop1"), prop("x", "prop2")), Some("constraintName")), 63.2),
      PlanDescriptionImpl(id, "CreateUniquePropertyConstraint", NoChildren, Seq(Details("CONSTRAINT constraintName ON (x:Label) ASSERT (x.prop1, x.prop2) IS UNIQUE")), Set.empty))
  }

  test("CreateNodeKeyConstraint") {
    assertGood(attach(CreateNodeKeyConstraint(" x", label("Label"), Seq(prop(" x", "prop")), None), 63.2),
      PlanDescriptionImpl(id, "CreateNodeKeyConstraint", NoChildren, Seq(Details("CONSTRAINT ON (` x`:Label) ASSERT (` x`.prop) IS NODE KEY")), Set.empty))

    assertGood(attach(CreateNodeKeyConstraint("x", label("Label"), Seq(prop("x", "prop1"), prop("x", "prop2")), Some("constraintName")), 63.2),
      PlanDescriptionImpl(id, "CreateNodeKeyConstraint", NoChildren, Seq(Details("CONSTRAINT constraintName ON (x:Label) ASSERT (x.prop1, x.prop2) IS NODE KEY")), Set.empty))
  }

  test("CreateNodePropertyExistenceConstraint") {
    assertGood(attach(CreateNodePropertyExistenceConstraint(label("Label"), prop(" x", "prop"), None), 63.2),
      PlanDescriptionImpl(id, "CreateNodePropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT ON (` x`:Label) ASSERT exists(` x`.prop)")), Set.empty))

    assertGood(attach(CreateNodePropertyExistenceConstraint(label("Label"), prop("x","prop"), Some("constraintName")), 63.2),
      PlanDescriptionImpl(id, "CreateNodePropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT constraintName ON (x:Label) ASSERT exists(x.prop)")), Set.empty))
  }

  test("CreateRelationshipPropertyExistenceConstraint") {
    assertGood(attach(CreateRelationshipPropertyExistenceConstraint(RelTypeName("R")(pos), prop(" x", "prop"), None), 63.2),
      PlanDescriptionImpl(id, "CreateRelationshipPropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT ON ()-[` x`:R]-() ASSERT exists(` x`.prop)")), Set.empty))

    assertGood(attach(CreateRelationshipPropertyExistenceConstraint(RelTypeName("R")(pos), prop(" x", "prop"), Some("constraintName")), 63.2),
      PlanDescriptionImpl(id, "CreateRelationshipPropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT constraintName ON ()-[` x`:R]-() ASSERT exists(` x`.prop)")), Set.empty))
  }

  test("DropUniquePropertyConstraint") {
    assertGood(attach(DropUniquePropertyConstraint(LabelName("Label")(pos), Seq(prop(" x", "prop"), prop(" x", "prop2"))), 63.2),
      PlanDescriptionImpl(id, "DropUniquePropertyConstraint", NoChildren, Seq(Details("CONSTRAINT ON (` x`:Label) ASSERT (` x`.prop, ` x`.prop2) IS UNIQUE")), Set.empty))
  }

  test("DropNodeKeyConstraint") {
    assertGood(attach(DropNodeKeyConstraint(LabelName("Label")(pos), Seq(prop(" x", "prop"), prop(" x", "prop2"))), 63.2),
      PlanDescriptionImpl(id, "DropNodeKeyConstraint", NoChildren, Seq(Details("CONSTRAINT ON (` x`:Label) ASSERT (` x`.prop, ` x`.prop2) IS NODE KEY")), Set.empty))
  }

  test("DropNodePropertyExistenceConstraint") {
    assertGood(attach(DropNodePropertyExistenceConstraint(LabelName("Label")(pos), prop("x", " prop")), 63.2),
      PlanDescriptionImpl(id, "DropNodePropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT ON (x:Label) ASSERT exists(x.` prop`)")), Set.empty))
  }

  test("DropRelationshipPropertyExistenceConstraint") {
    assertGood(attach(DropRelationshipPropertyExistenceConstraint(RelTypeName("$R")(pos), prop("x", " prop")), 63.2),
      PlanDescriptionImpl(id, "DropRelationshipPropertyExistenceConstraint", NoChildren, Seq(Details("CONSTRAINT ON ()-[x:`$R`]-() ASSERT exists(x.` prop`)")), Set.empty))
  }

  test("DropConstraintOnName") {
    assertGood(attach(DropConstraintOnName("name"), 63.2),
      PlanDescriptionImpl(id, "DropConstraint", NoChildren, Seq(Details("CONSTRAINT name")), Set.empty))
  }

  test("Aggregation") {
    // Aggregation 1 grouping, 0 aggregating
    assertGood(
      attach(Aggregation(lhsLP, Map("a" -> varFor("a")), Map.empty), 17.5),
      PlanDescriptionImpl(id, "EagerAggregation", SingleChild(lhsPD), Seq(Details("a")), Set("a")))

    // Aggregation 2 grouping, 0 aggregating
    assertGood(
      attach(Aggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c")), Map.empty), 17.5),
      PlanDescriptionImpl(id, "EagerAggregation", SingleChild(lhsPD), Seq(Details("a, c AS b")), Set("a", "b")))

    val countFunction = FunctionInvocation(FunctionName(Count.name)(pos), distinct = false, IndexedSeq(varFor("c")))(pos)
    val collectDistinctFunction = FunctionInvocation(FunctionName(Collect.name)(pos), distinct = true, IndexedSeq(varFor("c")))(pos)

    // Aggregation 1 grouping, 1 aggregating
    assertGood(
      attach(Aggregation(lhsLP, Map("a" -> varFor("a")), Map("count" -> countFunction)), 1.3),
      PlanDescriptionImpl(id, "EagerAggregation", SingleChild(lhsPD), Seq(Details("a, count(c) AS count")), Set("a", "count")))

    // Aggregation 2 grouping, 2 aggregating
    assertGood(
      attach(Aggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c")), Map("count(c)" -> countFunction, "collect" -> collectDistinctFunction)), 1.3),
      PlanDescriptionImpl(id, "EagerAggregation", SingleChild(lhsPD), Seq(Details("a, c AS b, count(c), collect(DISTINCT c) AS collect")), Set("a", "b", "count(c)", "collect")))

    // Distinct 1 grouping
    assertGood(
      attach(Distinct(lhsLP, Map("  a@23" -> varFor("  a@23"))), 45.9),
      PlanDescriptionImpl(id, "Distinct", SingleChild(lhsPD), Seq(Details("a")), Set("  a@23")))

    // Distinct 2 grouping
    assertGood(
      attach(Distinct(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"))), 45.9),
      PlanDescriptionImpl(id, "Distinct", SingleChild(lhsPD), Seq(Details("a, c AS b")), Set("a", "b")))

    // OrderedDistinct 1 column, 1 sorted
    assertGood(
      attach(OrderedDistinct(lhsLP, Map("a" -> varFor("a")), Seq(varFor("a"))), 45.9),
      PlanDescriptionImpl(id, "OrderedDistinct", SingleChild(lhsPD), Seq(Details("a")), Set("a")))

    // OrderedDistinct 3 column, 2 sorted
    assertGood(
      attach(OrderedDistinct(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"), "d" -> varFor("d")), Seq(varFor("d"), varFor("a"))), 45.9),
      PlanDescriptionImpl(id, "OrderedDistinct", SingleChild(lhsPD), Seq(Details("d, a, c AS b")), Set("a", "b", "d")))

    // OrderedAggregation 1 grouping, 0 aggregating, 1 sorted
    assertGood(
      attach(OrderedAggregation(lhsLP, Map("a" -> varFor("a")), Map.empty, Seq(varFor("a"))), 17.5),
      PlanDescriptionImpl(id, "OrderedAggregation", SingleChild(lhsPD), Seq(Details("a")), Set("a")))

    // OrderedAggregation 3 grouping, 0 aggregating, 2 sorted
    assertGood(
      attach(OrderedAggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"), "d" -> varFor("d")), Map.empty, Seq(varFor("d"), varFor("a"))), 17.5),
      PlanDescriptionImpl(id, "OrderedAggregation", SingleChild(lhsPD), Seq(Details("d, a, c AS b")), Set("a", "b", "d")))

    // OrderedAggregation 3 grouping, 1 aggregating with alias, 2 sorted
    assertGood(
      attach(OrderedAggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"), "d" -> varFor("d")), Map("count" -> countFunction), Seq(varFor("d"), varFor("a"))), 1.3),
      PlanDescriptionImpl(id, "OrderedAggregation", SingleChild(lhsPD), Seq(Details("d, a, c AS b, count(c) AS count")), Set("a", "b", "d", "count")))

    // OrderedAggregation 3 grouping, 1 aggregating without alias, 2 sorted
    assertGood(
      attach(OrderedAggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"), "d" -> varFor("d")), Map("collect(DISTINCT c)" -> collectDistinctFunction), Seq(varFor("d"), varFor("a"))), 1.3),
      PlanDescriptionImpl(id, "OrderedAggregation", SingleChild(lhsPD), Seq(Details("d, a, c AS b, collect(DISTINCT c)")), Set("a", "b", "d", "collect(DISTINCT c)")))

    // OrderedAggregation 3 grouping, 1 aggregating with alias, sorted on aliased column
    assertGood(
      attach(OrderedAggregation(lhsLP, Map("a" -> varFor("a"), "b" -> varFor("c"), "d" -> varFor("d")), Map("collect(DISTINCT c)" -> collectDistinctFunction), Seq(varFor("d"), varFor("c"))), 1.3),
      PlanDescriptionImpl(id, "OrderedAggregation", SingleChild(lhsPD), Seq(Details("d, c AS b, a, collect(DISTINCT c)")), Set("a", "b", "d", "collect(DISTINCT c)")))
  }

  test("Create") {
    val properties = MapExpression(Seq(
      (key("y"), number("1")),
      (key("crs"), StringLiteral("cartesian")(pos))))(pos)

    assertGood(
      attach(Create(lhsLP, Seq(CreateNode("x", Seq.empty, None)), Seq(CreateRelationship("r", "x", RelTypeName("R")(pos), "y", SemanticDirection.INCOMING, None))), 32.2),
      PlanDescriptionImpl(id, "Create", SingleChild(lhsPD), Seq(Details(Seq("(x)", "(x)<-[r:R]-(y)"))), Set("a", "x", "r")))

    assertGood(
      attach(Create(lhsLP, Seq(CreateNode("x", Seq(label("Label")), None)), Seq(CreateRelationship("r", "x", RelTypeName("R")(pos), "y", SemanticDirection.INCOMING, None))), 32.2),
      PlanDescriptionImpl(id, "Create", SingleChild(lhsPD), Seq(Details(Seq("(x:Label)", "(x)<-[r:R]-(y)"))), Set("a", "x", "r")))

    assertGood(
      attach(Create(lhsLP, Seq(CreateNode("x", Seq(label("Label1"), label("Label2")), None)), Seq(CreateRelationship("r", "x", RelTypeName("R")(pos), "y", SemanticDirection.INCOMING, None))), 32.2),
      PlanDescriptionImpl(id, "Create", SingleChild(lhsPD), Seq(Details(Seq("(x:Label1:Label2)", "(x)<-[r:R]-(y)"))), Set("a", "x", "r")))

    assertGood(
      attach(Create(lhsLP, Seq(CreateNode("x", Seq(label("Label")), Some(properties))), Seq(CreateRelationship("r", "x", RelTypeName("R")(pos), "y", SemanticDirection.INCOMING, Some(properties)))), 32.2),
      PlanDescriptionImpl(id, "Create", SingleChild(lhsPD), Seq(Details(Seq("(x:Label)", "(x)<-[r:R]-(y)"))), Set("a", "x", "r")))
  }

  test("Delete") {
    assertGood(attach(DeleteExpression(lhsLP, prop("x", "prop")), 34.5),
      PlanDescriptionImpl(id, "Delete", SingleChild(lhsPD), Seq(Details("x.prop")), Set("a")))

    assertGood(attach(DeleteNode(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "Delete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))

    assertGood(attach(DeletePath(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "Delete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))

    assertGood(attach(DeleteRelationship(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "Delete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))

    assertGood(attach(DetachDeleteExpression(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "DetachDelete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))

    assertGood(attach(DetachDeleteNode(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "DetachDelete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))

    assertGood(attach(DetachDeletePath(lhsLP, varFor("x")), 34.5),
      PlanDescriptionImpl(id, "DetachDelete", SingleChild(lhsPD), Seq(Details("x")), Set("a")))
  }

  test("Eager") {
    assertGood(attach(Eager(lhsLP), 34.5),
      PlanDescriptionImpl(id, "Eager", SingleChild(lhsPD), Seq.empty, Set("a")))
  }

  test("EmptyResult") {
    assertGood(attach(EmptyResult(lhsLP), 34.5),
      PlanDescriptionImpl(id, "EmptyResult", SingleChild(lhsPD), Seq.empty, Set("a")))
  }

  test("DropResult") {
    assertGood(attach(DropResult(lhsLP), 34.5),
      PlanDescriptionImpl(id, "DropResult", SingleChild(lhsPD), Seq.empty, Set("a")))
  }

  test("ErrorPlan") {
    assertGood(attach(ErrorPlan(lhsLP, new Exception("Exception")), 12.5),
      PlanDescriptionImpl(id, "Error", SingleChild(lhsPD), Seq.empty, Set("a")))
  }

  test("Expand") {
    assertGood(attach(Expand(lhsLP, "a", OUTGOING, Seq.empty, "y", "r1", ExpandAll), 95.0),
      PlanDescriptionImpl(id, "Expand(All)", SingleChild(lhsPD), Seq(Details("(a)-[r1]->(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", INCOMING, Seq(RelTypeName("R")(pos)), "y", "r1", ExpandAll), 95.0),
      PlanDescriptionImpl(id, "Expand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[r1:R]-(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", BOTH, Seq(RelTypeName("R1")(pos), RelTypeName("R2")(pos)), "y", "r1", ExpandAll), 95.0),
      PlanDescriptionImpl(id, "Expand(All)", SingleChild(lhsPD), Seq(Details("(a)-[r1:R1|R2]-(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", OUTGOING, Seq.empty, "y", "r1", ExpandInto), 113.0),
      PlanDescriptionImpl(id, "Expand(Into)", SingleChild(lhsPD), Seq(Details("(a)-[r1]->(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", INCOMING, Seq(RelTypeName("R")(pos)), "y", "r1", ExpandInto), 113.0),
      PlanDescriptionImpl(id, "Expand(Into)", SingleChild(lhsPD), Seq(Details("(a)<-[r1:R]-(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", BOTH, Seq(RelTypeName("R1")(pos), RelTypeName("R2")(pos)), "y", "r1", ExpandInto), 113.0),
      PlanDescriptionImpl(id, "Expand(Into)", SingleChild(lhsPD), Seq(Details("(a)-[r1:R1|R2]-(y)")), Set("a", "y", "r1")))

    assertGood(attach(Expand(lhsLP, "a", BOTH, Seq(RelTypeName("R1")(pos), RelTypeName("R2")(pos)), "y", "  UNNAMED1", ExpandInto), 113.0),
      PlanDescriptionImpl(id, "Expand(Into)", SingleChild(lhsPD), Seq(Details("(a)-[:R1|R2]-(y)")), Set("a", "y", "  UNNAMED1")))
  }

  test("Limit") {
    assertGood(attach(Limit(lhsLP, number("1"), DoNotIncludeTies), 113.0),
      PlanDescriptionImpl(id, "Limit", SingleChild(lhsPD), Seq(Details("1")), Set("a")))
  }

  test("CachedProperties") {
    assertGood(attach(CacheProperties(lhsLP, Set(cachedProp("n", "prop"))), 113.0),
      PlanDescriptionImpl(id, "CacheProperties", SingleChild(lhsPD), Seq(Details("cache[n.prop]")), Set("a")))

    assertGood(attach(CacheProperties(lhsLP, Set(prop("n", "prop1"), prop("n", "prop2"))), 113.0),
      PlanDescriptionImpl(id, "CacheProperties", SingleChild(lhsPD), Seq(Details("n.prop1, n.prop2")), Set("a")))
  }

  test("LockNodes") {
    assertGood(attach(LockNodes(lhsLP, Set("a", "b")), 12.0),
      PlanDescriptionImpl(id, "LockNodes", SingleChild(lhsPD), Seq(Details("a, b")), Set("a")))
  }

  test("OptionalExpand") {
    val predicate1 = Equals(varFor("to"), number("32"))(pos)
    val predicate2 = LessThan(varFor("a"), number("2002"))(pos)

    // Without predicate
    assertGood(attach(OptionalExpand(lhsLP, "a", INCOMING, Seq(RelTypeName("R")(pos)), "to", "r", ExpandAll, None), 12.0),
      PlanDescriptionImpl(id, "OptionalExpand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[r:R]-(to)")), Set("a", "to", "r")))

    // With predicate and no relationship types
    assertGood(attach(OptionalExpand(lhsLP, "a", OUTGOING, Seq(), "to", "r", ExpandAll, Some(predicate1)), 12.0),
      PlanDescriptionImpl(id, "OptionalExpand(All)", SingleChild(lhsPD), Seq(Details("(a)-[r]->(to) WHERE to = 32")), Set("a", "to", "r")))

    // With predicate and relationship types
    assertGood(attach(OptionalExpand(lhsLP, "a", BOTH, Seq(RelTypeName("R")(pos)), "to", "r", ExpandAll, Some(And(predicate1, predicate2)(pos))), 12.0),
      PlanDescriptionImpl(id, "OptionalExpand(All)", SingleChild(lhsPD), Seq(Details("(a)-[r:R]-(to) WHERE to = 32 AND a < 2002")), Set("a", "to", "r")))

    // With multiple relationship types
    assertGood(attach(OptionalExpand(lhsLP, "a", INCOMING, Seq(RelTypeName("R1")(pos), RelTypeName("R2")(pos)), "to", "r", ExpandAll, None), 12.0),
      PlanDescriptionImpl(id, "OptionalExpand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[r:R1|R2]-(to)")), Set("a", "to", "r")))
  }

  test("Projection") {
    assertGood(attach(Projection(lhsLP, Map("x" -> varFor("y"))), 2345.0),
      PlanDescriptionImpl(id, "Projection", SingleChild(lhsPD), Seq(Details("y AS x")), Set("a", "x")))

    assertGood(attach(Projection(lhsLP, Map("x" -> varFor("y"), "n.prop" -> prop("n", "prop"))), 2345.0),
      PlanDescriptionImpl(id, "Projection", SingleChild(lhsPD), Seq(Details("y AS x, n.prop")), Set("a", "x", "n.prop")))

    // Projection should show up in the order they were specified in
    assertGood(attach(Projection(lhsLP, Map("n.prop" -> prop("n", "prop"), "x" -> varFor("y"))), 2345.0),
      PlanDescriptionImpl(id, "Projection", SingleChild(lhsPD), Seq(Details("n.prop, y AS x")), Set("a", "x", "n.prop")))
  }

  test("Selection") {
    val predicate1 = Equals(varFor("x"), number("32"))(pos)
    val predicate2 = LessThan(varFor("y"), number("2002"))(pos)

    assertGood(attach(Selection(Seq(predicate1, predicate2), lhsLP), 2345.0),
      PlanDescriptionImpl(id, "Filter", SingleChild(lhsPD), Seq(Details("x = 32 AND y < 2002")), Set("a")))
  }

  test("Skip") {
    assertGood(attach(Skip(lhsLP, number("78")), 2345.0),
      PlanDescriptionImpl(id, "Skip", SingleChild(lhsPD), Seq(Details("78")), Set("a")))
  }

  test("FindShortestPaths") {
    val predicate = Equals(prop("r", "prop"), number("32"))(pos)

    // with(out) relationship types
    // length variations

    // without: predicates, path name, relationship type
    assertGood(attach(FindShortestPaths(lhsLP, ShortestPathPattern(None, PatternRelationship("r", ("a", "y"), SemanticDirection.BOTH, Seq.empty, VarPatternLength(2, Some(4))), single = true)(null), Seq.empty), 2345.0),
      PlanDescriptionImpl(id, "ShortestPath", SingleChild(lhsPD), Seq(Details("(a)-[r*2..4]-(y)")), Set("r", "a", "y")))

    // with: predicates, path name, relationship type
    assertGood(attach(FindShortestPaths(lhsLP, ShortestPathPattern(Some("p"), PatternRelationship("r", ("a", "y"), SemanticDirection.BOTH, Seq(RelTypeName("R")(pos)), VarPatternLength(2, Some(4))), single = true)(null), Seq(predicate)), 2345.0),
      PlanDescriptionImpl(id, "ShortestPath", SingleChild(lhsPD), Seq(Details("p = (a)-[r:R*2..4]-(y) WHERE r.prop = 32")), Set("r", "a", "y", "p")))

    // with: predicates, UNNAMED variables, relationship type, unbounded max length
    assertGood(attach(FindShortestPaths(lhsLP, ShortestPathPattern(None, PatternRelationship("r", ("a", "  UNNAMED2"), SemanticDirection.BOTH, Seq(RelTypeName("R")(pos)), VarPatternLength(2, None)), single = true)(null), Seq(predicate)), 2345.0),
      PlanDescriptionImpl(id, "ShortestPath", SingleChild(lhsPD), Seq(Details("(a)-[r:R*2..]-() WHERE r.prop = 32")), Set("r", "a", "  UNNAMED2")))
  }

  test("MergeCreate") {
    // NOTE: currently tests only assert that we do NOT render anything other than the introduced variable
    val properties = MapExpression(Seq(
      (key("y"), number("1")),
      (key("crs"), StringLiteral("cartesian")(pos))))(pos)

    assertGood(attach(MergeCreateNode(lhsLP, "x", Seq(label("L1"), label("L2")), Some(properties)), 113.0),
      PlanDescriptionImpl(id, "MergeCreateNode", SingleChild(lhsPD), Seq(Details("x")), Set("a", "x")))

    assertGood(attach(MergeCreateRelationship(lhsLP, "r", "x", RelTypeName("R")(pos), "y", Some(properties)), 113.0),
      PlanDescriptionImpl(id, "MergeCreateRelationship", SingleChild(lhsPD), Seq(Details("(x)-[r:R]->(y)")), Set("a", "r", "x", "y")))
  }

  test("Optional") {
    assertGood(attach(Optional(lhsLP, Set("a")), 113.0),
      PlanDescriptionImpl(id, "Optional", SingleChild(lhsPD), Seq(Details("a")), Set("a")))
  }

  test("Anti") {
    assertGood(attach(Anti(lhsLP), 113.0),
      PlanDescriptionImpl(id, "Anti", SingleChild(lhsPD), Seq.empty, Set("a")))
  }

  test("ProjectEndpoints") {
    assertGood(attach(ProjectEndpoints(lhsLP, "r", "start", startInScope = true, "end", endInScope = true, None, directed = true, VarPatternLength(1, Some(1))), 234.2),
      PlanDescriptionImpl(id, "ProjectEndpoints", SingleChild(lhsPD), Seq(Details("(start)-[r]->(end)")), Set("a", "start", "r", "end")))

    assertGood(attach(ProjectEndpoints(lhsLP, "r", "start", startInScope = true, "end", endInScope = true, Some(Seq(RelTypeName("R")(pos))), directed = false, VarPatternLength(1, Some(3))), 234.2),
      PlanDescriptionImpl(id, "ProjectEndpoints(BOTH)", SingleChild(lhsPD), Seq(Details("(start)-[r:R*..3]-(end)")), Set("a", "start", "r", "end")))
  }

  test("VarExpand") {
    val predicate = (varName: String) => Equals(prop(varName, "prop"), number("32"))(pos)
    val nodePredicate = VariablePredicate(varFor("x"), predicate("x"))
    val relationshipPredicate = VariablePredicate(varFor("r"), predicate("r"))

    // -- PruningVarExpand --

    // With nodePredicate and relationshipPredicate
    assertGood(attach(PruningVarExpand(lhsLP, "a", SemanticDirection.OUTGOING, Seq(RelTypeName("R")(pos)), "y", 1, 4, Some(nodePredicate), Some(relationshipPredicate)), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(Pruning)", SingleChild(lhsPD), Seq(Details("(a)-[r:R*..4]->(y) WHERE x.prop = 32 AND r.prop = 32"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "y")))

    // With nodePredicate, without relationshipPredicate
    assertGood(attach(PruningVarExpand(lhsLP, "a", SemanticDirection.OUTGOING, Seq(RelTypeName("R")(pos)), "y", 2, 4, Some(nodePredicate), None), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(Pruning)", SingleChild(lhsPD), Seq(Details("(a)-[:R*2..4]->(y) WHERE x.prop = 32"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "y")))

    // Without predicates, without relationship type
    assertGood(attach(PruningVarExpand(lhsLP, "a", SemanticDirection.OUTGOING, Seq(), "y", 2, 4, None, None), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(Pruning)", SingleChild(lhsPD), Seq(Details("(a)-[*2..4]->(y)"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "y")))

    // -- VarExpand --

    // With unnamed variables, without predicates
    assertGood(attach(VarExpand(lhsLP, "a", INCOMING, INCOMING, Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), "  UNNAMED123", "  UNNAMED99", VarPatternLength(1, Some(1)), ExpandAll), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[:LIKES|LOVES]-()"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "  UNNAMED99", "  UNNAMED123")))

    // With nodePredicate and relationshipPredicate
    assertGood(attach(VarExpand(lhsLP, "a", INCOMING, INCOMING, Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), "to", "rel", VarPatternLength(1, Some(1)), ExpandAll, Some(nodePredicate), Some(relationshipPredicate)), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[rel:LIKES|LOVES]-(to) WHERE x.prop = 32 AND r.prop = 32"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "to", "rel")))

    // With nodePredicate, without relationshipPredicate, with length
    assertGood(attach(VarExpand(lhsLP, "a", INCOMING, INCOMING, Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), "to", "rel", VarPatternLength(2, Some(3)), ExpandAll, Some(nodePredicate)), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(All)", SingleChild(lhsPD), Seq(Details("(a)<-[rel:LIKES|LOVES*2..3]-(to) WHERE x.prop = 32"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "to", "rel")))

    // With unbounded length
    assertGood(attach(VarExpand(lhsLP, "a", OUTGOING, OUTGOING, Seq(RelTypeName("LIKES")(pos), RelTypeName("LOVES")(pos)), "to", "rel", VarPatternLength(2, None), ExpandAll), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "VarLengthExpand(All)", SingleChild(lhsPD), Seq(Details("(a)-[rel:LIKES|LOVES*2..]->(to)"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "to", "rel")))
  }

  test("Updates") {
    // RemoveLabels
    assertGood(attach(RemoveLabels(lhsLP, "x", Seq(label("L1"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "RemoveLabels", SingleChild(lhsPD), Seq(Details("x:L1"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(RemoveLabels(lhsLP, "x", Seq(label("L1"), label("L2"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "RemoveLabels", SingleChild(lhsPD), Seq(Details("x:L1:L2"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    // SetLabels
    assertGood(attach(SetLabels(lhsLP, "x", Seq(label("L1"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetLabels", SingleChild(lhsPD), Seq(Details("x:L1"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetLabels(lhsLP, "x", Seq(label("L1"), label("L2"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetLabels", SingleChild(lhsPD), Seq(Details("x:L1:L2"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    val map = MapExpression(Seq(
      (key("foo"), number("1")),
      (key("bar"), number("2"))
    ))(pos)
    val prettifiedMapExpr = "{foo: 1, bar: 2}"

    // Set From Map
    assertGood(attach(SetNodePropertiesFromMap(lhsLP, "x", map, removeOtherProps = true), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetNodePropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x = $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetNodePropertiesFromMap(lhsLP, "x", map, removeOtherProps = false), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetNodePropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x += $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetRelationshipPropertiesFromMap(lhsLP, "x", map, removeOtherProps = true), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetRelationshipPropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x = $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetRelationshipPropertiesFromMap(lhsLP, "x", map, removeOtherProps = false), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetRelationshipPropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x += $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetPropertiesFromMap(lhsLP, varFor("x"), map, removeOtherProps = true), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetPropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x = $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    assertGood(attach(SetPropertiesFromMap(lhsLP, varFor("x"), map, removeOtherProps = false), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetPropertiesFromMap", SingleChild(lhsPD), Seq(Details(s"x += $prettifiedMapExpr"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    // Set
    assertGood(attach(SetProperty(lhsLP, varFor("x"), key("prop"), number("1")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetProperty", SingleChild(lhsPD), Seq(Details("x.prop = 1"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    assertGood(attach(SetNodeProperty(lhsLP, "x", key("prop"), number("1")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetProperty", SingleChild(lhsPD), Seq(Details("x.prop = 1"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))

    assertGood(attach(SetRelationshipProperty(lhsLP, "x", key("prop"), number("1")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "SetProperty", SingleChild(lhsPD), Seq(Details("x.prop = 1"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))
  }

  test("Sort") {
    // Sort
    assertGood(attach(Sort(lhsLP, Seq(Ascending("a"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "Sort", SingleChild(lhsPD), Seq(Details("a ASC"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    assertGood(attach(Sort(lhsLP, Seq(Descending("a"), Ascending("y"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "Sort", SingleChild(lhsPD), Seq(Details("a DESC, y ASC"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    // Top
    assertGood(attach(Top(lhsLP, Seq(Ascending("a")), number("3")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "Top", SingleChild(lhsPD), Seq(Details("a ASC LIMIT 3"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    assertGood(attach(Top(lhsLP, Seq(Descending("a"), Ascending("y")), number("3")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "Top", SingleChild(lhsPD), Seq(Details("a DESC, y ASC LIMIT 3"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    // Partial Sort
    assertGood(attach(PartialSort(lhsLP, Seq(Ascending("a")), Seq(Descending("y"))), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "PartialSort", SingleChild(lhsPD), Seq(Details("a ASC, y DESC"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))

    // Partial Top
    assertGood(attach(PartialTop(lhsLP, Seq(Ascending("a")), Seq(Descending("y")), number("3")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "PartialTop", SingleChild(lhsPD), Seq(Details("a ASC, y DESC LIMIT 3"), Order(ProvidedOrder.asc(varFor("a")))), Set("a")))
  }

  test("Unwind") {
    assertGood(attach(UnwindCollection(lhsLP, "x", varFor("list")), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "Unwind", SingleChild(lhsPD), Seq(Details("list AS x"), Order(ProvidedOrder.asc(varFor("a")))), Set("a", "x")))
  }

  test("Admin") {
    assertGood(attach(ShowUsers(privLhsLP), 1.0, ProvidedOrder.asc(varFor("a"))),
      PlanDescriptionImpl(id, "ShowUsers", SingleChild(privLhsPD), Seq(Order(ProvidedOrder.asc(varFor("a")))), Set.empty))

    assertGood(attach(CreateUser(privLhsLP, util.Left("name"), varFor("password"), requirePasswordChange = false, suspended = None), 1.0),
      PlanDescriptionImpl(id, "CreateUser", SingleChild(privLhsPD), Seq(Details("USER name")), Set.empty))

    assertGood(attach(DropUser(privLhsLP, util.Left("name")), 1.0),
      PlanDescriptionImpl(id, "DropUser", SingleChild(privLhsPD), Seq(Details("USER name")), Set.empty))

    assertGood(attach(AlterUser(privLhsLP, util.Left("name"), None, requirePasswordChange = Some(true), suspended = Some(false)), 1.0),
      PlanDescriptionImpl(id, "AlterUser", SingleChild(privLhsPD), Seq(Details("USER name")), Set.empty))

    assertGood(attach(ShowRoles(privLhsLP, withUsers = false, showAll = true), 1.0),
      PlanDescriptionImpl(id, "ShowRoles", SingleChild(privLhsPD), Seq.empty, Set.empty))

    assertGood(attach(DropRole(privLhsLP, util.Left("role")), 1.0),
      PlanDescriptionImpl(id, "DropRole", SingleChild(privLhsPD), Seq(Details("ROLE role")), Set.empty))

    assertGood(attach(CreateRole(privLhsLP, util.Left("role")), 1.0),
      PlanDescriptionImpl(id, "CreateRole", SingleChild(privLhsPD), Seq(Details("ROLE role")), Set.empty))

    assertGood(attach(RequireRole(privLhsLP, util.Left("role")), 1.0),
      PlanDescriptionImpl(id, "RequireRole", SingleChild(privLhsPD), Seq(Details("ROLE role")), Set.empty))

    assertGood(attach(CopyRolePrivileges(privLhsLP, util.Left("role1"), util.Left("role2"), grantDeny = "DENIED"), 1.0),
      PlanDescriptionImpl(id, "CopyRolePrivileges(DENIED)", SingleChild(privLhsPD), Seq(Details("FROM ROLE role2 TO ROLE role1")), Set.empty))

    assertGood(attach(CopyRolePrivileges(privLhsLP, util.Left("role1"), util.Left("role2"), grantDeny = "GRANTED"), 1.0),
      PlanDescriptionImpl(id, "CopyRolePrivileges(GRANTED)", SingleChild(privLhsPD), Seq(Details("FROM ROLE role2 TO ROLE role1")), Set.empty))

    assertGood(attach(GrantRoleToUser(privLhsLP, util.Left("role"), util.Left("user")), 1.0),
      PlanDescriptionImpl(id, "GrantRoleToUser", SingleChild(privLhsPD), Seq(Details(Seq("ROLE role", "USER user"))), Set.empty))

    assertGood(attach(RevokeRoleFromUser(privLhsLP, util.Left("role"), util.Left("user")), 1.0),
      PlanDescriptionImpl(id, "RevokeRoleFromUser", SingleChild(privLhsPD), Seq(Details(Seq("ROLE role", "USER user"))), Set.empty))

    assertGood(attach(GrantDbmsAction(privLhsLP, CreateUserAction, util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "GrantDbmsAction", SingleChild(privLhsPD), Seq(Details(Seq("CREATE USER", "ROLE role1"))), Set.empty))

    assertGood(attach(DenyDbmsAction(privLhsLP, CreateUserAction, util.Left("user")), 1.0),
      PlanDescriptionImpl(id, "DenyDbmsAction", SingleChild(privLhsPD), Seq(Details(Seq("CREATE USER", "ROLE user"))), Set.empty))

    assertGood(attach(RevokeDbmsAction(privLhsLP, CreateUserAction, util.Left("role1"), "revokeType"), 1.0),
      PlanDescriptionImpl(id, "RevokeDbmsAction(revokeType)", SingleChild(privLhsPD), Seq(Details(Seq("CREATE USER", "ROLE role1"))), Set.empty))

    assertGood(attach(GrantDatabaseAction(privLhsLP, CreateNodeLabelAction, ast.NamedGraphScope(util.Left("foo"))(pos), UserAllQualifier()(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "GrantDatabaseAction", SingleChild(privLhsPD), Seq(Details(Seq("CREATE NEW NODE LABEL", "DATABASE foo", "ALL USERS", "ROLE role1"))), Set.empty))

    assertGood(attach(DenyDatabaseAction(privLhsLP, CreateNodeLabelAction, ast.AllGraphsScope()(pos), UserQualifier(util.Left("user1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "DenyDatabaseAction", SingleChild(privLhsPD), Seq(Details(Seq("CREATE NEW NODE LABEL", "ALL DATABASES", "USER user1", "ROLE role1"))), Set.empty))

    assertGood(attach(RevokeDatabaseAction(privLhsLP, CreateNodeLabelAction, ast.AllGraphsScope()(pos), UserQualifier(util.Left("user1"))(pos), util.Left("role1"), "revokeType"), 1.0),
      PlanDescriptionImpl(id, "RevokeDatabaseAction(revokeType)", SingleChild(privLhsPD), Seq(Details(Seq("CREATE NEW NODE LABEL", "ALL DATABASES", "USER user1", "ROLE role1"))), Set.empty))

    assertGood(attach(GrantTraverse(privLhsLP, ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "GrantTraverse", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(DenyTraverse(privLhsLP, ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "DenyTraverse", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(RevokeTraverse(privLhsLP, ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1"), "revokeType"), 1.0),
      PlanDescriptionImpl(id, "RevokeTraverse(revokeType)", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(GrantRead(privLhsLP, PropertyResource("prop")(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "GrantRead", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "PROPERTY prop", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(DenyRead(privLhsLP, AllResource()(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "DenyRead", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "ALL PROPERTIES", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(RevokeRead(privLhsLP, PropertyResource("prop")(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1"), "revokeType"), 1.0),
      PlanDescriptionImpl(id, "RevokeRead(revokeType)", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "PROPERTY prop", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(GrantMatch(privLhsLP, PropertyResource("prop")(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "GrantMatch", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "PROPERTY prop", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(DenyMatch(privLhsLP, PropertyResource("prop")(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1")), 1.0),
      PlanDescriptionImpl(id, "DenyMatch", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "PROPERTY prop", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(RevokeMatch(privLhsLP, PropertyResource("prop")(pos), ast.AllGraphsScope()(pos), LabelsQualifier(Seq("Label1"))(pos), util.Left("role1"), "revokeType"), 1.0),
      PlanDescriptionImpl(id, "RevokeMatch(revokeType)", SingleChild(privLhsPD), Seq(Details(Seq("ALL GRAPHS", "PROPERTY prop", "NODES Label1", "ROLE role1"))), Set.empty))

    assertGood(attach(ShowPrivileges(Some(privLhsLP), ShowRolePrivileges(util.Left("role1"))(pos)), 1.0),
      PlanDescriptionImpl(id, "ShowPrivileges", SingleChild(privLhsPD), Seq(Details("ROLE role1")), Set.empty))

    assertGood(attach(CreateDatabase(privLhsLP, util.Left("db1")), 1.0),
      PlanDescriptionImpl(id, "CreateDatabase", SingleChild(privLhsPD), Seq(Details("db1")), Set.empty))

    assertGood(attach(DropDatabase(privLhsLP, util.Left("db1")), 1.0),
      PlanDescriptionImpl(id, "DropDatabase", SingleChild(privLhsPD), Seq(Details("db1")), Set.empty))

    assertGood(attach(StartDatabase(privLhsLP, util.Left("db1")), 1.0),
      PlanDescriptionImpl(id, "StartDatabase", SingleChild(privLhsPD), Seq(Details("db1")), Set.empty))

    assertGood(attach(StopDatabase(privLhsLP, util.Left("db1")), 1.0),
      PlanDescriptionImpl(id, "StopDatabase", SingleChild(privLhsPD), Seq(Details("db1")), Set.empty))

    assertGood(attach(EnsureValidNonSystemDatabase(privLhsLP, util.Left("db1"), "action1"), 1.0),
      PlanDescriptionImpl(id, "EnsureValidNonSystemDatabase", SingleChild(privLhsPD), Seq(Details("db1")), Set.empty))

    val expectedPD = PlanDescriptionImpl(id, "CreateDatabase", NoChildren, Seq(Details("db1")), Set.empty)
    assertGood(attach(EnsureValidNumberOfDatabases(CreateDatabase(privLhsLP, util.Left("db1"))), 1.0),
      PlanDescriptionImpl(id, "EnsureValidNumberOfDatabases", SingleChild(expectedPD), Seq.empty, Set.empty))

    assertGood(attach(LogSystemCommand(privLhsLP, "command1"), 1.0),
      PlanDescriptionImpl(id, "LogSystemCommand", SingleChild(privLhsPD), Seq.empty, Set.empty))

    assertGood(attach(DoNothingIfNotExists(privLhsLP, "User", util.Left("user1")), 1.0),
      PlanDescriptionImpl(id, "DoNothingIfNotExists(User)", SingleChild(privLhsPD), Seq(Details("USER user1")), Set.empty))

    assertGood(attach(DoNothingIfExists(privLhsLP, "User", util.Left("user1")), 1.0),
      PlanDescriptionImpl(id, "DoNothingIfExists(User)", SingleChild(privLhsPD), Seq(Details("USER user1")), Set.empty))

    assertGood(attach(AssertNotCurrentUser(privLhsLP, util.Left("user1"), "verb1", "validation message"), 1.0),
      PlanDescriptionImpl(id, "AssertNotCurrentUser", SingleChild(privLhsPD), Seq(Details("USER user1")), Set.empty))
  }

  test("AntiConditionalApply") {
    assertGood(attach(AntiConditionalApply(lhsLP, rhsLP, Seq("c")), 2345.0),
      PlanDescriptionImpl(id, "AntiConditionalApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "b", "c")))
  }

  test("AntiSemiApply") {
    assertGood(attach(AntiSemiApply(lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "AntiSemiApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a")))
  }

  test("ConditionalApply") {
    assertGood(attach(ConditionalApply(lhsLP, rhsLP, Seq("c")), 2345.0),
      PlanDescriptionImpl(id, "ConditionalApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "b", "c")))
  }

  test("Apply") {
    assertGood(attach(Apply(lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "Apply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "b")))
  }

  test("AssertSameNode") {
    assertGood(attach(AssertSameNode("n", lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "AssertSameNode", TwoChildren(lhsPD, rhsPD), Seq(Details(Seq("n"))), Set("a", "b", "n")))
  }

  test("CartesianProduct") {
    assertGood(attach(CartesianProduct(lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "CartesianProduct", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "b")))
  }

  test("NodeHashJoin") {
    assertGood(attach(NodeHashJoin(Set("a"), lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "NodeHashJoin", TwoChildren(lhsPD, rhsPD), Seq(Details("a")), Set("a", "b")))
  }

  test("ForeachApply") {
    assertGood(attach(ForeachApply(lhsLP, rhsLP, "x", ListLiteral(Seq(number("1"), number("2")))(pos)), 2345.0),
      PlanDescriptionImpl(id, "Foreach", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a")))
  }

  test("LetSelectOrSemiApply") {
    assertGood(attach(LetSelectOrSemiApply(lhsLP, rhsLP, "x", Equals(prop("a", "foo"), number("42"))(pos)), 2345.0),
      PlanDescriptionImpl(id, "LetSelectOrSemiApply", TwoChildren(lhsPD, rhsPD), Seq(Details("a.foo = 42")), Set("a", "x")))
  }

  test("LetSelectOrAntiSemiApply") {
    assertGood(attach(LetSelectOrAntiSemiApply(lhsLP, rhsLP, "x", Equals(prop("a", "foo"), number("42"))(pos)), 2345.0),
      PlanDescriptionImpl(id, "LetSelectOrAntiSemiApply", TwoChildren(lhsPD, rhsPD), Seq(Details("a.foo = 42")), Set("a", "x")))
  }

  test("LetSemiApply") {
    assertGood(attach(LetSemiApply(lhsLP, rhsLP, "x"), 2345.0),
      PlanDescriptionImpl(id, "LetSemiApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "x")))
  }

  test("LetAntiSemiApply") {
    assertGood(attach(LetAntiSemiApply(lhsLP, rhsLP, "x"), 2345.0),
      PlanDescriptionImpl(id, "LetAntiSemiApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a", "x")))
  }

  test("LeftOuterHashJoin") {
    assertGood(attach(LeftOuterHashJoin(Set("a"), lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "NodeLeftOuterHashJoin", TwoChildren(lhsPD, rhsPD), Seq(Details("a")), Set("a", "b")))
  }

  test("RightOuterHashJoin") {
    assertGood(attach(RightOuterHashJoin(Set("a"), lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "NodeRightOuterHashJoin", TwoChildren(lhsPD, rhsPD), Seq(Details("a")), Set("a", "b")))
  }

  test("RollUpApply") {
    assertGood(attach(RollUpApply(lhsLP, rhsLP, "collection", "x", Set("y")), 2345.0),
      PlanDescriptionImpl(id, "RollUpApply", TwoChildren(lhsPD, rhsPD), Seq(Details(Seq("collection", "x"))), Set("a", "collection")))
  }

  test("SelectOrAntiSemiApply") {
    assertGood(attach(SelectOrAntiSemiApply(lhsLP, rhsLP, Equals(prop("a", "foo"), number("42"))(pos)), 2345.0),
      PlanDescriptionImpl(id, "SelectOrAntiSemiApply", TwoChildren(lhsPD, rhsPD), Seq(Details("a.foo = 42")), Set("a")))
  }

  test("SelectOrSemiApply") {
    assertGood(attach(SelectOrSemiApply(lhsLP, rhsLP, Equals(prop("a", "foo"), number("42"))(pos)), 2345.0),
      PlanDescriptionImpl(id, "SelectOrSemiApply", TwoChildren(lhsPD, rhsPD), Seq(Details("a.foo = 42")), Set("a")))
  }

  test("SemiApply") {
    assertGood(attach(SemiApply(lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "SemiApply", TwoChildren(lhsPD, rhsPD), Seq.empty, Set("a")))
  }

  test("TriadicSelection") {
    assertGood(attach(TriadicSelection(lhsLP, rhsLP, positivePredicate = true, "a", "b", "c"), 2345.0),
      PlanDescriptionImpl(id, "TriadicSelection", TwoChildren(lhsPD, rhsPD), Seq(Details("WHERE (a)--(c)")), Set("a", "b")))

    assertGood(attach(TriadicSelection(lhsLP, rhsLP, positivePredicate = false, "a", "b", "c"), 2345.0),
      PlanDescriptionImpl(id, "TriadicSelection", TwoChildren(lhsPD, rhsPD), Seq(Details("WHERE NOT (a)--(c)")), Set("a", "b")))
  }

  test("Union") {
    // leafs no overlapping variables
    assertGood(attach(Union(lhsLP, rhsLP), 2345.0),
      PlanDescriptionImpl(id, "Union", TwoChildren(lhsPD, rhsPD), Seq.empty, Set.empty))

    // leafs with overlapping variables
    val lp = attach(AllNodesScan("a", Set.empty), 2.0, ProvidedOrder.empty)
    val pd = PlanDescriptionImpl(id, "AllNodesScan", NoChildren, Seq(Details("a")), Set("a"))
    assertGood(attach(Union(lhsLP, lp), 2345.0),
      PlanDescriptionImpl(id, "Union", TwoChildren(lhsPD, pd), Seq.empty, Set("a")))
  }

  test("ValueHashJoin") {
    assertGood(attach(ValueHashJoin(lhsLP, rhsLP, Equals(prop("a", "foo"), prop("b", "foo"))(pos)), 2345.0),
      PlanDescriptionImpl(id, "ValueHashJoin", TwoChildren(lhsPD, rhsPD), Seq(Details("a.foo = b.foo")), Set("a", "b")))
  }

  def assertGood(logicalPlan: LogicalPlan, expectedPlanDescription: InternalPlanDescription, validateAllArgs: Boolean = false): Unit = {
    val producedPlanDescription = LogicalPlan2PlanDescription(logicalPlan, IDPPlannerName, CypherVersion.default, readOnly, cardinalities, providedOrders, StubExecutionPlan())

    def shouldValidateArg(arg: Argument) =
      validateAllArgs ||
        !(arg.isInstanceOf[PlannerImpl] ||
          arg.isInstanceOf[Planner] ||
          arg.isInstanceOf[EstimatedRows] ||
          arg.isInstanceOf[Version] ||
          arg.isInstanceOf[RuntimeVersion] ||
          arg.isInstanceOf[PlannerVersion])

    def shouldBeEqual(a: InternalPlanDescription, b: InternalPlanDescription): Unit = {
      withClue("name")(a.name should equal(b.name))
      if (validateAllArgs) {
        withClue("arguments(all)")(a.arguments should equal(b.arguments))
      } else {
        val aArgsToValidate = a.arguments.filter(shouldValidateArg)
        val bArgsToValidate = b.arguments.filter(shouldValidateArg)

        withClue("arguments")(aArgsToValidate should equal(bArgsToValidate))
      }
      withClue("variables")(a.variables should equal(b.variables))
    }

    shouldBeEqual(producedPlanDescription, expectedPlanDescription)

    withClue("children") {
      (expectedPlanDescription.children, producedPlanDescription.children) match {
        case (NoChildren, NoChildren) =>
        case (SingleChild(expectedChild), SingleChild(producedChild)) =>
          shouldBeEqual(expectedChild, producedChild)
        case (TwoChildren(expectedLhs, expectedRhs), TwoChildren(producedLhs, producedRhs)) =>
          shouldBeEqual(expectedLhs, producedLhs)
          shouldBeEqual(expectedRhs, producedRhs)
        case (expected, produced) =>
          fail(s"${expected.getClass} does not equal ${produced.getClass}")
      }
    }
  }

  private def varFor(name: String): Variable = Variable(name)(pos)

  private def prop(varName: String, propName: String): Property = Property(varFor(varName), PropertyKeyName(propName)(pos))(pos)

  private def cachedProp(varName: String, propName: String): CachedProperty = CachedProperty(varName, varFor(varName), PropertyKeyName(propName)(pos), NODE_TYPE)(pos)

  private def label(name: String): LabelName = LabelName(name)(pos)

  private def key(name: String): PropertyKeyName = PropertyKeyName(name)(pos)

  private def number(i: String): SignedDecimalIntegerLiteral = SignedDecimalIntegerLiteral(i)(pos)
}
