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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartDataScopeTest {
    val manager = SmartDataScopeManager

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
        assertEquals(indent, (myIndent1 as SmartVersionedDataAlias<*>).alias)
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
        val MAX_WIDTH = SmartAggregatedDataKey("MAX_WIDTH", 0, WIDTH, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() } )

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
        val MAX_COLUMN_WIDTH = SmartAggregatedDataKey("MAX_COLUMN_WIDTH", 0, COLUMN_WIDTH, setOf(SmartScopes.TOP, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() } )
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartLatestDataKey("COLUMN_ALIGNMENT", TextAlignment.LEFT, ALIGNMENT, setOf(SmartScopes.TOP, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS))

        val tableDataScope = manager.createDataScope("tableDataScope")
        var formattedTable = EditableCharSequence()

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        SmartVersionManager.groupedUpdate(Runnable {
            val tableRows = table.splitPartsSegmented('\n', false)

            var row = 0
            for (line in tableRows.segments) {
                var formattedRow = EditableCharSequence()
                val rowDataScope = tableDataScope.createDataScope("row:$row")
                var col = 0
                for (column in line.splitPartsSegmented('|', false).segments) {
                    val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                    val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)
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
                    rowDataScope[COLUMN_WIDTH, col] = formattedCol.widthDataPoint
                    formattedCol.alignmentDataPoint = COLUMN_ALIGNMENT.dataPoint(tableDataScope, col)
                    formattedCol.widthDataPoint = MAX_COLUMN_WIDTH.dataPoint(tableDataScope, col)
                    col++
                }

                formattedTable.append("| ", formattedRow, " |\n")
                row++
            }
        })

        tableDataScope.finalizeAllScopes()

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")
    }

}
