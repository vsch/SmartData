/*
 * Copyright (c) 2015-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
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

class MarkdownTableFormatterTest {

    @Test
    fun formatTableSimpleDefault() {

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
|:------------------------------|:----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleNoLeadTrailPipes() {

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_LEAD_TRAIL_PIPES = false
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""Header 0                      | Header 1              |          Header 2           |              Header 3
:-----------------------------|:----------------------|:---------------------------:|---------------------:
Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleNoPipePadding() {

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_SPACE_AROUND_PIPE = false
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""|Header 0                     |Header 1             |         Header 2          |             Header 3|
|:----------------------------|:--------------------|:-------------------------:|--------------------:|
|Row 1 Col 0 Data             |Row 1 Col 1 Data     |   Row 1 Col 2 More Data   |Row 1 Col 3 Much Data|
|Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|     Row 2 Col 3 Data|
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleNoColAdj() {

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data          |Row 1 Col 3 Much Data           |
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data  |
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_ADJUST_COLUMN_WIDTH = false
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0 | Header 1 | Header 2 | Header 3 |
|:--------|:---------|:--------:|-------:|
| Row 1 Col 0 Data | Row 1 Col 1 Data |     Row 1 Col 2 More Data     |          Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data | Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleNoColAlign() {
        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_APPLY_COLUMN_ALIGNMENT = false
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              | Header 2                    | Header 3              |
|:------------------------------|:----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      | Row 1 Col 2 More Data       | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data | Row 2 Col 3 Data      |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleAsIsAlignMarker() {
        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_LEFT_ALIGN_MARKER = 0
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
|-------------------------------|:----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleRemoveAlignMarker() {
        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_LEFT_ALIGN_MARKER = -1
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
|-------------------------------|-----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableSpannedDefault() {
        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
|Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data|
|Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data|
|Row 3 Col 0-1 Default Alignment||Row 3 Col 2 a lot more Data|Row 3 Col 3 Data|
|Row 4 Col 0 Default Alignment|Row 4 Col 1-2 More Data||Row 4 Col 3 Data|
|Row 5 Col 0 Default Alignment|Row 5 Col 1 More Data|Row 5 Col 2-3 a lot more Data||
|Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data|||Row 6 Col 3 Data|
|Row 7 Col 0 Default Alignment|Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data|||
|Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data||||
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
|:------------------------------|:----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
| Row 3 Col 0-1 Default Alignment                      || Row 3 Col 2 a lot more Data |      Row 3 Col 3 Data |
| Row 4 Col 0 Default Alignment | Row 4 Col 1-2 More Data                            ||      Row 4 Col 3 Data |
| Row 5 Col 0 Default Alignment | Row 5 Col 1 More Data |           Row 5 Col 2-3 a lot more Data            ||
| Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data |||      Row 6 Col 3 Data |
| Row 7 Col 0 Default Alignment | Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data      |||
| Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data       ||||
""", formattedTable.toString())
    }

    @Test
    fun formatTableSpannedNoLeadTrailPipes() {
        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
|Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data|
|Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data|
|Row 3 Col 0-1 Default Alignment||Row 3 Col 2 a lot more Data|Row 3 Col 3 Data|
|Row 4 Col 0 Default Alignment|Row 4 Col 1-2 More Data||Row 4 Col 3 Data|
|Row 5 Col 0 Default Alignment|Row 5 Col 1 More Data|Row 5 Col 2-3 a lot more Data||
|Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data|||Row 6 Col 3 Data|
|Row 7 Col 0 Default Alignment|Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data|||
|Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data||||
""".toCharArray())

        val settings = MarkdownTableFormatSettings()
        settings.TABLE_LEAD_TRAIL_PIPES = false
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""Header 0                      | Header 1              |          Header 2           |              Header 3
:-----------------------------|:----------------------|:---------------------------:|---------------------:
Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data
Row 3 Col 0-1 Default Alignment                      || Row 3 Col 2 a lot more Data |      Row 3 Col 3 Data
Row 4 Col 0 Default Alignment | Row 4 Col 1-2 More Data                            ||      Row 4 Col 3 Data
Row 5 Col 0 Default Alignment | Row 5 Col 1 More Data |          Row 5 Col 2-3 a lot more Data           ||
Row 6 Col 0-2 Default Alignment Row 6 Col 1 More Data Row 6 Col 2 a lot more Data |||      Row 6 Col 3 Data
Row 7 Col 0 Default Alignment | Row 7 Col 1-3 More Data Row 7 Col 2 a lot more Data Row 7 Col 3 Data    |||
Row 8 Col 0-3 Default Alignment Row 8 Col 1 More Data Row 8 Col 2 a lot more Data Row 8 Col 3 Data     ||||
""", formattedTable.toString())
    }

    @Test
    fun formatTableSimpleTracking() {

        val chars = """Header 0|Header 1|Header 2|Header 3
Header 0 line 2|Header 1 extra|More Header 2|Another Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray()
        val table = SmartCharArraySequence(chars)

        val settings = MarkdownTableFormatSettings()
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        val rows = table.splitPartsSegmented('\n', true)
        var rowOffset = 0
        for (row in rows.segments) {
            val cols = row.splitPartsSegmented('|', true)
            var colOffset = rowOffset
            for (col in cols.segments) {
                val start = formattedTable.trackedLocation(chars, colOffset + col.countLeading(' '))
                val end = formattedTable.trackedLocation(chars, colOffset + col.length - 2 - col.countTrailing(' '))

                if (!col.contains('-')) {
                    assertEquals(true, start != null, "testing start source for $colOffset '${col.asString()}'")
                    assertEquals(true, end != null, "testing start source for ${colOffset + col.length - 2} '${col.asString()}'")

                    assertEquals(table.subSequence(start!!.offset, end!!.offset + 1).toString(), formattedTable.subSequence(start.index, end.index + 1).toString(), "Testing tracked content match from ${start.offset}, ${end.offset} to ${start.index}, ${end.index}")
                }

                colOffset += col.length
            }
            rowOffset += row.length
        }

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""| Header 0                      | Header 1              |          Header 2           |              Header 3 |
| Header 0 line 2               | Header 1 extra        |        More Header 2        |      Another Header 3 |
|:------------------------------|:----------------------|:---------------------------:|----------------------:|
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }

    @Test
    fun formatTableIndentedTracking() {

        val chars = """    Header 0|Header 1|Header 2|Header 3
     Header 0 line 2|Header 1 extra|More Header 2|Another Header 3
     --------|:-------- |:--------:|-------:
    Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
    Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray()
        val table = SmartCharArraySequence(chars)

        val settings = MarkdownTableFormatSettings()
        var formattedTable = MarkdownTableFormatter(settings).formatTable(table)

        val rows = table.splitPartsSegmented('\n', true)
        var rowOffset = 0
        for (row in rows.segments) {
            val cols = row.splitPartsSegmented('|', true)
            var colOffset = rowOffset
            for (col in cols.segments) {
                val start = formattedTable.trackedLocation(chars, colOffset + col.countLeading(' '))
                val end = formattedTable.trackedLocation(chars, colOffset + col.length - 2 - col.countTrailing(' '))

                if (!col.contains('-')) {
                    assertEquals(true, start != null, "testing start source for $colOffset '${col.asString()}'")
                    assertEquals(true, end != null, "testing start source for ${colOffset + col.length - 2} '${col.asString()}'")

                    assertEquals(table.subSequence(start!!.offset, end!!.offset + 1).toString(), formattedTable.subSequence(start.index, end.index + 1).toString(), "Testing tracked content match from ${start.offset}, ${end.offset} to ${start.index}, ${end.index}")
                }

                colOffset += col.length
            }
            rowOffset += row.length
        }

        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")

        kotlin.test.assertEquals("""    | Header 0                      | Header 1              |          Header 2           |              Header 3 |
    | Header 0 line 2               | Header 1 extra        |        More Header 2        |      Another Header 3 |
    |:------------------------------|:----------------------|:---------------------------:|----------------------:|
    | Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
    | Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
""", formattedTable.toString())
    }


}
