/*
 * Copyright (c) 2015-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
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

/**
 * markdown table formatter
 *
 * spacePadPipes:Boolean - true to put spaces before and after | characters
 * discretionaryAlignMarker:Int - -1 to always remove, 0 to leave as is, 1 to always add
 */
class MarkdownTableFormatter(val settings: MarkdownTableFormatSettings) {

    constructor() : this(MarkdownTableFormatSettings())

    private var myAlignmentDataPoints: List<SmartVersionedDataAlias<TextAlignment>> = listOf()
    private var myColumnWidthDataPoints: List<SmartVersionedDataAlias<Int>> = listOf()
    //    private val myRowColumns: ArrayList<List<SmartCharSequence>> = ArrayList()
    //    private val myRows: ArrayList<SmartCharSequence> = ArrayList()

    val columnCount: Int get() = myColumnWidthDataPoints.size

    fun columnWidth(index: Int): Int {
        return myColumnWidthDataPoints[index].get()
    }

    fun columnAlignmentDataPoint(index: Int): TextAlignment {
        return myAlignmentDataPoints[index].get()
    }

    fun formatTable(table: SmartCharSequence): SmartCharSequence {
        return formatTable(table, -1, CharWidthProvider.UNITY_PROVIDER)
    }

    //    val rows: Int get() = myRows.size
    //
    //    fun rowSequence(index: Int): SmartCharSequence {
    //        return myRows[index]
    //    }
    //
    //    fun rowColumns(index: Int): List<SmartCharSequence> {
    //        return myRowColumns[index]
    //    }

    fun formatTable(tableChars: SmartCharSequence, caretOffset: Int, charWidthProvider: CharWidthProvider): SmartCharSequence {
        val table = parseTable(tableChars, caretOffset, !settings.TABLE_ADJUST_COLUMN_WIDTH && settings.TABLE_TRIM_CELLS)

        if (settings.TABLE_FILL_MISSING_COLUMNS) {
            val unbalancedTable = table.minColumns != table.maxColumns
            if (unbalancedTable) table.fillMissingColumns(null)
        }

        return formatTable(table, table.indentPrefix, charWidthProvider)
    }

    fun formatTable(markdownTable: MarkdownTable, indentPrefix: CharSequence = EMPTY_SEQUENCE, charWidthProvider: CharWidthProvider): SmartCharSequence {
        val tableBalancer = SmartTableColumnBalancer(charWidthProvider)
        var formattedTable = EditableCharSequence()

        val pipeSequence = RepeatedCharSequence('|')
        val endOfLine = RepeatedCharSequence('\n')
        val space = RepeatedCharSequence(' ')
        val pipePadding = if (settings.TABLE_SPACE_AROUND_PIPE) space else EMPTY_SEQUENCE // or empty if don't want padding around pipes
        val alignMarker = RepeatedCharSequence(':')
        var rowColumns = ArrayList<SmartCharSequence>()

        var row = 0
        val addLeadTrailPipes = settings.TABLE_LEAD_TRAIL_PIPES || !indentPrefix.isEmpty() || markdownTable.minColumns < 2

        for (tableRow in markdownTable.rows) {
            var formattedRow = EditableCharSequence()

            if (addLeadTrailPipes) {
                formattedRow.append(indentPrefix, pipeSequence)
            } else {
                formattedRow.append(indentPrefix)
            }

            val segments = tableRow.rowCells
            var colIndex = 0
            var col = 0
            var lastSpan = 1
            var lastColumnEmpty = false

            while (colIndex < segments.size) {
                val tableCell = segments[colIndex]
                var columnChars:SmartCharSequence = SmartCharSequenceWrapper(tableCell.charSequence)

                if (settings.TABLE_TRIM_CELLS) columnChars = columnChars.trim()
                if (columnChars.isEmpty()) columnChars = columnChars.append(space)

                val separatorParts = if (row == markdownTable.separatorRow) columnChars.extractGroupsSegmented(SEPARATOR_COLUMN_PATTERN) else null
                assert(row != markdownTable.separatorRow || separatorParts != null, { "isSeparator but column does not match separator col" })

                val formattedCol: SmartVariableCharSequence

                if (separatorParts != null) {
                    val haveLeft = separatorParts.segments[2] != NULL_SEQUENCE
                    val haveRight = separatorParts.segments[4] != NULL_SEQUENCE

                    formattedCol = SmartVariableCharSequence(columnChars, SmartRepeatedCharSequence('-', 3 - haveLeft.ifElse(1, 0) - haveRight.ifElse(1, 0)), charWidthProvider)

                    formattedCol.leftPadChar = '-'
                    formattedCol.rightPadChar = '-'
                    when {
                        haveLeft && haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(colIndex, SmartImmutableData(TextAlignment.CENTER))
                            formattedCol.prefix = alignMarker
                            formattedCol.suffix = alignMarker
                        }
                        haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(colIndex, SmartImmutableData(TextAlignment.RIGHT))
                            formattedCol.suffix = alignMarker
                        }
                        else -> {
                            // when no alignment given use centered for header cells and left for body
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) {
                                val tableHeadingDefaultAlignment = !haveLeft && settings.TABLE_LEFT_ALIGN_MARKER != 1 || haveLeft && !haveRight && settings.TABLE_LEFT_ALIGN_MARKER == -1
                                tableBalancer.alignment(colIndex, SmartImmutableData(if (tableHeadingDefaultAlignment) TextAlignment.DEFAULT else TextAlignment.LEFT))
                            }
                            if (settings.TABLE_LEFT_ALIGN_MARKER == 1 || settings.TABLE_LEFT_ALIGN_MARKER == 0 && haveLeft) formattedCol.prefix = alignMarker
                        }
                    }
                } else {
                    formattedCol = SmartVariableCharSequence(columnChars, columnChars, charWidthProvider)
                    if (addLeadTrailPipes || colIndex > 0) formattedCol.prefix = pipePadding
                    if (addLeadTrailPipes || colIndex < segments.size - 1) formattedCol.suffix = if (columnChars.length > 0 && columnChars[columnChars.length - 1] != ' ') pipePadding else EMPTY_SEQUENCE
                }

                // see if we have spanned columns
                if (colIndex > 0) formattedRow.append(pipeSequence.repeat(lastSpan))
                formattedRow.append(formattedCol)
                rowColumns.add(formattedCol)

                val colSpan = tableCell.colSpan
                val widthOffset = if (colSpan > 1 && colIndex == segments.size - 1 && !addLeadTrailPipes) 1 else 0
                val dataPoint = tableBalancer.width(col, formattedCol.lengthDataPoint, colSpan, widthOffset)

                if (settings.TABLE_ADJUST_COLUMN_WIDTH) formattedCol.widthDataPoint = dataPoint
                else if (settings.TABLE_TRIM_CELLS) formattedCol.width = columnChars.length
                else formattedCol.width = tableCell.untrimmedWidth

                if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) formattedCol.alignmentDataPoint = tableBalancer.alignmentDataPoint(col, row < markdownTable.separatorRow)
                lastSpan = colSpan

                colIndex++
                col += colSpan
                lastColumnEmpty = columnChars.isBlank()
            }

            // here if we add pipes then add lastSpan, else lastSpan-1
            if (addLeadTrailPipes) {
                formattedRow.append(pipeSequence.repeat(lastSpan), endOfLine)
                formattedTable.append(formattedRow)
            } else {
                formattedRow.append(if (lastSpan > 1 || lastColumnEmpty) pipeSequence.repeat(lastSpan) else EMPTY_SEQUENCE, endOfLine)
                formattedTable.append(formattedRow)
            }

            row++
        }

        // take care of caption
        var caption = markdownTable.caption
        when (settings.TABLE_CAPTION) {
            MarkdownTableFormatSettings.TABLE_CAPTION_ADD -> if (caption == null) caption = ""
            MarkdownTableFormatSettings.TABLE_CAPTION_REMOVE_EMPTY -> if (caption?.isBlank() == true) caption = null
            MarkdownTableFormatSettings.TABLE_CAPTION_REMOVE -> caption = null
            else -> {
            }
        }

        if (caption != null) {
            when (settings.TABLE_CAPTION_SPACES) {
                MarkdownTableFormatSettings.TABLE_CAPTION_SPACES_REMOVE -> caption = caption.trim()
                MarkdownTableFormatSettings.TABLE_CAPTION_SPACES_ADD -> caption = " ${caption.trim()} "
                else -> {
                }
            }

            formattedTable.append("[$caption]\n")
        }

        tableBalancer.finalizeTable()
        myAlignmentDataPoints = tableBalancer.columnAlignmentDataPoints
        myColumnWidthDataPoints = tableBalancer.columnWidthDataPoints

        return formattedTable.contents //.cachedProxy
    }

    companion object {
        @JvmStatic
        val SEPARATOR_COLUMN_PATTERN = "(\\s+)?(:)?(-{1,})(:)?(\\s+)?"
        @JvmStatic
        val SEPARATOR_COLUMN_PATTERN_REGEX = SEPARATOR_COLUMN_PATTERN.toRegex()
        @JvmStatic
        val SEPARATOR_COLUMN = SmartRepeatedCharSequence('-', 3)
        @JvmStatic
        val EMPTY_COLUMN = SmartRepeatedCharSequence(' ', 1)

        fun parseTable(table: SmartCharSequence, caretOffset: Int, trimCells: Boolean): MarkdownTable {
            val space = RepeatedCharSequence(' ')
            var indentPrefix: CharSequence = EMPTY_SEQUENCE
            val tableRows = table.splitPartsSegmented('\n', false)
            var row = 0
            val tableRowCells = ArrayList<TableRow>()
            var offsetRow: Int? = null
            var offsetCol: Int? = null

            // see if there is an indentation prefix, these are always spaces in multiples of 4
            var minIndent: Int? = null
            for (line in tableRows.segments) {
                val tableRow = SafeCharSequenceIndex(line)
                val spaceCount = tableRow.tabExpandedColumnOf((tableRow.firstNonBlank - 1).minLimit(0), 4)
                if (minIndent == null || minIndent > spaceCount) minIndent = spaceCount
            }

            val removeSpaces = ((minIndent ?: 0) / 4) * 4
            val stripPrefix = if (removeSpaces > 0) "\\s{1,$removeSpaces}".toRegex() else "".toRegex()

            if (removeSpaces > 0) {
                indentPrefix = RepeatedCharSequence(' ', removeSpaces)
            }

            for (line in tableRows.segments) {
                var rowText = line // line.trimEnd()

                // remove the indent prefix
                if (!indentPrefix.isEmpty()) {
                    rowText = rowText.removePrefix(stripPrefix) as SmartCharSequence
                }

                // remove leading pipes, and trailing pipes that are singles
                if (rowText.length > 0 && rowText[0] == '|') rowText = rowText.subSequence(1, rowText.length)
                if (rowText.length > 2 && rowText[rowText.length - 2] != '|' && rowText[rowText.length - 1] == '|') rowText = rowText.subSequence(0, rowText.length - 1)

                val segments = rowText.splitPartsSegmented('|', false).segments
                val tableColumns = ArrayList<TableCell>()
                var col = 0

                var hadSeparatorCols = false
                var allSeparatorCols = true

                while (col < segments.size) {
                    var column = segments[col]
                    val untrimmedWidth = if (trimCells) column.trim().length else column.length
                    val origColumn = if (column.isEmpty()) space else column

                    if (!column.isEmpty()) {
                        val colStart = column.trackedSourceLocation(0)
                        val colEnd = column.trackedSourceLocation(column.lastIndex)

                        if (!(colStart.offset <= caretOffset && caretOffset <= colEnd.offset + 1)) {
                            column = column.trim()
                            //println("caretOffset $caretOffset starCol: ${colStart.offset}, endCol: ${colEnd.offset}")
                        } else {
                            //println("> caretOffset $caretOffset starCol: ${colStart.offset}, endCol: ${colEnd.offset} <")
                            // trim spaces after caret
                            offsetCol = col
                            offsetRow = row

                            if (trimCells) {
                                column = column.trim()
                            } else {
                                val caretIndex = caretOffset - colStart.offset
                                column = column.subSequence(0, caretIndex).append(column.subSequence(caretIndex, column.length).trimEnd())
                                if (!column.isEmpty()) {
                                    // can trim off leading since we may add spaces
                                    val leadingSpaces = column.countLeading(' ', '\t').maxLimit(caretIndex)
                                    if (leadingSpaces > 0) column = column.subSequence(leadingSpaces, column.length)
                                }
                            }
                        }
                    }

                    if (column.isEmpty()) column = column.append(origColumn.subSequence(0, 1))

                    val headerParts = column.extractGroupsSegmented(SEPARATOR_COLUMN_PATTERN)
                    if (headerParts != null) {
                        hadSeparatorCols = true
                    } else {
                        allSeparatorCols = false
                    }

                    // see if we have spanned columns
                    var span = 1
                    while (col + span < segments.size && segments[col + span].isEmpty()) span++

                    tableColumns.add(TableCell(column, untrimmedWidth, span))
                    col += span
                }

                val isSeparator = hadSeparatorCols && allSeparatorCols
                tableRowCells.add(TableRow(tableColumns, isSeparator))
                row++
            }

            return MarkdownTable(tableRowCells, null, indentPrefix, null, offsetRow, offsetCol)
        }
    }
}
