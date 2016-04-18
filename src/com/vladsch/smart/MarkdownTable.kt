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

data class TableRow(val rowCells: ArrayList<TableCell>, val isSeparator: Boolean) {
    val totalColumns: Int get() {
        return columnOf(rowCells.size - 1)
    }

    fun columnOf(index: Int): Int {
        var columns = 0

        for (i in 0..index.maxBound(rowCells.size - 1)) {
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

    fun appendColumns(count: Int) {
        for (i in 1..count) {
            // add empty column
            if (isSeparator) {
                rowCells.add(TableCell(MarkdownTableFormatter.HEADER_COLUMN, 0, 1))
            } else {
                rowCells.add(TableCell(EMPTY_SEQUENCE, 0, 1))
            }
        }
    }

    fun insertColumns(column: Int, count: Int) {
        if (count <= 0) return

        val totalColumns = totalColumns
        if (column >= totalColumns) {
            // append to the end
            appendColumns(column - totalColumns + count)
        } else {
            var index = indexOf(column)

            // insert in the middle
            val cell = rowCells[index]
            val cellColumn = columnOf(index)

            if (cellColumn > column) {
                // spanning column, we expand its span
                rowCells.removeAt(index)
                rowCells.add(index, cell.withColSpan(cell.colSpan + count))
            } else {
                for (i in 1..count) {
                    // add empty column
                    if (isSeparator) {
                        rowCells.add(index, TableCell(MarkdownTableFormatter.HEADER_COLUMN, 0, 1))
                    } else {
                        rowCells.add(index, TableCell(EMPTY_SEQUENCE, 0, 1))
                    }
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

    fun isEmpty():Boolean {
        if (isSeparator) return false

        for (cell in rowCells) {
            if (!cell.charSequence.isBlank()) return false
        }
        return true
    }
}

class MarkdownTable(val rows: ArrayList<TableRow>, val indentPrefix: CharSequence, val offsetRow: Int?, val offsetColumn: Int?) {
    val maxColumns: Int get() {
        var columns = 0

        for (row in rows) {
            columns = columns.max(row.totalColumns)
        }

        return columns
    }

    val minColumns: Int get() {
        var columns = Integer.MAX_VALUE

        for (row in rows) {
            columns = columns.min(row.totalColumns)
        }

        return columns
    }

    fun fillMissingColumns() {
        val maxColumns = this.maxColumns

        for (row in rows) {
            val rowColumns = row.totalColumns
            row.appendColumns(maxColumns - rowColumns)
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
    }

    fun insertSeparatorRow(rowIndex: Int) {
        val maxColumns = this.maxColumns
        val emptyRow = TableRow(ArrayList(), true)
        emptyRow.appendColumns(maxColumns)
        rows.add(rowIndex, emptyRow)
    }

    fun deleteRows(rowIndex: Int, count: Int) {
        var remaining = count
        while (rowIndex < rows.size && remaining > 0) {
            rows.removeAt(rowIndex)
            remaining--
        }
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
}

