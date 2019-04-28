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

class SmartSamples {
    @Test
    fun test_Sum() {
        val v1 = SmartVolatileData("v1", 0)
        val v2 = SmartVolatileData("v2", 0)
        val v3 = SmartVolatileData("v3", 0)
        val sum = SmartVectorData("sum", listOf(v1, v2, v3)) { val sum = it.sumBy { it }; println("computed sum: $sum"); sum }

        println("v1: ${v1.get()}, v2: ${v2.get()}, v3: ${v3.get()}")
        println("sum: ${sum.get()}")
        println("sum: ${sum.get()}")
        v1.set(10)
        println("v1: ${v1.get()}, v2: ${v2.get()}, v3: ${v3.get()}")
        println("sum: ${sum.get()}")
        println("sum: ${sum.get()}")
        v2.set(20)
        println("v1: ${v1.get()}, v2: ${v2.get()}, v3: ${v3.get()}")
        println("sum: ${sum.get()}")
        println("sum: ${sum.get()}")
        v3.set(30)
        println("v1: ${v1.get()}, v2: ${v2.get()}, v3: ${v3.get()}")
        println("sum: ${sum.get()}")
        println("sum: ${sum.get()}")

        v1.set(100)
        v2.set(200)
        v3.set(300)
        println("v1: ${v1.get()}, v2: ${v2.get()}, v3: ${v3.get()}")
        println("sum: ${sum.get()}")
        println("sum: ${sum.get()}")
    }

    @Test
    fun test_Prod() {
        val v1 = SmartVolatileData("v1", 1)
        val v2 = SmartVolatileData("v2", 1)
        val v3 = SmartVolatileData("v3", 1)
        val prod = SmartVectorData("prod", listOf(v1, v2, v3)) {
            val iterator = it.iterator()
            var prod = iterator.next()
            print("Prod of $prod ")
            for (value in iterator) {
                print("$value ")
                prod *= value
            }
            println("is $prod")
            prod
        }

        var t = prod.get()
        v1.set(10)
        t = prod.get()
        v2.set(20)
        t = prod.get()
        v3.set(30)
        t = prod.get()

        v1.set(100)
        v2.set(200)
        v3.set(300)
        t = prod.get()
    }

    @Test
    fun test_DataScopes() {
        val WIDTH = SmartVolatileDataKey("WIDTH", 0)
        val MAX_WIDTH = SmartAggregatedScopesDataKey("MAX_WIDTH", 0, WIDTH, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartDependentDataKey("COLUMN_ALIGNMENT", TextColumnAlignment.NULL_VALUE, listOf(ALIGNMENT, MAX_WIDTH), SmartScopes.SELF, { TextColumnAlignment(WIDTH.value(it), ALIGNMENT.value(it)) })

        val topScope = SmartDataScopeManager.createDataScope("top")
        val child1 = topScope.createDataScope("child1")
        val child2 = topScope.createDataScope("child2")
        val grandChild11 = child1.createDataScope("grandChild11")
        val grandChild21 = child2.createDataScope("grandChild21")

        WIDTH[child1, 0] = 10
        WIDTH[child2, 0] = 15
        WIDTH[grandChild11, 0] = 8
        WIDTH[grandChild21, 0] = 20

        val maxWidth = topScope[MAX_WIDTH, 0]

        topScope.finalizeAllScopes()

        println("MAX_WIDTH: ${maxWidth.get()}")

        WIDTH[grandChild11, 0] = 12
        WIDTH[grandChild21, 0] = 17

        println("MAX_WIDTH: ${maxWidth.get()}")

        WIDTH[grandChild21, 0] = 10

        println("MAX_WIDTH: ${maxWidth.get()}")
    }

    @Test
    fun test_VariableSequence() {
        val columns = SmartCharArraySequence("Column1|Column2|Column3".toCharArray())

        // split into pieces
        val splitColumns = columns.splitParts('|', includeDelimiter = false)
        val col1 = SmartVariableCharSequence(splitColumns[0])
        val col2 = SmartVariableCharSequence(splitColumns[1])
        val col3 = SmartVariableCharSequence(splitColumns[2])

        val delimiter = SmartCharArraySequence("|".toCharArray())

        // splice them into a single sequence
        val formattedColumns = SmartSegmentedCharSequence(delimiter, col1, delimiter, col2, delimiter, col3,delimiter)

        // connect width and alignment of column 2 and 3 to corresponding properties of column 1
        col2.widthDataPoint = col1.widthDataPoint
        col3.widthDataPoint = col1.widthDataPoint
        col2.alignmentDataPoint = col1.alignmentDataPoint
        col3.alignmentDataPoint = col1.alignmentDataPoint

        // output formatted sequence, all columns follow the setting of column 1
        println("Unformatted Columns: $formattedColumns\n")
        col1.width = 15
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.CENTER
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.RIGHT
        println("Formatted Columns: $formattedColumns\n")
    }

    @Test
    fun test_VariableSequenceSnapshot() {
        val columns = SmartCharArraySequence("Column1|Column2|Column3".toCharArray())

        // split into pieces
        val splitColumns = columns.splitParts('|', includeDelimiter = false)
        val col1 = SmartVariableCharSequence(splitColumns[0])
        val col2 = SmartVariableCharSequence(splitColumns[1])
        val col3 = SmartVariableCharSequence(splitColumns[2])

        val delimiter = SmartCharArraySequence("|".toCharArray())

        // splice them into a single sequence
        val formattedColumns = SmartSegmentedCharSequence(delimiter, col1, delimiter, col2, delimiter, col3,delimiter)

        // connect width and alignment of column 2 and 3 to corresponding properties of column 1
        col2.widthDataPoint = col1.widthDataPoint
        col3.widthDataPoint = col1.widthDataPoint
        col2.alignmentDataPoint = col1.alignmentDataPoint
        col3.alignmentDataPoint = col1.alignmentDataPoint

        // output formatted sequence, all columns follow the setting of column 1
        println("Unformatted Columns: $formattedColumns\n")
        col1.width = 15
        val cashedProxyLeft15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.CENTER
        val cashedProxyCenter15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.RIGHT
        val cashedProxyRight15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")

        println("cachedProxyLeft15 isStale(${cashedProxyLeft15.version.isStale}): $cashedProxyLeft15\n")
        println("cachedProxyCenter15 isStale(${cashedProxyCenter15.version.isStale}): $cashedProxyCenter15\n")
        println("cachedProxyRight15 isStale(${cashedProxyRight15.version.isStale}): $cashedProxyRight15\n")
    }

    @Test
    fun formatTable() {
        // width of text in the column, without formatting
        val COLUMN_WIDTH = SmartVolatileDataKey("COLUMN_WIDTH", 0)

        // alignment of the column
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)

        // max column width across all rows in the table
        val MAX_COLUMN_WIDTH = SmartAggregatedScopesDataKey("MAX_COLUMN_WIDTH", 0, COLUMN_WIDTH, setOf(SmartScopes.RESULT_TOP, SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })

        // alignment of each in the top data scope, a copy of one provided by header row
        val COLUMN_ALIGNMENT = SmartLatestDataKey("COLUMN_ALIGNMENT", TextAlignment.LEFT, ALIGNMENT, setOf(SmartScopes.RESULT_TOP, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS))

        val tableDataScope = SmartDataScopeManager.createDataScope("tableDataScope")
        var formattedTable = EditableCharSequence()

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val tableRows = table.splitPartsSegmented('\n', false)

        var row = 0
        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            val rowDataScope = tableDataScope.createDataScope("row:$row")
            var col = 0
            for (column in line.splitPartsSegmented('|', false).segments) {
                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)

                // treatment of discretionary left align marker `:` in header. 1 to always add, 0 to leave as is, any other value to remove it
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

        tableDataScope.finalizeAllScopes()
        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")
    }
}


