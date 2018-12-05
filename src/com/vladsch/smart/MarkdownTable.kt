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

import com.intellij.openapi.editor.LogicalPosition
import java.util.*

data class TableCell(val charSequence: CharSequence, val untrimmedWidth: Int, val colSpan: Int = 1, val isUnterminated: Boolean = false) {
    init {
        assert(colSpan >= 1)
    }

    fun withColSpan(colSpan: Int): TableCell {
        return TableCell(charSequence, untrimmedWidth, colSpan)
    }
}

class TableRow(val rowCells: ArrayList<TableCell>, val isSeparator: Boolean) {
    constructor(rowCells: ArrayList<TableCell>) : this(rowCells, isSeparator(rowCells))

    val totalColumns: Int
        get() {
            return columnOf(rowCells.size)
        }

    val isUnterminated: Boolean
        get() {
            return rowCells.size > 0 && rowCells[rowCells.size - 1].isUnterminated
        }

    fun columnOf(index: Int): Int {
        return columnOfOrNull(index)!!
    }

    fun columnOfOrNull(index: Int?): Int? {
        if (index == null) return null

        var columns = 0

        for (i in 0..(index - 1).maxLimit(rowCells.size - 1)) {
            val cell = rowCells[i]
            columns += cell.colSpan
        }

        return columns
    }

    fun indexOf(column: Int): Int {
        return indexOfOrNull(column)!!
    }

    fun indexOfOrNull(column: Int?): Int? {
        if (column == null) return null

        var columns = 0
        var index = 0
        for (cell in rowCells) {
            if (columns >= column) break
            columns += cell.colSpan
            index++
        }
        return index
    }

    private fun addColumn(index: Int = rowCells.size) {
        if (isSeparator) {
            rowCells.add(index, TableCell(MarkdownTableFormatter.SEPARATOR_COLUMN, MarkdownTableFormatter.SEPARATOR_COLUMN.length, 1))
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

    private fun explicitColumns(): Array<TableCell?> {
        val explicitColumns = Array<TableCell?>(totalColumns) { null }

        var explicitIndex = 0
        for (cell in rowCells) {
            explicitColumns[explicitIndex] = cell
            explicitIndex += cell.colSpan
        }
        return explicitColumns
    }

    private fun addExplicitColumns(explicitColumns: Array<TableCell?>, isUnterminated: Boolean) {
        var lastCell: TableCell? = null

        for (i in 0..explicitColumns.lastIndex) {
            val cell = explicitColumns[i]
            lastCell = if (cell == null) {
                lastCell?.withColSpan(lastCell.colSpan + 1)
                        ?: TableCell(EMPTY_SEQUENCE, 0, 1, i == explicitColumns.lastIndex && isUnterminated)
            } else {
                if (lastCell != null) rowCells.add(lastCell)
                cell.withColSpan(1)
            }
        }

        if (lastCell != null) {
            rowCells.add(lastCell)
        }
    }

    fun moveColumn(fromColumn: Int, toColumn: Int) {
        val maxColumn = totalColumns
        if (fromColumn != toColumn && fromColumn < maxColumn && toColumn < maxColumn) {
            val isUnterminated = rowCells.last().isUnterminated
            val explicitColumns = explicitColumns()

            val fromCell = explicitColumns[fromColumn]
            if (toColumn < fromColumn) {
                // shift in between columns right
                System.arraycopy(explicitColumns, toColumn, explicitColumns, toColumn + 1, fromColumn - toColumn)
            } else {
                // shift in between columns left
                System.arraycopy(explicitColumns, fromColumn + 1, explicitColumns, fromColumn, toColumn - fromColumn)
            }
            explicitColumns[toColumn] = fromCell

            // reconstruct cells
            rowCells.clear()
            addExplicitColumns(explicitColumns, isUnterminated)
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
        @JvmStatic
        fun isSeparator(rowCells: ArrayList<TableCell>): Boolean {
            if (rowCells.isEmpty()) return false;

            for (cell in rowCells) {
                if (!MarkdownTableFormatter.SEPARATOR_COLUMN_PATTERN_REGEX.matches(cell.charSequence)) return false
            }
            return true
        }
    }

    fun logicalPosition(tableStartColumn: Int, row: Int, index: Int, inColumnOffset: Int): LogicalPosition? {
        if (index < rowCells.size) {
            var col = 0
            val endColIndex = if (inColumnOffset < 0) index else index - 1
            for (i in 0..endColIndex) {
                col += rowCells[i].untrimmedWidth
            }
            return LogicalPosition(row, tableStartColumn + col + inColumnOffset)
        }
        return null
    }
}

class MarkdownTable(val rows: ArrayList<TableRow>, val caption: String?, val indentPrefix: CharSequence, val exactColumn: Int?, val offsetRow: Int?, val offsetColumn: Int?) {
    private var mySeparatorRow: Int = 0
    private var mySeparatorRowCount: Int = 0

    init {
        computeSeparatorRow()
    }

    val isUnterminated: Boolean
        get() {
            return rows.size > 0 && rows[rows.size - 1].isUnterminated
        }

    val separatorRow: Int get() = mySeparatorRow

    val separatorRowCount: Int get() = mySeparatorRowCount

    val maxColumns: Int
        get() {
            return maxColumnsWithout()
        }

    val minColumns: Int
        get() {
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

    fun moveColumn(fromColumn: Int, toColumn: Int) {
        for (row in rows) {
            row.moveColumn(fromColumn, toColumn)
        }
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

    fun logicalPositionFromColumnOffset(tableStartColumn: Int, row: Int, column: Int, firstRowOffset: Int, inColumnOffset: Int): LogicalPosition? {

        if (row < rows.size) {
            return rows[row].logicalPosition(tableStartColumn, row + firstRowOffset, rows[row].indexOf(column), inColumnOffset)
        }
        return null
    }

    fun logicalPosition(tableStartColumn: Int, row: Int, column: Int, firstRowOffset: Int, inColumnOffset: Int): LogicalPosition? {
        if (row < rows.size) {
            return rows[row].logicalPosition(tableStartColumn, row + firstRowOffset, column, inColumnOffset)
        }
        return null
    }
}

