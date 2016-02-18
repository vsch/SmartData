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


/**
 * markdown table formatter
 *
 * spacePadPipes:Boolean - true to put spaces before and after | characters
 * discretionaryAlignMarker:Int - -1 to always remove, 0 to leave as is, 1 to always add
 */
class MarkdownTableFormatter(val settings: MarkdownTableFormatSettings) {
    constructor() : this(MarkdownTableFormatSettings())

    fun formatTable(table: SmartCharSequence): SmartCharSequence {
        return formatTable(table, -1)
    }

    fun formatTable(table: SmartCharSequence, caretOffset: Int): SmartCharSequence {
        val tableBalancer = SmartTableColumnBalancer()
        var formattedTable = EditableCharSequence()

        val pipeSequence = RepeatedCharSequence('|')
        val endOfLine = RepeatedCharSequence('\n')
        val space = RepeatedCharSequence(' ')
        val afterPipePadding = if (settings.TABLE_SPACE_AFTER_PIPE) space else EMPTY_SEQUENCE // or empty if don't want padding around pipes
        val beforePipePadding = if (settings.TABLE_SPACE_BEFORE_PIPE) space else EMPTY_SEQUENCE // or empty if don't want padding around pipes
        val alignMarker = RepeatedCharSequence(':')

        val tableRows = table.splitPartsSegmented('\n', false)
        var row = 0

        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            var rowText = line

            // remove leading pipes, and trailing pipes that are singles
            if (rowText.length > 0 && rowText[0] == '|') rowText = rowText.subSequence(1, rowText.length)
            if (rowText.length > 2 && rowText[rowText.length - 2] != '|' && rowText[rowText.length - 1] == '|') rowText = rowText.subSequence(0, rowText.length - 1)

            //val locStart = line.trackedSourceLocation(0)
            //val locEnd = line.trackedSourceLocation(line.lastIndex)
            //println("caretOffset $caretOffset startLine: ${locStart.offset}, endLine: ${locEnd.offset}")

            val segments = rowText.splitPartsSegmented('|', false).segments
            var col = 0
            var lastSpan = 1
            while (col < segments.size) {
                var column = segments[col]

                val colStart = column.trackedSourceLocation(0)
                val colEnd = column.trackedSourceLocation(column.lastIndex)
                //println("caretOffset $caretOffset starCol: ${colStart.offset}, endCol: ${colEnd.offset}")

                if (settings.TABLE_ADJUST_COLUMN_WIDTH) {
                    if (!(colStart.offset <= caretOffset && caretOffset <= colEnd.offset)) {
                        column = column.trim() as SmartCharSequence
                    } else {
                        // can trim off leading since we may add spaces
                        column = column.trimStart() as SmartCharSequence
                        // should also trim spaces after caret
                        val caretIndex = caretOffset - colStart.offset
                        column = column.subSequence(0, caretIndex).append(column.subSequence(caretIndex, column.length).trimEnd())
                    }
                }

                if (column.isEmpty()) column = column.append(space)

                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null && settings.TABLE_ADJUST_COLUMN_WIDTH) EMPTY_SEQUENCE else column)

                if (headerParts != null) {
                    val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                    val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                    if (settings.TABLE_ADJUST_COLUMN_WIDTH) {
                        formattedCol.leftPadChar = '-'
                        formattedCol.rightPadChar = '-'
                    }
                    when {
                        haveLeft && haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.CENTER))
                            if (settings.TABLE_ADJUST_COLUMN_WIDTH) {
                                formattedCol.prefix = alignMarker
                                formattedCol.suffix = alignMarker
                            }
                        }
                        haveRight -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.RIGHT))
                            if (settings.TABLE_ADJUST_COLUMN_WIDTH) formattedCol.suffix = alignMarker
                        }
                        else -> {
                            if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) tableBalancer.alignment(col, SmartImmutableData(TextAlignment.LEFT))
                            if (settings.TABLE_ADJUST_COLUMN_WIDTH) {
                                if (settings.TABLE_LEFT_ALIGN_MARKER == 1 || settings.TABLE_LEFT_ALIGN_MARKER == 0 && haveLeft) formattedCol.prefix = alignMarker
                            }
                        }
                    }
                } else {
                    if (settings.TABLE_LEAD_TRAIL_PIPES || col > 0) formattedCol.prefix = afterPipePadding
                    if (settings.TABLE_LEAD_TRAIL_PIPES || col < segments.lastIndex) formattedCol.suffix = if (column[column.lastIndex] != ' ') beforePipePadding else EMPTY_SEQUENCE
                }

                // see if we have spanned columns
                var span = 1
                while (col + span <= segments.lastIndex && segments[col + span].isEmpty()) span++

                if (col > 0) formattedRow.appendOptimized(pipeSequence.repeat(lastSpan))
                formattedRow.append(formattedCol)

                if (settings.TABLE_ADJUST_COLUMN_WIDTH) {
                    val widthOffset = if (span > 1 && col == segments.lastIndex - span + 1 && !settings.TABLE_LEAD_TRAIL_PIPES) 1 else 0
                    formattedCol.widthDataPoint = tableBalancer.width(col, formattedCol.lengthDataPoint, span, widthOffset)
                }
                if (settings.TABLE_APPLY_COLUMN_ALIGNMENT) formattedCol.alignmentDataPoint = tableBalancer.alignmentDataPoint(col)

                lastSpan = span

                col += span
            }

            // here if we add pipes then add lastSpan, else lastSpan-1
            if (settings.TABLE_LEAD_TRAIL_PIPES) {
                formattedTable.appendOptimized(pipeSequence, formattedRow, pipeSequence.repeat(lastSpan), endOfLine)
            } else {
                formattedTable.appendOptimized(formattedRow, if (lastSpan > 1) pipeSequence.repeat(lastSpan) else EMPTY_SEQUENCE, endOfLine)
            }
            row++
        }

        tableBalancer.finalizeTable()
        return formattedTable.chars
    }
}
