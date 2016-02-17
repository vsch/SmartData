/*
 * Copyright (c) 2015-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vladsch.smart

import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartDataScopeTest {
    val manager = SmartDataScopeManager.INSTANCE

    @Test
    fun test_childScopes() {
        val scope = manager.createDataScope("top")
        val child1 = scope.createDataScope("child1")
        val child2 = scope.createDataScope("child2")
        val grandChild21 = child2.createDataScope("grandChild21")

        assertEquals(2, scope.children.size)
        assertEquals(1, scope.descendants.size)
        assertEquals(1, child2.children.size)
    }

    @Test
    fun test_DefaultBasic() {
        val INDENT = SmartVolatileDataKey("INDENT", 0)
        val scope = manager.createDataScope("top")

        val myIndent = scope.dataPoint(INDENT, 0)
        val myIndent2 = scope.dataPoint(INDENT, 2)

        assertEquals(1, scope.consumers.size)
        assertTrue(scope.consumers.containsKey(INDENT))
        assertTrue(scope.consumers[INDENT]?.contains(0) ?: false)
        assertTrue(scope.consumers[INDENT]?.contains(2) ?: false)
        assertFalse(scope.consumers[INDENT]?.contains(1) ?: false)
        assertFalse(scope.consumers[INDENT]?.contains(3) ?: false)

        assertEquals(0, myIndent.value)
        assertEquals(0, myIndent2.value)
    }

    @Test
    fun test_Basic_VolatileInverted() {
        val INDENT = SmartVolatileDataKey("INDENT", 0)
        val scope = manager.createDataScope("top")

        val indent = SmartVolatileData(0)
        val myIndent = scope.dataPoint(INDENT, 0)
        val myIndent2 = scope.dataPoint(INDENT, 2)
        scope.setValue(INDENT, 0, indent)

        assertEquals(1, scope.consumers.size)
        assertTrue(scope.consumers.containsKey(INDENT))
        assertTrue(scope.consumers[INDENT]?.contains(0) ?: false)
        assertTrue(scope.consumers[INDENT]?.contains(2) ?: false)
        assertFalse(scope.consumers[INDENT]?.contains(1) ?: false)
        assertFalse(scope.consumers[INDENT]?.contains(3) ?: false)

        assertEquals(0, indent.value)
        assertEquals(0, myIndent.value)

        indent.value = 1
        assertEquals(1, myIndent.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(10, myIndent.value)
        assertEquals(10, indent.value)

    }

    @Test
    fun test_Basic_Volatile() {
        val INDENT = SmartVolatileDataKey("INDENT", 0)
        val scope = manager.createDataScope("top")

        val indent = SmartVolatileData(0)
        scope.setValue(INDENT, 0, indent)
        val myIndent = scope.dataPoint(INDENT, 0)

        assertEquals(0, indent.value)
        assertEquals(0, myIndent.value)

        indent.value = 1
        assertEquals(1, myIndent.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(10, myIndent.value)
        assertEquals(10, indent.value)

    }

    @Test
    fun test_ParentVolatile_Default() {
        val INDENT = SmartVolatileDataKey("INDENT", 0)
        val scope = manager.createDataScope("top")
        val child = scope.createDataScope("child")

        val indent = SmartVolatileData(0)
        scope.setValue(INDENT, 0, indent)
        val myIndent = child.dataPoint(INDENT, 0)

        assertEquals(0, scope.consumers.size)
        assertEquals(1, child.consumers.size)

        assertTrue(child.consumers.containsKey(INDENT))
        assertTrue(child.consumers[INDENT]?.contains(0) ?: false)
        assertFalse(child.consumers[INDENT]?.contains(1) ?: false)
        assertFalse(child.consumers[INDENT]?.contains(3) ?: false)

        assertEquals(0, indent.value)
        assertEquals(0, myIndent.value)

        indent.value = 1
        assertEquals(0, myIndent.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(0, myIndent.value)
        assertEquals(10, indent.value)

        scope.finalizeAllScopes()

        indent.value = 1
        assertEquals(1, myIndent.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(10, myIndent.value)
        assertEquals(10, indent.value)
    }

    @Test
    fun test_ParentVolatile_Child() {
        val INDENT = SmartVolatileDataKey("INDENT", 0)
        val scope = manager.createDataScope("top")
        val child = scope.createDataScope("child")

        val indent = SmartVolatileData(0)
        val childIndent = SmartVolatileData(0)
        scope.setValue(INDENT, 0, indent)
        child.setValue(INDENT, 0, childIndent)
        val myIndent = child.dataPoint(INDENT, 0)

        assertEquals(0, indent.value)
        assertEquals(0, childIndent.value)
        assertEquals(0, myIndent.value)

        indent.value = 1
        assertEquals(0, myIndent.value)
        assertEquals(0, childIndent.value)
        assertEquals(1, indent.value)

        childIndent.value = 10
        assertEquals(10, myIndent.value)
        assertEquals(10, childIndent.value)
        assertEquals(1, indent.value)
    }

    @Test
    fun test_ParentComputed_Basic() {
        val INDENT = SmartParentComputedDataKey("INDENT", 0, { it + 4 })
        val scope = manager.createDataScope("top")
        val child2 = scope.createDataScope("child2")
        val grandChild21 = child2.createDataScope("grandChild21")

        val indent = SmartVolatileData("indent", 0)
        scope.setValue(INDENT, 0, indent)
        val myIndent2 = child2.dataPoint(INDENT, 0)
        val myIndent21 = grandChild21.dataPoint(INDENT, 0)

        val parentValue = scope.getValue(INDENT, 0)

        scope.finalizeAllScopes()

        assertEquals(indent, parentValue)
        assertTrue(child2.getRawValue(INDENT, 0) is SmartVersionedDataAlias<*>)
        assertTrue(grandChild21.getRawValue(INDENT, 0) is SmartVersionedDataAlias<*>)
        assertEquals(indent, (myIndent2 as SmartVersionedDataAlias<*>).alias.dependencies.first())
        assertEquals(myIndent2.alias, (myIndent21 as SmartVersionedDataAlias<*>).alias.dependencies.first())

        assertEquals(0, indent.value)
        assertEquals(4, myIndent2.value)
        assertEquals(8, myIndent21.value)

        indent.value = 1
        assertEquals(5, myIndent2.value)
        assertEquals(9, myIndent21.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(14, myIndent2.value)
        assertEquals(18, myIndent21.value)
        assertEquals(10, indent.value)
    }

    @Test
    fun test_ParentComputed_Complex() {
        val INDENT = SmartParentComputedDataKey("INDENT", 0, { it + 4 })
        val scope = SmartDataScopeManager.createDataScope("top")
        val child1 = scope.createDataScope("child1")
        val child2 = scope.createDataScope("child2")
        val grandChild11 = child1.createDataScope("grandChild11")
        val grandChild21 = child2.createDataScope("grandChild21")

        val indent = SmartVolatileData("indent", 0)
        scope.setValue(INDENT, 0, indent)
        child1.setValue(INDENT, 0, indent)

        val myIndent1 = child1.dataPoint(INDENT, 0)
        val myIndent2 = child2.dataPoint(INDENT, 0)
        val myIndent11 = grandChild11.dataPoint(INDENT, 0)
        val myIndent21 = grandChild21.dataPoint(INDENT, 0)

        val parentValue = scope.getValue(INDENT, 0)

        scope.finalizeAllScopes()

        assertEquals(indent, parentValue)
        assertEquals(indent, myIndent1)
        assertEquals(indent, (myIndent11 as SmartVersionedDataAlias<*>).alias.dependencies.first())
        assertEquals(indent, (myIndent2 as SmartVersionedDataAlias<*>).alias.dependencies.first())
        assertEquals(myIndent2.alias, (myIndent21 as SmartVersionedDataAlias<*>).alias.dependencies.first())

        assertEquals(0, indent.value)
        assertEquals(0, myIndent1.value)
        assertEquals(4, myIndent2.value)
        assertEquals(4, myIndent11.value)
        assertEquals(8, myIndent21.value)

        indent.value = 1
        assertEquals(1, myIndent1.value)
        assertEquals(5, myIndent2.value)
        assertEquals(5, myIndent11.value)
        assertEquals(9, myIndent21.value)
        assertEquals(1, indent.value)

        indent.value = 10
        assertEquals(10, myIndent1.value)
        assertEquals(14, myIndent2.value)
        assertEquals(14, myIndent11.value)
        assertEquals(18, myIndent21.value)
        assertEquals(10, indent.value)
    }

    @Test
    fun test_AggregatedDataScopes() {
        val WIDTH = SmartVolatileDataKey("WIDTH", 0)
        val MAX_WIDTH = SmartAggregatedScopesDataKey("MAX_WIDTH", 0, WIDTH, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })

        val topScope = SmartDataScopeManager.createDataScope("top")
        val child1 = topScope.createDataScope("child1")
        val child2 = topScope.createDataScope("child2")
        val grandChild11 = child1.createDataScope("grandChild11")
        val grandChild21 = child2.createDataScope("grandChild21")

        WIDTH[child1, 0] = 10
        assertTrue(child1.getRawValue(WIDTH, 0) is SmartVolatileData)

        WIDTH[child2, 0] = 15
        assertTrue(child2.getRawValue(WIDTH, 0) is SmartVolatileData)

        WIDTH[grandChild11, 0] = 8
        assertTrue(grandChild11.getRawValue(WIDTH, 0) is SmartVolatileData)
        WIDTH[grandChild21, 0] = 20
        assertTrue(grandChild21.getRawValue(WIDTH, 0) is SmartVolatileData)

        val maxWidth = topScope[MAX_WIDTH, 0]
        assertTrue(maxWidth is SmartVersionedDataAlias)
        assertTrue(topScope.consumers.containsKey(MAX_WIDTH))

        topScope.finalizeAllScopes()

        assertEquals(20, maxWidth.value)

        WIDTH[grandChild11, 0] = 12
        WIDTH[grandChild21, 0] = 17

        assertEquals(17, maxWidth.value)

        WIDTH[grandChild21, 0] = 10
        assertEquals(15, maxWidth.value)
    }

    @Test
    fun test_SmartAggregatedDependencies() {
        val LENGTH = SmartVolatileDataKey("LENGTH", 0)
        val WIDTH = SmartVolatileDataKey("WIDTH", 0)
        val PERIMETER = SmartAggregatedDependenciesDataKey("PERIMETER", 0, listOf(WIDTH, LENGTH), false, IterableDataComputable { it.sum() * 2 })
        val PERIMETER_TOTAL = SmartVectorDataKey("PERIMETER_TOTAL", 0, listOf(PERIMETER), setOf(SmartScopes.RESULT_TOP, SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.sum() })

        val topScope = SmartDataScopeManager.createDataScope("top")
        val child1 = topScope.createDataScope("child1")
        val child2 = topScope.createDataScope("child2")
        val grandChild11 = child1.createDataScope("grandChild11")
        val grandChild21 = child2.createDataScope("grandChild21")

        WIDTH[child1, 0] = 10
        LENGTH[child1, 0] = 20
        assertTrue(child1.getRawValue(WIDTH, 0) is SmartVolatileData)
        assertTrue(child1.getRawValue(LENGTH, 0) is SmartVolatileData)

        WIDTH[child2, 0] = 15
        LENGTH[child2, 0] = 15
        assertTrue(child2.getRawValue(WIDTH, 0) is SmartVolatileData)
        assertTrue(child2.getRawValue(LENGTH, 0) is SmartVolatileData)

        WIDTH[grandChild11, 0] = 8
        LENGTH[grandChild11, 0] = 12
        assertTrue(grandChild11.getRawValue(WIDTH, 0) is SmartVolatileData)
        assertTrue(grandChild11.getRawValue(LENGTH, 0) is SmartVolatileData)

        WIDTH[grandChild21, 0] = 20
        LENGTH[grandChild21, 0] = 40
        assertTrue(grandChild21.getRawValue(WIDTH, 0) is SmartVolatileData)
        assertTrue(grandChild21.getRawValue(LENGTH, 0) is SmartVolatileData)

        val perimeterTotal = topScope[PERIMETER_TOTAL, 0]
        assertTrue(perimeterTotal is SmartVersionedDataAlias)
        assertTrue(topScope.consumers.containsKey(PERIMETER_TOTAL))

        manager.trace = true
        topScope.finalizeAllScopes()

        assertEquals(280, perimeterTotal.value)

        WIDTH[grandChild11, 0] = 12
        LENGTH[grandChild11, 0] = 28
        assertEquals(320, perimeterTotal.value)

        WIDTH[child2, 0] = 10
        assertEquals(310, perimeterTotal.value)
    }

    //    @Test
    //    fun test_DataKey_registerParentComputedBasic() {
    //        val INDENT2 = SmartParentComputedDataKey("INDENT2", 0, { it + 4 })
    //
    //        assertFalse(manager.dependentKeys.containsKey(INDENT2))
    //    }
    //
    //    @Test
    //    fun test_DataKey_resolveDependenciesBasic() {
    //        val INDENT = SmartParentComputedDataKey("INDENT", 0, { it + 4 })
    //        val MAX_INDENT = SmartAggregatedDataKey("MAX_INDENT", 0, INDENT, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), { it.max() ?: 0 })
    //        val ADD_INDENT = SmartTransformedDataKey("ADD_INDENT", 0, MAX_INDENT, SmartScopes.SELF, { it + 4 })
    //
    //        assertTrue(manager.dependentKeys.containsKey(INDENT))
    //        assertTrue(manager.dependentKeys.containsKey(MAX_INDENT))
    //        assertFalse(manager.dependentKeys.containsKey(ADD_INDENT))
    //
    //        manager.resolveDependencies()
    //
    //        assertEquals(0, manager.keyComputeLevel[INDENT])
    //        assertEquals(1, manager.keyComputeLevel[MAX_INDENT])
    //        assertEquals(2, manager.keyComputeLevel[ADD_INDENT])
    //    }

    @Test
    fun formatTable() {
        val COLUMN_WIDTH = SmartVolatileDataKey("COLUMN_WIDTH", 0)
        val MAX_COLUMN_WIDTH = SmartAggregatedScopesDataKey("MAX_COLUMN_WIDTH", 0, COLUMN_WIDTH, setOf(SmartScopes.RESULT_TOP, SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartLatestDataKey("COLUMN_ALIGNMENT", TextAlignment.LEFT, ALIGNMENT, setOf(SmartScopes.RESULT_TOP, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS))

        val tableDataScope = manager.createDataScope("tableDataScope")
        var formattedTable = EditableCharSequence()

        var maxColumnWidth0 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 0)
        var maxColumnWidth1 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 1)
        var maxColumnWidth2 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 2)
        var maxColumnWidth3 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 3)

        var columnAlignment0 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 0)
        var columnAlignment1 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 1)
        var columnAlignment2 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 2)
        var columnAlignment3 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 3)

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())
        var lastColumn: SmartVariableCharSequence? = null

        //        SmartVersionManager.groupedUpdate(Runnable {
        val tableRows = table.splitPartsSegmented('\n', false)

        var row = 0
        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            val rowDataScope = tableDataScope.createDataScope("row:$row")
            var col = 0
            for (column in line.splitPartsSegmented('|', false).segments) {
                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)
                lastColumn = formattedCol
                val discretionary = 1

                if (headerParts != null) {
                    val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                    val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                    formattedCol.leftPadChar = '-'
                    formattedCol.rightPadChar = '-'
                    when {
                        haveLeft && haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.CENTER
                            formattedCol.prefix = ":"
                            formattedCol.suffix = ":"
                        }
                        haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.RIGHT
                            formattedCol.suffix = ":"
                        }
                        else -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.LEFT
                            if (discretionary == 1 || discretionary == 0 && haveLeft) formattedCol.prefix = ":"
                        }
                    }
                }

                if (col > 0) formattedRow.append(" | ")
                formattedRow.append(formattedCol)
                rowDataScope[COLUMN_WIDTH, col] = formattedCol.lengthDataPoint
                formattedCol.alignmentDataPoint = COLUMN_ALIGNMENT.dataPoint(tableDataScope, col)
                formattedCol.widthDataPoint = MAX_COLUMN_WIDTH.dataPoint(tableDataScope, col)
                col++
            }

            formattedTable.append("| ", formattedRow, " |\n")
            row++
        }
        //        })

        println("currentSerial: ${SmartVersionManager.currentSerial}")
        val column = lastColumn
        if (column != null) println(column.widthDataPoint)

        tableDataScope.finalizeAllScopes()

        println("currentSerial: ${SmartVersionManager.currentSerial}")
        if (column != null) {
            println(column.widthDataPoint)
            println("lastColumnWidth.widthDataPoint.value = ${column.widthDataPoint.value}")
            println("lastColumnWidth.value = ${column.width}")
        }

        println("maxColumnWidth0: $maxColumnWidth0")
        println("maxColumnWidth1: $maxColumnWidth1")
        println("maxColumnWidth2: $maxColumnWidth2")
        println("maxColumnWidth3: $maxColumnWidth3")
        println("columnAlignment0: $columnAlignment0")
        println("columnAlignment1: $columnAlignment1")
        println("columnAlignment2: $columnAlignment2")
        println("columnAlignment3: $columnAlignment3")

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
| :---------------------------- | :-------------------- | :-------------------------: | --------------------: |
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSpanned() {
        val COLUMN_WIDTH = SmartVolatileDataKey("COLUMN_WIDTH", 0)
        val MAX_COLUMN_WIDTH = SmartAggregatedScopesDataKey("MAX_COLUMN_WIDTH", 0, COLUMN_WIDTH, SmartScopes.TOP_DOWN, IterableDataComputable { it.max() })

        val ADD_WIDTH = SmartVolatileDataKey("ADD_WIDTH", 0)
        val MAX_ADD_WIDTH = SmartAggregatedScopesDataKey("MAX_ADD_WIDTH", 0, ADD_WIDTH, SmartScopes.TOP_DOWN, IterableDataComputable { it.max() })
        val FINAL_COLUMN_WIDTH = SmartDependentDataKey("FINAL_COLUMN_WIDTH", 0, listOf(MAX_ADD_WIDTH, MAX_COLUMN_WIDTH), SmartScopes.SELF, DataValueComputable { MAX_ADD_WIDTH.value(it) + MAX_COLUMN_WIDTH.value(it) })

        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartLatestDataKey("COLUMN_ALIGNMENT", TextAlignment.LEFT, ALIGNMENT, SmartScopes.TOP_DOWN)

        //        manager.trace = true
        val tableDataScope = manager.createDataScope("tableDataScope")
        var formattedTable = EditableCharSequence()

        //        var maxColumnWidth0 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 0)
        //        var maxColumnWidth1 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 1)
        //        var maxColumnWidth2 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 2)
        //        var maxColumnWidth3 = tableDataScope.dataPoint(MAX_COLUMN_WIDTH, 3)
        //
        //        var columnAlignment0 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 0)
        //        var columnAlignment1 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 1)
        //        var columnAlignment2 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 2)
        //        var columnAlignment3 = tableDataScope.dataPoint(COLUMN_ALIGNMENT, 3)

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
Row 3 Col 0-1 Default Alignment||Row 3 Col 2 a lot more Data|Row 3 Col 3 Data
Row 4 Col 0 Default Alignment|Row 4 Col 1-2 More Data||Row 4 Col 3 Data
Row 5 Col 0 Default Alignment|Row 5 Col 1 More Data|Row 5 Col 2-3 a lot more Data||
Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data|||Row 6 Col 3 Data
Row 7 Col 0 Default Alignment|Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data|||
Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data||||
""".toCharArray())
        var lastColumn: SmartVariableCharSequence? = null

        val tableRows = table.splitPartsSegmented('\n', false)
        var dataPoint: SmartVersionedDataHolder<Int>

        var row = 0
        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            val rowDataScope = tableDataScope.createDataScope("row:$row")

            val segments = line.splitPartsSegmented('|', false).segments
            var col = 0
            var lastSpan = 1
            while (col < segments.size) {
                val column = segments[col]

                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)
                lastColumn = formattedCol
                val discretionary = 1

                if (headerParts != null) {
                    val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                    val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                    formattedCol.leftPadChar = '-'
                    formattedCol.rightPadChar = '-'
                    when {
                        haveLeft && haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.CENTER
                            formattedCol.prefix = ":"
                            formattedCol.suffix = ":"
                        }
                        haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.RIGHT
                            formattedCol.suffix = ":"
                        }
                        else -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.LEFT
                            if (discretionary == 1 || discretionary == 0 && haveLeft) formattedCol.prefix = ":"
                        }
                    }
                }

                // see if we have spanned columns
                var span = 1
                while (col + span <= segments.lastIndex && segments[col + span].isEmpty()) span++

                if (col > 0) formattedRow.append("|".repeat(lastSpan))
                if (span > 1) {
                    // here we do aggregation and distribution of column width
                    // MAX_COLUMN_WIDTH\                                                                / ADD_WIDTH
                    // MAX_COLUMN_WIDTH---> AGGR_MAX_WIDTH, (COLUMN_WIDTH - AGG_MAX_WIDTH) distribute >-- ADD_WIDTH
                    // MAX_COLUMN_WIDTH/                                                                \ ADD_WIDTH

                    val aggregator = SmartVersionedIntAggregatorDistributor("${rowDataScope.name}:col$col:aggregator:MAX_COLUMN_WIDTH", span, 0)
                    if (manager.trace) aggregator.trace = true

                    for (spanCol in 0..span - 1) {
                        dataPoint = MAX_COLUMN_WIDTH.dataPoint(tableDataScope, col + spanCol)
                        if (manager.trace) println("MAX_COLUMN_WIDTH dataPoint[${col + spanCol}] $dataPoint")

                        aggregator.get(spanCol).connect(dataPoint)
                    }

                    val columnLength = formattedCol.lengthDataPoint
                    val difference = SmartDependentData("${rowDataScope.name}:col${col}:difference", listOf(aggregator, columnLength), DataComputable {
                        val diff = (columnLength.value - aggregator.value).minBound(0)
                        if (manager.trace) println("${rowDataScope.name} col: $col span: $span diff: $diff")
                        diff
                    })

                    val distributor = SmartVersionedIntAggregatorDistributor("${rowDataScope.name}:col$col:distributor:ADD_WIDTH", span, 0)

                    if (manager.trace) {
                        distributor.trace = true
                        dataPoint = difference
                        println("difference dataPoint[${rowDataScope.name}:col${col}] $dataPoint")
                    }

                    //                    difference.value
                    distributor.connect(difference)
                    //                    dataPoint = distributor
                    //                    println("distributor dataPoint[${rowDataScope.name}:col${col}] $dataPoint")
                    //                    assertEquals(difference.value, distributor.value)
                    //                    distributor.value

                    //                    var aggr = 0
                    //                    for (spanCol in 0..span - 1) {
                    //                        aggr += distributor.get(spanCol).value
                    //                    }
                    //                    assertEquals(difference.value, aggr, "Distributed value ${difference.value} not equal to aggregated distributed values")

                    for (spanCol in 0..span - 1) {
                        dataPoint = distributor.get(spanCol)
                        if (manager.trace) println("DIST ADD_WIDTH dataPoint[${col + spanCol}] $dataPoint")
                        rowDataScope[ADD_WIDTH, col + spanCol] = dataPoint

                        if (manager.trace) {
                            println("DIST ADD_WIDTH value: ${dataPoint.value} dataPoint[${col + spanCol}] $dataPoint")

                            // lets consume MAX_ADD_WIDTH so it is computed
                            dataPoint = MAX_ADD_WIDTH.dataPoint(tableDataScope, col + spanCol)
                            println("MAX_ADD_WIDTH dataPoint[${col + spanCol}] $dataPoint")
                        }
                    }

                    // here we have to aggregate FINAL_COLUMN_WIDTH and feed it to the column data point
                    val finalAggregator = SmartVersionedIntAggregatorDistributor("${rowDataScope.name}:col$col:finalAggregator:FINAL_COLUMN_WIDTH", span, 0)
                    if (manager.trace) finalAggregator.trace = true

                    for (spanCol in 0..span - 1) {
                        finalAggregator.get(spanCol).connect(FINAL_COLUMN_WIDTH.dataPoint(tableDataScope, col + spanCol))
                    }

                    formattedCol.widthDataPoint = SmartDependentData(listOf(finalAggregator), DataComputable { finalAggregator.value })
                } else {
                    rowDataScope[COLUMN_WIDTH, col] = formattedCol.lengthDataPoint
                    formattedCol.widthDataPoint = FINAL_COLUMN_WIDTH.dataPoint(tableDataScope, col)
                }

                lastSpan = span
                formattedCol.alignmentDataPoint = COLUMN_ALIGNMENT.dataPoint(tableDataScope, col)

                formattedRow.append(formattedCol)
                col += span
            }

            // here if we add pipes then add lastSpan, else lastSpan-1
            formattedTable.append("|", formattedRow, "|".repeat(lastSpan), "\n")
            row++
        }

        val column = lastColumn
        if (manager.trace) {
            println("currentSerial: ${SmartVersionManager.currentSerial}")
            if (column != null) println(column.widthDataPoint)
        }

        tableDataScope.finalizeAllScopes()

        if (manager.trace) {
            for (col in 0..3) {
                var dataPoint = MAX_COLUMN_WIDTH.dataPoint(tableDataScope, col)
                println("MAX_COLUMN_WIDTH dataPoint[${col}] $dataPoint")

                dataPoint = MAX_ADD_WIDTH.dataPoint(tableDataScope, col)
                println("MAX_ADD_WIDTH dataPoint[${col}] $dataPoint")
            }

            println("currentSerial: ${SmartVersionManager.currentSerial}")
            if (column != null) {
                println(column.widthDataPoint)
                println("lastColumnWidth.widthDataPoint.value = ${column.widthDataPoint.value}")
                println("lastColumnWidth.value = ${column.width}")
            }
        }


        //        println("maxColumnWidth0: $maxColumnWidth0")
        //        println("maxColumnWidth1: $maxColumnWidth1")
        //        println("maxColumnWidth2: $maxColumnWidth2")
        //        println("maxColumnWidth3: $maxColumnWidth3")
        //        println("columnAlignment0: $columnAlignment0")
        //        println("columnAlignment1: $columnAlignment1")
        //        println("columnAlignment2: $columnAlignment2")
        //        println("columnAlignment3: $columnAlignment3")

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        assertEquals("""|Header 0                       |Header 1              |          Header 2          |             Header 3|
|:------------------------------|:---------------------|:--------------------------:|--------------------:|
|Row 1 Col 0 Data               |Row 1 Col 1 Data      |   Row 1 Col 2 More Data    |Row 1 Col 3 Much Data|
|Row 2 Col 0 Default Alignment  |Row 2 Col 1 More Data |Row 2 Col 2 a lot more Data |     Row 2 Col 3 Data|
|Row 3 Col 0-1 Default Alignment                      ||Row 3 Col 2 a lot more Data |     Row 3 Col 3 Data|
|Row 4 Col 0 Default Alignment  |Row 4 Col 1-2 More Data                           ||     Row 4 Col 3 Data|
|Row 5 Col 0 Default Alignment  |Row 5 Col 1 More Data |          Row 5 Col 2-3 a lot more Data          ||
|Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data|||     Row 6 Col 3 Data|
|Row 7 Col 0 Default Alignment  |Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data   |||
|Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data    ||||
""", formattedTable.toString())
    }

}
