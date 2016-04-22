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

import java.util.*

data class TableCell(val charSequence: SmartCharSequence, val untrimmedWidth: Int, val colSpan: Int = 1) {
    init {
        assert(colSpan >= 1)
    }

    fun withColSpan(colSpan: Int): TableCell {
        return TableCell(charSequence, untrimmedWidth, colSpan)
    }

    fun separatorParts(): SmartSegmentedCharSequence? {
        return charSequence.extractGroupsSegmented(MarkdownTableFormatter.HEADER_COLUMN_PATTERN)
    }
}

class TableRow(val rowCells: ArrayList<TableCell>, val isSeparator: Boolean) {
    constructor(rowCells: ArrayList<TableCell>) : this(rowCells, isSeparator(rowCells))

    val totalColumns: Int get() {
        return columnOf(rowCells.size)
    }

    fun columnOf(index: Int): Int {
        var columns = 0

        for (i in 0..(index - 1).maxBound(rowCells.size - 1)) {
            val cell = rowCells[i]
            columns += cell.colSpan
        }

        return columns
    }

    fun indexOf(column: Int): Int {
        var columns = 0
        var index = 0
        for (cell in rowCells) {
            columns += cell.colSpan
            if (columns > column) break
            index++
            if (columns >= column) break
        }
        return index
    }

    private fun addColumn(index: Int = rowCells.size) {
        if (isSeparator) {
            rowCells.add(index, TableCell(MarkdownTableFormatter.HEADER_COLUMN, MarkdownTableFormatter.HEADER_COLUMN.length, 1))
        } else {
            rowCells.add(index, TableCell(MarkdownTableFormatter.EMPTY_COLUMN, MarkdownTableFormatter.EMPTY_COLUMN.length, 1))
        }
    }

    fun appendColumns(count: Int) {
        for (i in 1..count) {
            // add empty column
            addColumn()
        }
    }

    fun insertColumns(column: Int, count: Int) {
        if (count <= 0) return

        val totalColumns = totalColumns

        if (column >= totalColumns) {
            // append to the end
            appendColumns(count)
        } else {
            // insert in the middle
            var index = indexOf(column)
            val cellColumn = columnOf(index)

            if (cellColumn > column) {
                // spanning column, we expand its span
                val cell = rowCells[index]
                rowCells.removeAt(index)
                rowCells.add(index, cell.withColSpan(cell.colSpan + count))
            } else {
                for (i in 1..count) {
                    // add empty column
                    addColumn(index)
                }
            }
        }
    }

    fun deleteColumns(column: Int, count: Int) {
        var remaining = count
        var index = indexOf(column)
        while (index < rowCells.size && remaining > 0) {
            val cell = rowCells[index]
            rowCells.removeAt(index);
            if (cell.colSpan > remaining) {
                rowCells.add(index, cell.withColSpan(cell.colSpan - remaining))
            }
            remaining -= cell.colSpan
        }
    }

    fun isEmptyColumn(column: Int): Boolean {
        val index = indexOf(column)
        return isSeparator || index >= rowCells.size || rowCells[index].charSequence.isBlank()
    }

    fun isEmpty(): Boolean {
        if (isSeparator) return false

        for (cell in rowCells) {
            if (!cell.charSequence.isBlank()) return false
        }
        return true
    }

    companion object {
        @JvmStatic fun isSeparator(rowCells: ArrayList<TableCell>): Boolean {
            if (rowCells.isEmpty()) return false;

            for (cell in rowCells) {
                if (!MarkdownTableFormatter.HEADER_COLUMN_PATTERN_REGEX.matches(cell.charSequence)) return false
            }
            return true
        }
    }
}

class MarkdownTable(val rows: ArrayList<TableRow>, val indentPrefix: CharSequence, val exactColumn: Int?, val offsetRow: Int?, val offsetColumn: Int?) {
    private var mySeparatorRow: Int = 0
    private var mySeparatorRowCount: Int = 0

    init {
        computeSeparatorRow()
    }

    val separatorRow: Int get() = mySeparatorRow

    val separatorRowCount: Int get() = mySeparatorRowCount

    val maxColumns: Int get() {
        return maxColumnsWithout()
    }

    val minColumns: Int get() {
        return minColumnsWithout()
    }

    fun fillMissingColumns(column: Int?) {
        val maxColumns = this.maxColumns

        for (row in rows) {
            val rowColumns = row.totalColumns
            val count = maxColumns - rowColumns
            if (count > 0) {
                var done = 0
                if (column != null) {
                    row.insertColumns(column, 1)
                    done = 1
                }
                if (count - done > 0) row.appendColumns(count - done)
            }
        }
    }

    fun insertColumns(column: Int, count: Int) {
        for (row in rows) {
            row.insertColumns(column, count)
        }
    }

    fun deleteColumns(column: Int, count: Int) {
        for (row in rows) {
            row.deleteColumns(column, count)
        }
    }

    fun insertRows(rowIndex: Int, count: Int) {
        val maxColumns = this.maxColumns

        for (i in 1..count) {
            val emptyRow = TableRow(ArrayList(), false)
            emptyRow.appendColumns(maxColumns)
            rows.add(rowIndex, emptyRow)
        }

        computeSeparatorRow()
    }

    fun computeSeparatorRow() {
        var firstSeparator = -1
        var secondSeparator = -1
        var firstNonSeparator = -1
        var separators = 0

        var rowIndex = 0

        for (row in rows) {
            if (row.isSeparator) {
                separators++
                if (firstSeparator == -1) firstSeparator = rowIndex
                else if (secondSeparator == -1) secondSeparator = rowIndex
            } else if (firstNonSeparator == -1) firstNonSeparator = rowIndex
            rowIndex++
        }

        if (secondSeparator == -1) mySeparatorRow = firstSeparator
        else {
            if (firstNonSeparator >= 0 && firstNonSeparator < firstSeparator) mySeparatorRow = firstSeparator
            else mySeparatorRow = secondSeparator
        }

        mySeparatorRowCount = separators
    }

    fun insertSeparatorRow(rowIndex: Int) {
        val maxColumns = this.maxColumns
        val emptyRow = TableRow(ArrayList(), true)
        emptyRow.appendColumns(maxColumns)
        rows.add(rowIndex, emptyRow)
        mySeparatorRowCount++

        computeSeparatorRow()
    }

    fun deleteRows(rowIndex: Int, count: Int) {
        var remaining = count
        while (rowIndex < rows.size && remaining > 0) {
            rows.removeAt(rowIndex)
            remaining--
        }

        computeSeparatorRow()
    }

    fun isEmptyColumn(column: Int): Boolean {
        for (row in rows) {
            if (!row.isEmptyColumn(column)) {
                return false
            }
        }

        return true
    }

    fun isEmptyRow(rowIndex: Int): Boolean {
        return rowIndex < rows.size && rows[rowIndex].isEmpty()
    }

    fun isSeparatorRow(rowIndex: Int): Boolean {
        return rowIndex == mySeparatorRow
    }

    fun maxColumnsWithout(vararg skipRows: Int): Int {
        var columns = 0
        var index = 0

        for (row in rows) {
            if (index !in skipRows) columns = columns.max(row.totalColumns)
            index++
        }
        return columns
    }

    fun minColumnsWithout(vararg skipRows: Int): Int {
        var columns = Integer.MAX_VALUE
        var index = 0

        for (row in rows) {
            if (index !in skipRows) columns = columns.min(row.totalColumns)
            index++
        }

        return if (columns == Int.MAX_VALUE) 0 else columns
    }
}

