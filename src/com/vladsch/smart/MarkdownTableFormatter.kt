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
    private val myRowColumns: ArrayList<List<SmartCharSequence>> = ArrayList()
    private val myRows: ArrayList<SmartCharSequence> = ArrayList()

    val columnCount: Int get() = myColumnWidthDataPoints.size

    fun columnWidth(index: Int): Int {
        return myColumnWidthDataPoints[index].value
    }

    fun columnAlignmentDataPoint(index: Int): TextAlignment {
        return myAlignmentDataPoints[index].value
    }

    fun formatTable(table: SmartCharSequence): SmartCharSequence {
        return formatTable(table, -1)
    }

    val rows:Int get() = myRows.size

    fun rowSequence(index:Int):SmartCharSequence {
        return myRows[index]
    }

    fun rowColumns(index:Int):List<SmartCharSequence> {
        return myRowColumns[index]
    }

    fun formatTable(table: SmartCharSequence, caretOffset: Int): SmartCharSequence {
        val tableBalancer = SmartTableColumnBalancer()
        var formattedTable = EditableCharSequence()

        val pipeSequence = RepeatedCharSequence('|')
        val endOfLine = RepeatedCharSequence('\n')
        val space = RepeatedCharSequence(' ')
        val pipePadding = if (settings.TABLE_SPACE_AROUND_PIPE) space else EMPTY_SEQUENCE // or empty if don't want padding around pipes
        val alignMarker = RepeatedCharSequence(':')
        var rowColumns = ArrayList<SmartCharSequence>()

        val tableRows = table.splitPartsSegmented('\n', false)
        var row = 0
        var indentPrefix: CharSequence = EMPTY_SEQUENCE

        // see if there is an indentation prefix, these are always spaces in multiples of 4
        if (tableRows.segments.size > 0) {
            val line = tableRows.segments[0]
            val spaceCount = line.expandTabs(4).countLeading(' ')
            val removeSpaces = (spaceCount / 4) * 4
            if (removeSpaces > 0) {
                indentPrefix = space.repeat(removeSpaces)
            }
        }

        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            var rowText = line.trimEnd()

            // remove the indent prefix
            if (!indentPrefix.isEmpty()) rowText = rowText.removePrefix(indentPrefix, true) as SmartCharSequence

            // remove leading pipes, and trailing pipes that are singles
            if (rowText.length > 0 && rowText[0] == '|') rowText = rowText.subSequence(1, rowText.length)
            if (rowText.length > 2 && rowText[rowText.length - 2] != '|' && rowText[rowText.length - 1] == '|') rowText = rowText.subSequence(0, rowText.length - 1)

            //val locStart = line.trackedSourceLocation(0)
            //val locEnd = line.trackedSourceLocation(line.lastIndex)
            //println("caretOffset $caretOffset startLine: ${locStart.offset}, endLine: ${locEnd.offset}")
            if (settings.TABLE_LEAD_TRAIL_PIPES) {
                formattedRow.append(indentPrefix, pipeSequence)
            } else {
                formattedRow.append(indentPrefix)
            }

            val segments = rowText.splitPartsSegmented('|', false).segments
            var col = 0
            var lastSpan = 1
            while (col < segments.size) {
                var column = segments[col]
                val untrimmedWidth = column.length
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
                        val caretIndex = caretOffset - colStart.offset
                        column = column.subSequence(0, caretIndex).append(column.subSequence(caretIndex, column.length).trimEnd())

                        if (!column.isEmpty()) {
                            // can trim off leading since we may add spaces
                            val leadingSpaces = column.countLeading(' ', '\t').maxBound(caretIndex)
                            if (leadingSpaces > 0) column = column.subSequence(leadingSpaces, column.length)
                        }
                    }
                }

                if (column.isEmpty()) column = column.append(origColumn.subSequence(0, 1))

                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)

                if (headerParts != null) {
                    val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                    val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                    formattedCol.leftPadChar = '-'
                    formattedCol.rightPadChar = '-'
                    when {
                        haveLeft && haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.CENTER))
                            formattedCol.prefix = alignMarker
                            formattedCol.suffix = alignMarker
                        }
                        haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.RIGHT))
                            formattedCol.suffix = alignMarker
                        }
                        else -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.LEFT))
                            if (settings.TABLE_LEFT_ALIGN_MARKER == 1 || settings.TABLE_LEFT_ALIGN_MARKER == 0 && haveLeft) formattedCol.prefix = alignMarker
                        }
                    }
                } else {
                    if (settings.TABLE_LEAD_TRAIL_PIPES || col > 0) formattedCol.prefix = pipePadding
                    if (settings.TABLE_LEAD_TRAIL_PIPES || col < segments.lastIndex) formattedCol.suffix = if (column[column.lastIndex] != ' ') pipePadding else EMPTY_SEQUENCE
                }

                // see if we have spanned columns
                var span = 1
                while (col + span <= segments.lastIndex && segments[col + span].isEmpty()) span++

                if (col > 0) formattedRow.appendOptimized(pipeSequence.repeat(lastSpan))
                formattedRow.append(formattedCol)
                rowColumns.add(formattedCol)

                val widthOffset = if (span > 1 && col == segments.lastIndex - span + 1 && !settings.TABLE_LEAD_TRAIL_PIPES) 1 else 0
                val dataPoint = tableBalancer.width(col, formattedCol.lengthDataPoint, span, widthOffset)

                if (settings.TABLE_ADJUST_COLUMN_WIDTH) formattedCol.widthDataPoint = dataPoint
                else formattedCol.width = untrimmedWidth

                if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) formattedCol.alignmentDataPoint = tableBalancer.alignmentDataPoint(col)
                lastSpan = span

                col += span
            }

            // here if we add pipes then add lastSpan, else lastSpan-1
            if (settings.TABLE_LEAD_TRAIL_PIPES) {
                formattedRow.append(pipeSequence.repeat(lastSpan), endOfLine)
                formattedTable.appendOptimized(formattedRow)
            } else {
                formattedRow.append(if (lastSpan > 1) pipeSequence.repeat(lastSpan) else EMPTY_SEQUENCE, endOfLine)
                formattedTable.appendOptimized(formattedRow)
            }

            myRows.add(formattedRow)
            myRowColumns.add(rowColumns)
            row++
        }

        tableBalancer.finalizeTable()
        myAlignmentDataPoints = tableBalancer.columnAlignmentDataPoints
        myColumnWidthDataPoints = tableBalancer.columnWidthDataPoints

        return formattedTable.contents.cachedProxy
    }
}
