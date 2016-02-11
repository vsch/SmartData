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

class SmartDataTest {
    @Test
    fun test_basicChildAggregator() {
        val manager = SmartDataManager()

        val IndentKey = SmartValueKey(0, "INDENT", Int::class.java)
        val IndentPosKey = SmartValueKey(-1, "INDENT_POS", Int::class.java)
        val IndentMaxPosKey = SmartValueKey(0, "INDENT_MAX_POS", Int::class.java)
        val IndentMinPosKey = SmartValueKey(0, "INDENT_MIN_POS", Int::class.java)

        val indentKeyAggregator = SmartValueAggregator(arrayOf(IndentKey), setOf(SmartAggregate.PARENT), ValueAggregator { type, result, sources ->
            update(result, sources, IndentKey) { index, value ->
                println("IndentAggregator from ${sources.first().name} to ${result.name} [$index] $value -> ${value + 4}")
                value + 4
            }
        })

        val maxIndentPosAggregator = SmartValueAggregator(arrayOf(IndentMaxPosKey), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, IndentMaxPosKey, IndentPosKey) { self, index, value ->
                Integer.max(self ?: value, value)
            }
        })

        val minIndentPosAggregator = SmartValueAggregator(arrayOf(IndentMinPosKey), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, IndentMinPosKey, IndentPosKey) { self, index, value ->
                Integer.min(self ?: value, value)
            }
        })

        var indent0: Int? = null
        var indent1: Int? = null
        var indent2: Int? = null
        var indent21: Int? = null

        manager.registerAggregator(indentKeyAggregator, maxIndentPosAggregator, minIndentPosAggregator)

        val parentData = manager.createData("parentData")
        val childData1 = parentData.createData("childData1")
        val childData2 = parentData.createData("childData2")
        val grandChildData21 = childData2.createData("grandChildData21")

        parentData.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent0 = value
            }
        })

        grandChildData21.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent21 = value
            }
        })

        childData2.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent2 = value
            }
        })

        childData1.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent1 = value
            }
        })

        parentData.updateValue(IndentKey, 0)
        parentData.notifyListeners()

        assertEquals(0, indent0)
        assertEquals(4, indent1)
        assertEquals(4, indent2)
        assertEquals(8, indent21)

        grandChildData21.setValue(IndentPosKey, 4)
        childData2.setValue(IndentPosKey, 8)
        childData1.setValue(IndentPosKey, 0)
        parentData.setValue(IndentPosKey, 2)

        assertEquals(8, parentData.getValue(IndentMaxPosKey))
        assertEquals(0, parentData.getValue(IndentMinPosKey))

        assertEquals(0, childData1.getValue(IndentMaxPosKey))
        assertEquals(0, childData1.getValue(IndentMinPosKey))

        assertEquals(8, childData2.getValue(IndentMaxPosKey))
        assertEquals(4, childData2.getValue(IndentMinPosKey))

        assertEquals(4, grandChildData21.getValue(IndentMaxPosKey))
        assertEquals(4, grandChildData21.getValue(IndentMinPosKey))
    }

    @Test
    fun test_basicVector() {
        val manager = SmartDataManager()

        val IndentKey = SmartValueKey(0, "INDENT", Int::class.java)
        val IndentPosKey = SmartValueKey(-1, "INDENT_POS", Int::class.java)
        val IndentMaxPosKey = SmartValueKey(0, "INDENT_MAX_POS", Int::class.java)
        val IndentMinPosKey = SmartValueKey(0, "INDENT_MIN_POS", Int::class.java)

        val indentKeyAggregator = SmartValueAggregator(arrayOf(IndentKey), setOf(SmartAggregate.PARENT), ValueAggregator { type, result, sources ->
            update(result, sources, IndentKey) { index, value ->
                println("IndentAggregator from ${sources.first().name} to ${result.name} [$index] $value -> ${value + 4}")
                value + 4
            }
        })

        val maxIndentPosAggregator = SmartValueAggregator(arrayOf(IndentMaxPosKey), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, IndentMaxPosKey, IndentPosKey) { self, index, value ->
                Integer.max(self ?: value, value)
            }
        })

        val minIndentPosAggregator = SmartValueAggregator(arrayOf(IndentMinPosKey), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, IndentMinPosKey, IndentPosKey) { self, index, value ->
                Integer.min(self ?: value, value)
            }
        })

        var indent0: Int? = null
        var indent1: Int? = null
        var indent2: Int? = null
        var indent21: Int? = null

        manager.registerAggregator(indentKeyAggregator, maxIndentPosAggregator, minIndentPosAggregator)

        val parentData = manager.createData("parentData")
        val childData1 = parentData.createData("childData1")
        val childData2 = parentData.createData("childData2")
        val grandChildData21 = childData2.createData("grandChildData21")

        parentData.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent0 = value
            }
        })

        grandChildData21.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent21 = value
            }
        })

        childData2.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent2 = value
            }
        })

        childData1.registerListener(IndentKey, SmartValueListener<Int>() {
            it.forEachValue { index, value ->
                println("INDENT[$index]: $value")
                if (index == 0) indent1 = value
            }
        })

        val indent = parentData[IndentKey]
        indent.tail = 0
        //        parentData[IndentKey] = 0
        //parentData.updateValue(IndentKey, 0)
        parentData.notifyListeners()

        assertEquals(0, indent0)
        assertEquals(4, indent1)
        assertEquals(4, indent2)
        assertEquals(8, indent21)

        grandChildData21.setValue(IndentPosKey, 4)
        childData2.setValue(IndentPosKey, 8)
        childData1.setValue(IndentPosKey, 0)
        parentData.setValue(IndentPosKey, 2)

        assertEquals(8, parentData.getValue(IndentMaxPosKey))
        assertEquals(0, parentData.getValue(IndentMinPosKey))

        assertEquals(0, childData1.getValue(IndentMaxPosKey))
        assertEquals(0, childData1.getValue(IndentMinPosKey))

        assertEquals(8, childData2.getValue(IndentMaxPosKey))
        assertEquals(4, childData2.getValue(IndentMinPosKey))

        assertEquals(4, grandChildData21.getValue(IndentMaxPosKey))
        assertEquals(4, grandChildData21.getValue(IndentMinPosKey))
    }

    @Test
    fun test_allDressed() {
        val manager = SmartDataManager()

        val COLUMN_WIDTH = SmartValueKey(0, "COLUMN_WIDTH", Int::class.java)
        val MAX_COLUMN_WIDTH = SmartValueKey(0, "MAX_COLUMN_WIDTH", Int::class.java)
        val ALIGNMENT = SmartValueKey(TextAlignment.LEFT, "ALIGNMENT", TextAlignment::class.java)
        val COLUMN_ALIGNMENT = SmartValueKey(TextColumnAlignment.NULL_VALUE, "COLUMN_ALIGNMENT", TextColumnAlignment::class.java)

        val columnAlignment = SmartValueAggregator(arrayOf(COLUMN_ALIGNMENT), setOf(SmartAggregate.SELF, SmartAggregate.PARENT), ValueAggregator { type, result, sources ->
            when (type) {
                SmartAggregate.SELF -> {
                    val widths = result[MAX_COLUMN_WIDTH]
                    val alignments = result[ALIGNMENT]
                    val colAlignments = result[COLUMN_ALIGNMENT]

                    for (key in widths.keys.union(alignments.keys)) {
                        colAlignments[key] = TextColumnAlignment(widths[key], alignments[key])
                    }
                }
                else -> {
                    update(result, sources, COLUMN_ALIGNMENT) { index, value ->
                        value
                    }
                }
            }
        })

        val maxColumnWidthAggregator = SmartValueAggregator(arrayOf(MAX_COLUMN_WIDTH), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, MAX_COLUMN_WIDTH, COLUMN_WIDTH) { self, index, value ->
                Integer.max(self ?: value, value)
            }
        })

        val alignmentAggregator = SmartValueAggregator(arrayOf(ALIGNMENT), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            aggregate(true, result, sources, ALIGNMENT) { self, index, value ->
                value
            }
        })

        manager.registerAggregator(maxColumnWidthAggregator, alignmentAggregator, columnAlignment)

        val tableData = SmartData("tableData", manager, null)
        var formattedTable = EditableCharSequence()

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
--------|:--------|:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        SmartVersionManager.groupedUpdate(Runnable {
            val tableRows = table.splitPartsSegmented('\n', false)

            var row = 0
            for (line in tableRows.segments) {
                var rowSeq = EditableCharSequence()
                val rowData = SmartData("row:$row", manager, tableData)
                var col = 0
                for (column in line.splitPartsSegmented('|', false).segments) {
                    var trimmed = column.trim()
                    val haveLeft = trimmed.length > 0 && trimmed[0] == ':'
                    if (haveLeft) trimmed = trimmed.subSequence(1, trimmed.length)
                    val haveRight = trimmed.length > 0 && trimmed[trimmed.lastIndex] == ':'
                    if (haveRight) trimmed = trimmed.subSequence(0, trimmed.lastIndex)
                    val isHeader = trimmed.isAllSame('-')

                    val smartVariableCharSequence = SmartVariableCharSequence(if (isHeader) EMPTY_SEQUENCE else column)
                    val discretionary = 1

                    if (isHeader) {
                        smartVariableCharSequence.leftPadChar = '-'
                        smartVariableCharSequence.rightPadChar = '-'
                        when {
                            haveLeft && haveRight -> {
                                rowData.setValue(ALIGNMENT, TextAlignment.CENTER, col)
                                smartVariableCharSequence.prefix = ":"
                                smartVariableCharSequence.suffix = ":"
                            }
                            haveRight -> {
                                rowData.setValue(ALIGNMENT, TextAlignment.RIGHT, col)
                                smartVariableCharSequence.suffix = ":"
                            }
                            else -> {
                                rowData.setValue(ALIGNMENT, TextAlignment.LEFT, col)
                                if (discretionary == 1 || discretionary == 0 && haveLeft) smartVariableCharSequence.prefix = ":"
                            }
                        }
                    }

                    if (col > 0) rowSeq.append(" | ")
                    rowSeq.append(smartVariableCharSequence)
                    smartVariableCharSequence.provideLength(rowData, COLUMN_WIDTH, col)
                    smartVariableCharSequence.alignmentFrom(rowData, COLUMN_ALIGNMENT, col)
                    col++
                }
                formattedTable.append("| ").append(rowSeq).append(" |\n")
                row++
            }
        })

        println(tableData[MAX_COLUMN_WIDTH]);

        tableData.notifyListeners()
        //        formattedTable.flatten()

        println("Original Table:\n$table\n")
        println("Formatted Table:\n$formattedTable\n\n")
    }


    @Test
    fun test_allDressedVectors() {
        val manager = SmartDataManager()

        val COLUMN_WIDTH = SmartValueKey(0, "COLUMN_WIDTH", Int::class.java)
        val MAX_COLUMN_WIDTH = SmartValueKey(0, "MAX_COLUMN_WIDTH", Int::class.java)
        val ALIGNMENT = SmartValueKey(TextAlignment.LEFT, "ALIGNMENT", TextAlignment::class.java)
        val COLUMN_ALIGNMENT = SmartValueKey(TextColumnAlignment.NULL_VALUE, "COLUMN_ALIGNMENT", TextColumnAlignment::class.java)

        val columnAlignment = SmartValueAggregator(arrayOf(COLUMN_ALIGNMENT), setOf(SmartAggregate.SELF, SmartAggregate.PARENT), ValueAggregator { type, result, sources ->
            when (type) {
                SmartAggregate.SELF -> {
                    result[COLUMN_ALIGNMENT] = vectorMap(result[MAX_COLUMN_WIDTH], result[ALIGNMENT]) { width, alignment -> TextColumnAlignment(width, alignment) }
                }
                else -> {
                    result[COLUMN_ALIGNMENT] = vectorUnion(COLUMN_ALIGNMENT, sources)
                }
            }
        })

        val maxColumnWidthAggregator = SmartValueAggregator(arrayOf(MAX_COLUMN_WIDTH), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            result[MAX_COLUMN_WIDTH] = vectorFold(COLUMN_WIDTH, sources) { self, other -> Integer.max(self, other) }
        })

        val alignmentAggregator = SmartValueAggregator(arrayOf(ALIGNMENT), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            result[ALIGNMENT] = vectorUnion(ALIGNMENT, sources)
        })

        manager.registerAggregator(maxColumnWidthAggregator, alignmentAggregator, columnAlignment)

        val tableData = manager.createData("tableData")
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
                val rowData = tableData.createData("row:$row")
                var col = rowData[0]

                // TODO: when surrounded with | | then need to strip them from the table
                val segments = line.splitPartsSegmented('|', false).segments

                while (col < segments.size) {
                    val column = segments[col.index]
                    val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                    val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)
                    val discretionary = 1
                    var span = 1

                    if (headerParts != null) {
                        val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                        val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                        formattedCol.leftPadChar = '-'
                        formattedCol.rightPadChar = '-'
                        when {
                            haveLeft && haveRight -> {
                                col[ALIGNMENT] = TextAlignment.CENTER
                                formattedCol.prefix = ":"
                                formattedCol.suffix = ":"
                            }
                            haveRight -> {
                                col[ALIGNMENT] = TextAlignment.RIGHT
                                formattedCol.suffix = ":"
                            }
                            else -> {
                                col[ALIGNMENT] = TextAlignment.LEFT
                                if (discretionary == 1 || discretionary == 0 && haveLeft) formattedCol.prefix = ":"
                            }
                        }
                    } else if (!segments[col.index].isEmpty()) {
                        // see if we have column spans, consecutive ||
                        while (col.index + span < segments.size && segments[col.index + span].isEmpty()) span++
                    }


                    if (col > 0) formattedRow.append(" | ")
                    formattedRow.append(formattedCol)
                    if (span > 1) formattedCol.columnSpan = span
                    formattedCol.provideLength(col[COLUMN_WIDTH])
                    formattedCol.alignmentFrom(col[COLUMN_ALIGNMENT])
                    col += span
                }

                formattedTable.append("| ", formattedRow, " |\n")
                row++
            }
        })

        tableData.notifyListeners()
        println(tableData[MAX_COLUMN_WIDTH]);

        //        formattedTable.flatten()

        println("Original Table:\n$table\n")
        println("Formatted Table:\n$formattedTable\n\n")
    }

    fun formatTable() {
        val manager = SmartDataManager()

        val COLUMN_WIDTH = SmartValueKey(0, "COLUMN_WIDTH", Int::class.java)
        val MAX_COLUMN_WIDTH = SmartValueKey(0, "MAX_COLUMN_WIDTH", Int::class.java)
        val ALIGNMENT = SmartValueKey(TextAlignment.LEFT, "ALIGNMENT", TextAlignment::class.java)
        val COLUMN_ALIGNMENT = SmartValueKey(TextColumnAlignment.NULL_VALUE, "COLUMN_ALIGNMENT", TextColumnAlignment::class.java)

        val columnAlignment = SmartValueAggregator(arrayOf(COLUMN_ALIGNMENT), setOf(SmartAggregate.SELF, SmartAggregate.PARENT), ValueAggregator { type, result, sources ->
            when (type) {
                SmartAggregate.SELF -> {
                    result[COLUMN_ALIGNMENT] = vectorMap(result[MAX_COLUMN_WIDTH], result[ALIGNMENT]) { width, alignment -> TextColumnAlignment(width, alignment) }
                }
                else -> {
                    result[COLUMN_ALIGNMENT] = vectorUnion(COLUMN_ALIGNMENT, sources)
                }
            }
        })

        val maxColumnWidthAggregator = SmartValueAggregator(arrayOf(MAX_COLUMN_WIDTH), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            result[MAX_COLUMN_WIDTH] = vectorFold(COLUMN_WIDTH, sources) { self, other -> Integer.max(self, other) }
        })

        val alignmentAggregator = SmartValueAggregator(arrayOf(ALIGNMENT), setOf(SmartAggregate.CHILDREN), ValueAggregator { type, result, sources ->
            result[ALIGNMENT] = vectorUnion(ALIGNMENT, sources)
        })

        manager.registerAggregator(maxColumnWidthAggregator, alignmentAggregator, columnAlignment)

        val tableData = manager.createData("tableData")
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
                val rowData = tableData.createData("row:$row")
                var col = rowData[0]
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
                                col[ALIGNMENT] = TextAlignment.CENTER
                                formattedCol.prefix = ":"
                                formattedCol.suffix = ":"
                            }
                            haveRight -> {
                                col[ALIGNMENT] = TextAlignment.RIGHT
                                formattedCol.suffix = ":"
                            }
                            else -> {
                                col[ALIGNMENT] = TextAlignment.LEFT
                                if (discretionary == 1 || discretionary == 0 && haveLeft) formattedCol.prefix = ":"
                            }
                        }
                    }

                    if (col > 0) formattedRow.append(" | ")
                    formattedRow.append(formattedCol)
                    formattedCol.provideLength(col[COLUMN_WIDTH])
                    formattedCol.alignmentFrom(col[COLUMN_ALIGNMENT])
                    col++
                }
                formattedTable.append("| ", formattedRow, " |\n")
                row++
            }
        })

        tableData.notifyListeners()
        val formatted = formattedTable.cachedProxy

    }

    @Test
    fun time_Format() {
        for (i in 1..1000) {
            formatTable()
        }
    }
}
